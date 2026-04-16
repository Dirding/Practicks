package com.example.prilogenie.User;

import com.example.prilogenie.Class.Database;
import com.example.prilogenie.Class.Route;
import com.example.prilogenie.Utils.SessionManager;
import com.example.prilogenie.Utils.Alert;
import com.example.prilogenie.Utils.Table;
import com.example.prilogenie.Class.TransportType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ScheduleDetail { // Контроллер детального просмотра расписания маршрута
    @FXML private Label numLabel; // Элементы интерфейса, связанные с FXML разметкой
    @FXML private Label typeLabel;
    @FXML private Label costLabel;
    @FXML private Button favBtn;
    @FXML private Label stopALabel;
    @FXML private Label stopBLabel;
    @FXML private VBox stopsABox;
    @FXML private VBox stopsBBox;
    @FXML private VBox scheduleBox;
    @FXML private VBox container;
    @FXML private TableView<Display> table;
    @FXML private TableColumn<Display, String> vehicleCol;
    @FXML private TableColumn<Display, String> timeACol1, timeACol2, timeACol3, timeACol4, timeACol5;
    @FXML private TableColumn<Display, String> timeBCol1, timeBCol2, timeBCol3, timeBCol4, timeBCol5;
    private Route curRoute;
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
    private final Map<Integer, String> typeCache = new HashMap<>();

    @FXML
    public void initialize() { // Инициализация контроллера
        loadTypes();
        setupCols();
        table.setSelectionModel(null);
        table.setFocusTraversable(false);
    }

    private void setupCols() { // Настройка колонок таблицы расписания
        vehicleCol.setCellValueFactory(c -> c.getValue().vehicleNumProperty());
        timeACol1.setCellValueFactory(c -> c.getValue().timeA1Property());
        timeACol2.setCellValueFactory(c -> c.getValue().timeA2Property());
        timeACol3.setCellValueFactory(c -> c.getValue().timeA3Property());
        timeACol4.setCellValueFactory(c -> c.getValue().timeA4Property());
        timeACol5.setCellValueFactory(c -> c.getValue().timeA5Property());
        timeBCol1.setCellValueFactory(c -> c.getValue().timeB1Property());
        timeBCol2.setCellValueFactory(c -> c.getValue().timeB2Property());
        timeBCol3.setCellValueFactory(c -> c.getValue().timeB3Property());
        timeBCol4.setCellValueFactory(c -> c.getValue().timeB4Property());
        timeBCol5.setCellValueFactory(c -> c.getValue().timeB5Property());
        Table.centerCol(timeACol1); Table.centerCol(timeACol2); Table.centerCol(timeACol3);
        Table.centerCol(timeACol4); Table.centerCol(timeACol5); Table.centerCol(timeBCol1);
        Table.centerCol(timeBCol2); Table.centerCol(timeBCol3); Table.centerCol(timeBCol4);
        Table.centerCol(timeBCol5); Table.centerCol(vehicleCol);
        table.setEditable(false);
        table.setPlaceholder(new Label("Нет данных"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setRowFactory(tv -> { TableRow<Display> r = new TableRow<>(); r.setPrefHeight(40); return r; });
    }

    private void loadTypes() { // Загрузка типов транспорта
        String sql = "SELECT type_id, name FROM transport_types";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) typeCache.put(rs.getInt("type_id"), rs.getString("name"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getTypeName(int id) { // Получение названия типа
        return typeCache.getOrDefault(id, "Неизвестно");
    }

    private String getTypeCode(int id) { // Получение кода типа
        return TransportType.toCode(getTypeName(id));
    }

    public void setRoute(Route r) { // Установка маршрута
        this.curRoute = r;
        updateHeader();
        loadStops();
        loadSchedule();
    }

    private void updateHeader() { // Обновление заголовка
        if (curRoute == null) return;
        numLabel.setText("Маршрут №" + curRoute.getNumber());
        typeLabel.setText(getRussianType(getTypeCode(curRoute.getTypeId())));
        costLabel.setText(String.format("%.2f руб.", curRoute.getCost()));
        stopALabel.setText(curRoute.getStopAName() != null ? curRoute.getStopAName() : "Не указано");
        stopBLabel.setText(curRoute.getStopBName() != null ? curRoute.getStopBName() : "Не указано");
    }

    private void loadStops() { // Загрузка остановок для обоих направлений
        stopsABox.getChildren().clear();
        stopsBBox.getChildren().clear();
        loadStopsDir("A_TO_B", stopsABox);
        loadStopsDir("B_TO_A", stopsBBox);
    }

    private void loadStopsDir(String dir, VBox box) { // Загрузка остановок для направления
        String sql = "SELECT s.name, s.address, rs.stop_order FROM route_stops rs " +
                "JOIN stops s ON rs.stop_id = s.stop_id WHERE rs.route_id = ? AND rs.direction = ? ORDER BY rs.stop_order ASC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, curRoute.getRouteId());
            ps.setString(2, dir);
            ResultSet rs = ps.executeQuery();
            int cnt = 0;
            while (rs.next()) {
                cnt++;
                box.getChildren().add(createStop(cnt, rs.getString("name"), rs.getString("address")));
            }
            if (cnt == 0) {
                Label lbl = new Label("Нет остановок");
                lbl.setAlignment(Pos.CENTER);
                box.getChildren().add(lbl);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Label err = new Label("Ошибка загрузки");
            err.setAlignment(Pos.CENTER);
            box.getChildren().add(err);
        }
    }

    private HBox createStop(int num, String name, String addr) { // Создание блока остановки
        HBox box = new HBox(15);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setMaxWidth(550);
        box.getStyleClass().add("card-block");
        box.setPadding(new Insets(8, 15, 8, 15));
        Label numLbl = new Label(num + ".");
        numLbl.setMinWidth(30);
        numLbl.getStyleClass().add("text-label");
        numLbl.setStyle("-fx-font-size: 14px;");
        VBox info = new VBox(3);
        info.setAlignment(Pos.CENTER_LEFT);
        Label nameLbl = new Label(name);
        nameLbl.getStyleClass().add("text-label");
        nameLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        nameLbl.setWrapText(true);
        if (addr != null && !addr.isEmpty()) {
            Label addrLbl = new Label(addr);
            addrLbl.getStyleClass().add("text-label");
            addrLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #888888;");
            addrLbl.setWrapText(true);
            info.getChildren().addAll(nameLbl, addrLbl);
        } else {
            info.getChildren().add(nameLbl);
        }
        box.getChildren().addAll(numLbl, info);
        return box;
    }

    private void loadSchedule() { // Загрузка расписания
        List<VehicleInfo> vehicles = getVehicles();
        Map<Integer, Display> map = new LinkedHashMap<>();
        int cnt = 1;
        for (VehicleInfo v : vehicles) {
            map.put(v.id, new Display(cnt++, v.num));
        }
        String sql = "SELECT vehicle_id, direction, time FROM schedules WHERE route_id = ? ORDER BY vehicle_id, time";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, curRoute.getRouteId());
            ResultSet rs = ps.executeQuery();
            Map<Integer, List<LocalTime>> timesA = new HashMap<>();
            Map<Integer, List<LocalTime>> timesB = new HashMap<>();
            while (rs.next()) {
                int vid = rs.getInt("vehicle_id");
                String dir = rs.getString("direction");
                LocalTime t = rs.getTime("time").toLocalTime();
                if ("A_TO_B".equals(dir)) {
                    timesA.computeIfAbsent(vid, k -> new ArrayList<>()).add(t);
                } else {
                    timesB.computeIfAbsent(vid, k -> new ArrayList<>()).add(t);
                }
            }
            for (Map.Entry<Integer, Display> e : map.entrySet()) {
                int vid = e.getKey();
                Display d = e.getValue();
                List<LocalTime> a = timesA.getOrDefault(vid, new ArrayList<>());
                List<LocalTime> b = timesB.getOrDefault(vid, new ArrayList<>());
                Collections.sort(a);
                Collections.sort(b);
                for (int i = 0; i < 5; i++) {
                    if (i < a.size()) d.setTimeA(i, a.get(i).format(fmt));
                    if (i < b.size()) d.setTimeB(i, b.get(i).format(fmt));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        table.getItems().clear();
        table.getItems().addAll(map.values());
        table.refresh();
    }

    private List<VehicleInfo> getVehicles() { // Получение ТС маршрута
        List<VehicleInfo> list = new ArrayList<>();
        String sql = "SELECT vehicle_id, number FROM vehicles WHERE route_id = ? ORDER BY number";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, curRoute.getRouteId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new VehicleInfo(rs.getInt("vehicle_id"), rs.getString("number")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private String getRussianType(String t) { // Преобразование кода типа в русское название
        return TransportType.toRussian(t);
    }

    @FXML
    protected void handleAddFav() { // Добавление в избранное
        if (curRoute == null) return;
        String sql = "INSERT INTO favorites (user_id, route_id) VALUES (?, ?)";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, SessionManager.getCurrentUserId());
            ps.setInt(2, curRoute.getRouteId());
            ps.executeUpdate();
            Alert.success("Добавлено в избранное");
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                Alert.info("Уже в избранном");
            } else {
                e.printStackTrace();
                Alert.error("Ошибка");
            }
        }
    }

    @FXML
    protected void handleClose() { // Закрытие и возврат к каталогу
        try {
            ((Stage) container.getScene().getWindow()).close();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("transport.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            Alert.error("Ошибка");
        }
    }

    private static class VehicleInfo { // Вспомогательный класс для информации о ТС
        int id; String num;
        VehicleInfo(int i, String n) { id = i; num = n; }
    }

    public static class Display { // Класс для отображения расписания в таблице
        private final javafx.beans.property.StringProperty vehicleNum;
        private final javafx.beans.property.StringProperty[] timesA = new javafx.beans.property.StringProperty[5];
        private final javafx.beans.property.StringProperty[] timesB = new javafx.beans.property.StringProperty[5];
        public Display(int num, String vNum) {
            this.vehicleNum = new javafx.beans.property.SimpleStringProperty(String.valueOf(num));
            for (int i = 0; i < 5; i++) {
                timesA[i] = new javafx.beans.property.SimpleStringProperty("-");
                timesB[i] = new javafx.beans.property.SimpleStringProperty("-");
            }
        }
        public javafx.beans.property.StringProperty vehicleNumProperty() { return vehicleNum; }
        public javafx.beans.property.StringProperty timeA1Property() { return timesA[0]; }
        public javafx.beans.property.StringProperty timeA2Property() { return timesA[1]; }
        public javafx.beans.property.StringProperty timeA3Property() { return timesA[2]; }
        public javafx.beans.property.StringProperty timeA4Property() { return timesA[3]; }
        public javafx.beans.property.StringProperty timeA5Property() { return timesA[4]; }
        public javafx.beans.property.StringProperty timeB1Property() { return timesB[0]; }
        public javafx.beans.property.StringProperty timeB2Property() { return timesB[1]; }
        public javafx.beans.property.StringProperty timeB3Property() { return timesB[2]; }
        public javafx.beans.property.StringProperty timeB4Property() { return timesB[3]; }
        public javafx.beans.property.StringProperty timeB5Property() { return timesB[4]; }
        public void setTimeA(int i, String t) { if (i >=0 && i<5) timesA[i].set(t); }
        public void setTimeB(int i, String t) { if (i >=0 && i<5) timesB[i].set(t); }
    }
}