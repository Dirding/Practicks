package com.example.prilogenie.User;

import com.example.prilogenie.Class.Database;
import com.example.prilogenie.Class.TransportType;
import java.sql.*;
import java.util.*;

public class TripPlanner { // Класс для поиска оптимального маршрута между остановками

    public static class TripSegment { // Сегмент маршрута (один маршрут без пересадок)
        public String routeNumber;
        public String typeCode;
        public String startStop;
        public String endStop;
        public List<String> stops;

        public TripSegment(String routeNumber, String typeCode, String startStop, String endStop, List<String> stops) {
            this.routeNumber = routeNumber;
            this.typeCode = typeCode;
            this.startStop = startStop;
            this.endStop = endStop;
            this.stops = stops;
        }
    }

    public static class TripResult { // Результат поиска (последовательность сегментов)
        public List<TripSegment> segments = new ArrayList<>();
        public void addSegment(TripSegment segment) {
            segments.add(segment);
        }
    }

    public static TripResult findOptimalTrip(String startStop, String endStop) { // Поиск оптимального маршрута
        List<TripResult> allPossibleTrips = new ArrayList<>();
        allPossibleTrips.addAll(findDirectTrips(startStop, endStop));
        allPossibleTrips.addAll(findOneTransferTrips(startStop, endStop));
        if (allPossibleTrips.isEmpty()) return null;
        allPossibleTrips.sort(Comparator.comparingInt(trip -> trip.segments.size()));
        return allPossibleTrips.get(0);
    }

    private static List<TripResult> findDirectTrips(String startStop, String endStop) { // Поиск прямых маршрутов
        List<TripResult> results = new ArrayList<>();
        String sql = "SELECT DISTINCT r.route_id, r.number, tt.name as type_name, rs1.direction " +
                "FROM routes r " +
                "JOIN transport_types tt ON r.type_id = tt.type_id " +
                "JOIN route_stops rs1 ON r.route_id = rs1.route_id " +
                "JOIN route_stops rs2 ON r.route_id = rs2.route_id " +
                "JOIN stops s1 ON rs1.stop_id = s1.stop_id " +
                "JOIN stops s2 ON rs2.stop_id = s2.stop_id " +
                "WHERE s1.name = ? AND s2.name = ? " +
                "AND rs1.stop_order < rs2.stop_order " +
                "AND rs1.direction = rs2.direction";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, startStop);
            statement.setString(2, endStop);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int routeId = resultSet.getInt("route_id");
                String direction = resultSet.getString("direction");
                int startOrder = getStopOrder(routeId, startStop, direction);
                int endOrder = getStopOrder(routeId, endStop, direction);
                List<String> stopsOnRoute = getStopsBetween(routeId, startOrder, endOrder, direction);
                TripResult result = new TripResult();
                result.addSegment(new TripSegment(resultSet.getString("number"),
                        TransportType.toCode(resultSet.getString("type_name")), startStop, endStop, stopsOnRoute));
                results.add(result);
            }
        } catch (Exception ignored) {}
        return results;
    }

    private static int getStopOrder(int routeId, String stopName, String direction) { // Получение порядка остановки
        String sql = "SELECT rs.stop_order FROM route_stops rs " +
                "JOIN stops s ON rs.stop_id = s.stop_id " +
                "WHERE rs.route_id = ? AND s.name = ? AND rs.direction = ? LIMIT 1";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, routeId);
            statement.setString(2, stopName);
            statement.setString(3, direction);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) return resultSet.getInt("stop_order");
        } catch (SQLException ignored) {}
        return -1;
    }

    private static List<String> getStopsBetween(int routeId, int startOrder, int endOrder, String direction) { // Получение остановок между
        List<String> stops = new ArrayList<>();
        if (startOrder < 0 || endOrder < 0 || startOrder >= endOrder) return stops;
        String sql = "SELECT s.name FROM route_stops rs " +
                "JOIN stops s ON rs.stop_id = s.stop_id " +
                "WHERE rs.route_id = ? AND rs.direction = ? AND rs.stop_order BETWEEN ? AND ? " +
                "ORDER BY rs.stop_order";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, routeId);
            statement.setString(2, direction);
            statement.setInt(3, startOrder);
            statement.setInt(4, endOrder);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                stops.add(resultSet.getString("name"));
            }
        } catch (SQLException ignored) {}
        return stops;
    }

    private static List<TripResult> findOneTransferTrips(String startStop, String endStop) { // Поиск маршрутов с одной пересадкой
        List<TripResult> results = new ArrayList<>();
        String sql = "SELECT DISTINCT " +
                "r1.route_id as route1_id, r1.number as route1_num, tt1.name as type1_name, rs1_start.direction as dir1, " +
                "r2.route_id as route2_id, r2.number as route2_num, tt2.name as type2_name, rs2_start.direction as dir2, " +
                "transfer_stop.name as transfer_stop_name " +
                "FROM stops s_start " +
                "JOIN route_stops rs1_start ON s_start.stop_id = rs1_start.stop_id " +
                "JOIN routes r1 ON rs1_start.route_id = r1.route_id " +
                "JOIN transport_types tt1 ON r1.type_id = tt1.type_id " +
                "JOIN route_stops rs1_end ON r1.route_id = rs1_end.route_id AND rs1_start.direction = rs1_end.direction " +
                "JOIN stops transfer_stop ON rs1_end.stop_id = transfer_stop.stop_id " +
                "JOIN route_stops rs2_start ON transfer_stop.stop_id = rs2_start.stop_id " +
                "JOIN routes r2 ON rs2_start.route_id = r2.route_id " +
                "JOIN transport_types tt2 ON r2.type_id = tt2.type_id " +
                "JOIN route_stops rs2_end ON r2.route_id = rs2_end.route_id AND rs2_start.direction = rs2_end.direction " +
                "JOIN stops s_end ON rs2_end.stop_id = s_end.stop_id " +
                "WHERE s_start.name = ? AND s_end.name = ? " +
                "AND rs1_start.stop_order < rs1_end.stop_order " +
                "AND rs2_start.stop_order < rs2_end.stop_order " +
                "AND r1.route_id != r2.route_id";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, startStop);
            statement.setString(2, endStop);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String transferStop = resultSet.getString("transfer_stop_name");
                List<String> firstSegmentStops = getStopsBetween(resultSet.getInt("route1_id"),
                        getStopOrder(resultSet.getInt("route1_id"), startStop, resultSet.getString("dir1")),
                        getStopOrder(resultSet.getInt("route1_id"), transferStop, resultSet.getString("dir1")),
                        resultSet.getString("dir1"));
                List<String> secondSegmentStops = getStopsBetween(resultSet.getInt("route2_id"),
                        getStopOrder(resultSet.getInt("route2_id"), transferStop, resultSet.getString("dir2")),
                        getStopOrder(resultSet.getInt("route2_id"), endStop, resultSet.getString("dir2")),
                        resultSet.getString("dir2"));
                TripResult result = new TripResult();
                result.addSegment(new TripSegment(resultSet.getString("route1_num"),
                        TransportType.toCode(resultSet.getString("type1_name")), startStop, transferStop, firstSegmentStops));
                result.addSegment(new TripSegment(resultSet.getString("route2_num"),
                        TransportType.toCode(resultSet.getString("type2_name")), transferStop, endStop, secondSegmentStops));
                results.add(result);
            }
        } catch (Exception ignored) {}
        return results;
    }
}