package com.example.prilogenie.Manager.Route;

import com.example.prilogenie.Class.*;
import com.example.prilogenie.Manager.BaseManager;
import com.example.prilogenie.Utils.Alert;
import com.example.prilogenie.Utils.Table;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.Modality;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class RouteManager extends BaseManager<Route> { // Контроллер управления маршрутами
    @FXML private TableView<Route> table; // Элементы интерфейса, связанные с FXML разметкой
    @FXML private TableColumn<Route, String> numCol;
    @FXML private TableColumn<Route, String> typeCol;
    @FXML private TableColumn<Route, String> dirACol;
    @FXML private TableColumn<Route, String> dirBCol;
    @FXML private TableColumn<Route, Double> costCol;
    @FXML private TableColumn<Route, Void> actCol;
    private ObservableList<Route> list = FXCollections.observableArrayList();
    private List<Stop> stops = new ArrayList<>();
    private List<TransportType> types = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) { // Инициализация контроллера
        this.tableView = table;
        this.actionsColumn = actCol;
        this.hasToggleButton = true;
        loadStops();
        loadTypes();
        setupCols();
        setupActCol();
        Platform.runLater(() -> refreshData());
    }

    @Override
    protected void setupCols() { // Настройка колонок таблицы
        numCol.setCellValueFactory(new PropertyValueFactory<>("number"));
        typeCol.setCellValueFactory(c -> {
            int id = c.getValue().getTypeId();
            String n = types.stream().filter(t -> t.getTypeId() == id).map(TransportType::getTypeName).findFirst().orElse("Неизвестно");
            return new SimpleStringProperty(n);
        });
        dirACol.setCellValueFactory(c -> {
            int id = c.getValue().getStopAId();
            String n = stops.stream().filter(s -> s.getStopId() == id).map(Stop::getName).findFirst().orElse("?");
            return new SimpleStringProperty(n);
        });
        dirBCol.setCellValueFactory(c -> {
            int id = c.getValue().getStopBId();
            String n = stops.stream().filter(s -> s.getStopId() == id).map(Stop::getName).findFirst().orElse("?");
            return new SimpleStringProperty(n);
        });
        costCol.setCellValueFactory(new PropertyValueFactory<>("cost"));
        numCol.setMinWidth(80); numCol.setMaxWidth(120); numCol.setPrefWidth(100);
        typeCol.setMinWidth(80); typeCol.setMaxWidth(100); typeCol.setPrefWidth(90);
        dirACol.setMinWidth(150); dirACol.setPrefWidth(180);
        dirBCol.setMinWidth(150); dirBCol.setPrefWidth(180);
        costCol.setMinWidth(80); costCol.setMaxWidth(100); costCol.setPrefWidth(90);
        Table.centerCol(numCol);
        Table.centerCol(typeCol);
        Table.centerCol(dirACol);
        Table.centerCol(dirBCol);
        Table.centerCol(costCol);
        table.setItems(list);
        table.setSelectionModel(null);
    }

    @Override
    protected void updateToggleButton(Route item, Button toggleBtn) { // Обновление текста кнопки переключения статуса
        if (item.isActive()) {
            toggleBtn.setText("Отключить");
            toggleBtn.getStyleClass().removeAll("btn-green", "btn-blue", "btn-lg");
            toggleBtn.getStyleClass().addAll("btn-lg", "btn-blue");
        } else {
            toggleBtn.setText("Включить");
            toggleBtn.getStyleClass().removeAll("btn-green", "btn-blue", "btn-lg");
            toggleBtn.getStyleClass().addAll("btn-lg", "btn-green");
        }
    }

    @Override
    protected void handleToggle(Route item) { // Переключение активности маршрута
        boolean nw = !item.isActive();
        String sql = "UPDATE routes SET active = ? WHERE route_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, nw);
            ps.setInt(2, item.getRouteId());
            if (ps.executeUpdate() > 0) {
                item.setActive(nw);
                refreshData();
                Alert.success(nw ? "Маршрут активирован" : "Маршрут деактивирован");
            }
        } catch (SQLException ex) {
            Alert.error("Ошибка: " + ex.getMessage());
        }
    }

    @Override
    protected ObservableList<Route> loadData() { // Загрузка маршрутов из БД
        ObservableList<Route> res = FXCollections.observableArrayList();
        String sql = "SELECT route_id, number, type_id, stop_a_id, stop_b_id, cost, active FROM routes ORDER BY number";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Route r = new Route();
                r.setRouteId(rs.getInt("route_id"));
                r.setNumber(rs.getString("number"));
                r.setTypeId(rs.getInt("type_id"));
                r.setStopAId(rs.getInt("stop_a_id"));
                r.setStopBId(rs.getInt("stop_b_id"));
                r.setCost(rs.getDouble("cost"));
                r.setActive(rs.getBoolean("active"));
                res.add(r);
            }
        } catch (SQLException e) {
            Alert.error("Ошибка загрузки: " + e.getMessage());
        }
        return res;
    }

    @Override
    protected void saveToDB(Route r, boolean upd) throws SQLException { // Сохранение маршрута в БД с транзакцией
        Connection conn = null;
        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false);
            if (upd) {
                String sql = "UPDATE routes SET number = ?, type_id = ?, stop_a_id = ?, stop_b_id = ?, cost = ?, active = ? WHERE route_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, r.getNumber());
                    ps.setInt(2, r.getTypeId());
                    ps.setInt(3, r.getStopAId());
                    ps.setInt(4, r.getStopBId());
                    ps.setDouble(5, r.getCost());
                    ps.setBoolean(6, r.isActive());
                    ps.setInt(7, r.getRouteId());
                    ps.executeUpdate();
                }
            } else {
                String sql = "INSERT INTO routes (number, type_id, stop_a_id, stop_b_id, cost, active) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, r.getNumber());
                    ps.setInt(2, r.getTypeId());
                    ps.setInt(3, r.getStopAId());
                    ps.setInt(4, r.getStopBId());
                    ps.setDouble(5, r.getCost());
                    ps.setBoolean(6, r.isActive());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) r.setRouteId(keys.getInt(1));
                    }
                }
            }
            updateRouteStops(conn, r); // Обновление связанных остановок
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            if (e.getSQLState() != null && e.getSQLState().equals("23505")) {
                throw new SQLException("Маршрут с таким номером и типом уже существует!");
            }
            throw e;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) {}
        }
    }

    private void updateRouteStops(Connection conn, Route r) throws SQLException { // Обновление остановок маршрута
        String del = "DELETE FROM route_stops WHERE route_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(del)) {
            ps.setInt(1, r.getRouteId());
            ps.executeUpdate();
        }
        String ins = "INSERT INTO route_stops (route_id, stop_id, direction, stop_order) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(ins)) {
            ps.setInt(1, r.getRouteId());
            ps.setInt(2, r.getStopAId());
            ps.setString(3, "A_TO_B");
            ps.setInt(4, 1);
            ps.executeUpdate();
            ps.setInt(1, r.getRouteId());
            ps.setInt(2, r.getStopBId());
            ps.setString(3, "A_TO_B");
            ps.setInt(4, 2);
            ps.executeUpdate();
            ps.setInt(1, r.getRouteId());
            ps.setInt(2, r.getStopBId());
            ps.setString(3, "B_TO_A");
            ps.setInt(4, 1);
            ps.executeUpdate();
            ps.setInt(1, r.getRouteId());
            ps.setInt(2, r.getStopAId());
            ps.setString(3, "B_TO_A");
            ps.setInt(4, 2);
            ps.executeUpdate();
        }
    }

    @Override
    protected void showDialog(Route r) { // Открытие диалога добавления/редактирования
        loadStops();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("dialog_route.fxml"));
            GridPane page = loader.load();
            Dialogroute ctrl = loader.getController();
            ctrl.setEntity(r);
            ctrl.setStops(stops);
            ctrl.setTransportTypes(types);
            ctrl.setAllRoutes(list);
            Stage dlg = new Stage();
            ctrl.setDialogStage(dlg);
            Scene scene = new Scene(page);
            dlg.setScene(scene);
            dlg.setTitle("");
            if (table != null && table.getScene() != null) {
                dlg.initOwner(table.getScene().getWindow());
                dlg.initModality(Modality.WINDOW_MODAL);
            }
            dlg.setResizable(false);
            dlg.setMinWidth(600);
            dlg.setMinHeight(450);
            dlg.showAndWait();
            if (ctrl.isSaveClicked()) {
                Route res = ctrl.getEntity();
                boolean upd = r != null;
                handleSaveResult(r, res, upd);
                Platform.runLater(() -> table.refresh());
            }
        } catch (IOException e) {
            Alert.error("Ошибка загрузки: " + e.getMessage());
        }
    }

    @Override protected String getDelMsg(Route e) { return "маршрут \"" + e.getNumber() + "\""; } // Сообщение для подтверждения удаления
    @Override protected String getDelSQL() { return "DELETE FROM routes WHERE route_id = ?"; } // SQL удаления
    @Override protected int getId(Route e) { return e.getRouteId(); } // Получение ID маршрута
    @Override protected String getAddMsg() { return "Маршрут добавлен"; } // Сообщение об успешном добавлении
    @Override protected String getUpdMsg() { return "Маршрут обновлён"; } // Сообщение об успешном обновлении
    @Override protected String getDelMsg() { return "Маршрут удалён"; } // Сообщение об успешном удалении
    @FXML private void handleAdd() { showDialog(null); } // Обработчик добавления
    @FXML private void handleRefresh() { refreshData(); Alert.success("Список обновлён"); } // Обновление списка

    private void loadStops() { // Загрузка остановок из БД
        stops.clear();
        String sql = "SELECT stop_id, name, address FROM stops ORDER BY name";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                stops.add(new Stop(rs.getInt("stop_id"), rs.getString("name"), rs.getString("address")));
            }
        } catch (SQLException e) {
            Alert.error("Ошибка загрузки остановок");
        }
    }

    private void loadTypes() { // Загрузка типов транспорта из БД
        String sql = "SELECT type_id, name FROM transport_types ORDER BY name";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                types.add(new TransportType(rs.getInt("type_id"), rs.getString("name"), null));
            }
        } catch (SQLException e) {
            Alert.error("Ошибка загрузки типов");
        }
    }
}