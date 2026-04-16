package com.example.prilogenie.User;

import com.example.prilogenie.Class.Database;
import com.example.prilogenie.Class.Route;
import com.example.prilogenie.Utils.Alert;
import com.example.prilogenie.Class.TransportType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class Transport implements Initializable { // Контроллер главного окна пользователя (каталог маршрутов и планировщик)
    public StackPane contentStackPane;
    @FXML private BorderPane catalogView; // Элементы интерфейса, связанные с FXML разметкой
    @FXML private VBox plannerView;
    @FXML private ToggleButton catalogBtn;
    @FXML private ToggleButton plannerBtn;
    @FXML private VBox routesBox;
    @FXML private ToggleGroup typeGroup;
    @FXML private ToggleButton allTag;
    @FXML private ToggleButton busTag;
    @FXML private ToggleButton trolleyTag;
    @FXML private ToggleButton tramTag;
    @FXML private ToggleButton trainTag;
    @FXML private VBox tripResultContainer;
    @FXML private ComboBox<String> stopACombo;
    @FXML private ComboBox<String> stopBCombo;
    @FXML private TextField searchField;
    @FXML private ToggleGroup viewGroup;
    private ObservableList<Route> routes = FXCollections.observableArrayList();
    private ObservableList<String> stops = FXCollections.observableArrayList();
    private final Map<Integer, List<String>> stopsCache = new HashMap<>();
    private final Map<Integer, String> typeCache = new HashMap<>();
    private final Map<Integer, List<StopInfo>> routeStopOrders = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) { // Инициализация контроллера
        loadTypes();
        initUI();
        setupGroups();
        setupView();
        setupFilters();
        loadStops();
        loadRoutes();
    }

    private void loadTypes() { // Загрузка типов транспорта
        String sql = "SELECT type_id, name FROM transport_types";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                typeCache.put(rs.getInt("type_id"), TransportType.toCode(rs.getString("name")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getTypeCode(int id) { // Получение кода типа
        return typeCache.getOrDefault(id, "BUS");
    }

    private String getTypeName(String code) { // Получение названия типа
        return TransportType.toRussian(code);
    }

    private void initUI() { // Инициализация видимости панелей
        if (catalogView != null) catalogView.setVisible(true);
        if (plannerView != null) plannerView.setVisible(false);
    }

    private void setupGroups() { // Настройка групп переключателей
        if (allTag != null) allTag.setSelected(true);
    }

    private void setupView() { // Настройка переключения между каталогом и планировщиком
        if (viewGroup == null) {
            viewGroup = new ToggleGroup();
            if (catalogBtn != null) catalogBtn.setToggleGroup(viewGroup);
            if (plannerBtn != null) plannerBtn.setToggleGroup(viewGroup);
        }
        if (catalogBtn != null) catalogBtn.setSelected(true);
        viewGroup.selectedToggleProperty().addListener((obs, old, nw) -> {
            if (nw == null) {
                if (old != null) old.setSelected(true);
            } else {
                boolean cat = nw == catalogBtn;
                if (catalogView != null) catalogView.setVisible(cat);
                if (plannerView != null) plannerView.setVisible(!cat);
            }
        });
    }

    private void setupFilters() { // Настройка фильтров
        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        }
        if (typeGroup != null) {
            typeGroup.selectedToggleProperty().addListener((obs, o, n) -> applyFilters());
        }
    }

    private void loadStops() { // Загрузка списка остановок
        stops.clear();
        String sql = "SELECT name FROM stops ORDER BY name";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) stops.add(rs.getString("name"));
            Platform.runLater(() -> {
                if (stopACombo != null) stopACombo.setItems(stops);
                if (stopBCombo != null) stopBCombo.setItems(stops);
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadRoutes() { // Загрузка маршрутов
        routes.clear();
        stopsCache.clear();
        routeStopOrders.clear();
        String sql = "SELECT r.route_id, r.number, r.type_id, sa.name AS stop_a_name, " +
                "sb.name AS stop_b_name, r.stop_a_id, r.stop_b_id, r.cost, r.active " +
                "FROM routes r JOIN stops sa ON r.stop_a_id = sa.stop_id " +
                "JOIN stops sb ON r.stop_b_id = sb.stop_id WHERE r.active = true ORDER BY r.number";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Route r = new Route();
                r.setRouteId(rs.getInt("route_id"));
                r.setNumber(rs.getString("number"));
                r.setTypeId(rs.getInt("type_id"));
                r.setStopAName(rs.getString("stop_a_name"));
                r.setStopBName(rs.getString("stop_b_name"));
                r.setCost(rs.getDouble("cost"));
                r.setActive(rs.getBoolean("active"));
                routes.add(r);
                loadRouteStops(r.getRouteId());
            }
            applyFilters();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadRouteStops(int routeId) { // Загрузка остановок маршрута
        List<StopInfo> stopList = new ArrayList<>();
        String sql = "SELECT s.stop_id, s.name, rs.stop_order, rs.direction " +
                "FROM route_stops rs JOIN stops s ON rs.stop_id = s.stop_id " +
                "WHERE rs.route_id = ? ORDER BY rs.stop_order";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, routeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                stopList.add(new StopInfo(
                        rs.getString("name"),
                        rs.getInt("stop_order"),
                        rs.getString("direction")
                ));
            }
            routeStopOrders.put(routeId, stopList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void applyFilters() { // Применение фильтров к списку маршрутов
        if (routesBox == null) return;
        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        String typeCode;
        Toggle sel = typeGroup == null ? null : typeGroup.getSelectedToggle();
        if (sel == busTag) typeCode = "BUS";
        else if (sel == trolleyTag) typeCode = "TROLLEYBUS";
        else if (sel == tramTag) typeCode = "TRAM";
        else if (sel == trainTag) typeCode = "TRAIN";
        else typeCode = "ALL";
        List<Route> filtered = routes.stream()
                .filter(r -> {
                    String code = getTypeCode(r.getTypeId());
                    if (!typeCode.equals("ALL") && !code.equals(typeCode)) return false;
                    if (search.isEmpty()) return true;
                    if (r.getNumber() != null && r.getNumber().toLowerCase().contains(search)) return true;
                    return getStops(r).stream().anyMatch(s -> s.toLowerCase().contains(search));
                })
                .collect(Collectors.toList());
        updateCards(filtered);
    }

    private void updateCards(List<Route> list) { // Обновление карточек маршрутов
        if (routesBox == null) return;
        Platform.runLater(() -> {
            routesBox.getChildren().clear();
            if (list.isEmpty()) {
                routesBox.getChildren().add(new Label("Маршруты не найдены."));
                return;
            }
            for (Route r : list) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("route_card.fxml"));
                    Node card = loader.load();
                    RouteCard ctrl = loader.getController();
                    ctrl.setData(r, r.getStopAName(), r.getStopBName(), getTypeCode(r.getTypeId()), this::openSchedule);
                    routesBox.getChildren().add(card);
                } catch (Exception e) {
                    Button btn = getFallbackBtn(r);
                    routesBox.getChildren().add(btn);
                }
            }
        });
    }

    @NotNull
    private Button getFallbackBtn(Route r) { // Создание кнопки-заглушки при ошибке загрузки карточки
        Button btn = new Button(r.getNumber() + " - " + r.getStopAName() + " → " + r.getStopBName());
        btn.setOnAction(e -> openSchedule(r));
        return btn;
    }

    private void openSchedule(Route r) { // Открытие детального расписания
        try {
            Stage cur = (Stage) catalogView.getScene().getWindow();
            cur.close();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("schedule_detail.fxml"));
            Parent root = loader.load();
            ScheduleDetail ctrl = loader.getController();
            ctrl.setRoute(r);
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            Alert.error("Ошибка: " + e.getMessage());
        }
    }

    @FXML
    protected void handleFindTrip() { // Поиск маршрута между остановками
        String start = stopACombo.getValue();
        String end = stopBCombo.getValue();
        if (start == null || end == null || start.trim().isEmpty() || end.trim().isEmpty()) {
            Alert.warn("Выберите остановки.");
            return;
        }
        if (start.equals(end)) {
            Alert.warn("Остановки должны быть разными.");
            return;
        }
        TripPlanner.TripResult trip = TripPlanner.findOptimalTrip(start, end);
        displayTripResult(trip);
    }

    private void displayTripResult(TripPlanner.TripResult trip) { // Отображение результата поиска
        if (tripResultContainer == null) return;
        tripResultContainer.getChildren().clear();
        if (trip == null || trip.segments.isEmpty()) {
            Label noResult = new Label("Маршрут не найден");
            noResult.getStyleClass().add("text-label");
            tripResultContainer.getChildren().add(noResult);
            return;
        }
        for (int i = 0; i < trip.segments.size(); i++) {
            TripPlanner.TripSegment seg = trip.segments.get(i);
            VBox stageBlock = new VBox();
            stageBlock.setSpacing(8);
            stageBlock.setPadding(new Insets(15, 20, 15, 20));
            stageBlock.getStyleClass().add("card-block");
            HBox typeBadge = getHBox(seg);
            Label routeInfo = new Label("Маршрут " + seg.routeNumber);
            routeInfo.getStyleClass().add("text-label");
            routeInfo.setStyle("-fx-font-size: 16px;");
            Label routePath = new Label(seg.startStop + " → " + seg.endStop);
            routePath.getStyleClass().add("text-label");
            routePath.setStyle("-fx-font-size: 13px; -fx-text-fill: #888888;");
            stageBlock.getChildren().addAll(typeBadge, routeInfo, routePath);
            tripResultContainer.getChildren().add(stageBlock);
            if (i < trip.segments.size() - 1) {
                Label transferLabel = new Label("ПЕРЕСАДКА");
                transferLabel.setAlignment(Pos.CENTER);
                transferLabel.setMaxWidth(Double.MAX_VALUE);
                transferLabel.getStyleClass().add("text-label");
                transferLabel.setStyle("-fx-text-fill: #ffaa00; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 10 0 10 0;");
                tripResultContainer.getChildren().add(transferLabel);
            }
        }
    }

    @NotNull
    private HBox getHBox(TripPlanner.TripSegment seg) { // Создание блока с типом транспорта
        Label typeLabel = new Label(getTypeName(seg.typeCode).toUpperCase());
        typeLabel.setStyle("-fx-text-fill: #000000; -fx-font-weight: bold; -fx-font-size: 11px;");
        HBox typeBadge = new HBox(typeLabel);
        typeBadge.setAlignment(Pos.CENTER);
        typeBadge.setPadding(new Insets(2, 10, 2, 10));
        typeBadge.setStyle("-fx-background-color: #00ff88; -fx-background-radius: 15;");
        typeBadge.setMaxWidth(Region.USE_PREF_SIZE);
        return typeBadge;
    }

    @FXML
    protected void handleAccount() { // Переход в личный кабинет
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("account.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
            ((Stage) catalogView.getScene().getWindow()).close();
        } catch (IOException e) {
            Alert.error("Ошибка: " + e.getMessage());
        }
    }

    private List<String> getStops(Route r) { // Получение списка остановок маршрута
        int id = r.getRouteId();
        if (stopsCache.containsKey(id)) return stopsCache.get(id);
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT s.name FROM route_stops rs JOIN stops s ON rs.stop_id = s.stop_id WHERE rs.route_id = ? ORDER BY s.name";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String n = rs.getString("name");
                    if (n != null && !n.trim().isEmpty()) list.add(n.toLowerCase());
                }
            }
            stopsCache.put(id, list);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private static class StopInfo { // Вспомогательный класс для информации об остановке
        String name;
        int order;
        String direction;
        StopInfo(String n, int o, String d) { this.name = n; this.order = o; this.direction = d; }
    }
}