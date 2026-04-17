package com.example.prilogenie.Manager.Vehicle;

import com.example.prilogenie.Class.Database;
import com.example.prilogenie.Class.Route;
import com.example.prilogenie.Class.TransportType;
import com.example.prilogenie.Class.TransportVehicle;
import com.example.prilogenie.Manager.BaseManager;
import com.example.prilogenie.Utils.Alert;
import com.example.prilogenie.Utils.Table;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

public class VehicleManager extends BaseManager<TransportVehicle> { // Контроллер управления транспортными средствами
    @FXML private TableView<TransportVehicle> table; // Элементы интерфейса, связанные с FXML разметкой
    @FXML private TableColumn<TransportVehicle, String> numCol;
    @FXML private TableColumn<TransportVehicle, Integer> capCol;
    @FXML private TableColumn<TransportVehicle, String> typeCol;
    @FXML private TableColumn<TransportVehicle, String> routeCol;
    @FXML private TableColumn<TransportVehicle, Void> actCol;
    @Setter @Getter private ObservableList<TransportVehicle> list = FXCollections.observableArrayList();
    private ObservableList<TransportType> types;
    private ObservableList<Route> routes;

    @Override
    public void initialize(URL url, ResourceBundle rb) { // Инициализация контроллера
        this.tableView = table;
        this.actionsColumn = actCol;
        loadTypes();
        loadRoutes();
        setupCols();
        setupActCol();
        refreshData();
    }

    @Override
    protected void setupCols() { // Настройка колонок таблицы
        numCol.setCellValueFactory(new PropertyValueFactory<>("vehicleNumber"));
        capCol.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        typeCol.setCellValueFactory(c -> {
            int id = c.getValue().getTypeId();
            String n = types.stream()
                    .filter(t -> t.getTypeId() == id)
                    .map(TransportType::getTypeName)
                    .findFirst()
                    .orElse("-");
            return new javafx.beans.property.SimpleStringProperty(n);
        });
        routeCol.setCellValueFactory(c -> {
            Integer id = c.getValue().getRouteId();
            if (id == null) return new javafx.beans.property.SimpleStringProperty("-");
            String n = routes.stream()
                    .filter(r -> r.getRouteId() == id)
                    .map(Route::getNumber)
                    .findFirst()
                    .orElse("-");
            return new javafx.beans.property.SimpleStringProperty(n);
        });
        Table.centerCol(numCol);
        Table.centerCol(capCol);
        Table.centerCol(typeCol);
        Table.centerCol(routeCol);
        table.widthProperty().addListener((obs, o, n) -> { // Адаптивная ширина колонок
            double w = n.doubleValue();
            double aw = actCol.getWidth();
            if (aw < 10) aw = 210;
            double rw = w - aw - 20;
            if (rw > 0) {
                numCol.setPrefWidth(rw * 0.25);
                capCol.setPrefWidth(rw * 0.15);
                typeCol.setPrefWidth(rw * 0.30);
                routeCol.setPrefWidth(rw * 0.30);
            }
        });
        table.setSelectionModel(null);
    }

    @Override
    protected ObservableList<TransportVehicle> loadData() { // Загрузка ТС из БД
        ObservableList<TransportVehicle> res = FXCollections.observableArrayList();
        String sql = "SELECT vehicle_id, number, capacity, type_id, route_id FROM vehicles ORDER BY vehicle_id";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                TransportVehicle v = new TransportVehicle();
                v.setVehicleId(rs.getInt("vehicle_id"));
                v.setVehicleNumber(rs.getString("number"));
                v.setCapacity(rs.getInt("capacity"));
                v.setTypeId(rs.getInt("type_id"));
                v.setRouteId(rs.getInt("route_id"));
                res.add(v);
            }
            list.setAll(res);
        } catch (SQLException e) {
            Alert.error("Ошибка загрузки: " + e.getMessage());
        }
        return res;
    }

    private void loadTypes() { // Загрузка типов транспорта
        types = FXCollections.observableArrayList();
        String sql = "SELECT type_id, name FROM transport_types ORDER BY name";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                types.add(new TransportType(rs.getInt("type_id"), rs.getString("name"), null));
            }
        } catch (SQLException ignored) {}
    }

    private void loadRoutes() { // Загрузка активных маршрутов
        routes = FXCollections.observableArrayList();
        String sql = "SELECT route_id, number, type_id FROM routes WHERE active = true ORDER BY number";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Route r = new Route();
                r.setRouteId(rs.getInt("route_id"));
                r.setNumber(rs.getString("number"));
                r.setTypeId(rs.getInt("type_id"));
                routes.add(r);
            }
        } catch (SQLException ignored) {}
    }

    @Override
    protected void saveToDB(TransportVehicle v, boolean upd) throws SQLException { // Сохранение ТС в БД
        if (!typeExists(v.getTypeId())) {
            throw new SQLException("Тип транспорта не существует");
        }
        if (v.getRouteId() != null && !routeExists(v.getRouteId())) {
            throw new SQLException("Маршрут не существует");
        }
        String sql = upd ?
                "UPDATE vehicles SET number = ?, capacity = ?, type_id = ?, route_id = ? WHERE vehicle_id = ?" :
                "INSERT INTO vehicles (number, capacity, type_id, route_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, v.getVehicleNumber());
            ps.setInt(2, v.getCapacity());
            ps.setInt(3, v.getTypeId());
            if (v.getRouteId() == null) {
                ps.setNull(4, Types.INTEGER);
            } else {
                ps.setInt(4, v.getRouteId());
            }
            if (upd) ps.setInt(5, v.getVehicleId());
            ps.executeUpdate();
            if (!upd) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) v.setVehicleId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().equals("23505")) {
                throw new SQLException("Номер уже существует");
            } else {
                throw e;
            }
        }
    }

    private boolean typeExists(int id) { // Проверка существования типа транспорта
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM transport_types WHERE type_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean routeExists(int id) { // Проверка существования маршрута
        if (id == 0) return true;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM routes WHERE route_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    protected void showDialog(TransportVehicle v) { // Открытие диалога добавления/редактирования
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("dialog_vehicle.fxml"));
            GridPane page = loader.load();
            Dialogvehicle ctrl = loader.getController();
            ctrl.setTransportTypes(types);
            ctrl.setAllRoutes(routes);
            ctrl.setAllVehicles(list);
            ctrl.setVehicle(v);
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
            dlg.setMinHeight(400);
            dlg.setOnShown(e -> {
                if (table != null && table.getScene() != null) {
                    Stage owner = (Stage) table.getScene().getWindow();
                    dlg.setX(owner.getX() + owner.getWidth()/2 - dlg.getWidth()/2);
                    dlg.setY(owner.getY() + owner.getHeight()/2 - dlg.getHeight()/2);
                } else {
                    dlg.centerOnScreen();
                }
            });
            dlg.showAndWait();
            if (ctrl.isSaveClicked()) {
                TransportVehicle res = ctrl.getVehicle();
                handleSaveResult(v, res, v != null);
            }
        } catch (Exception e) {
            Alert.error("Ошибка: " + e.getMessage());
        }
    }

    @Override protected String getDelMsg(TransportVehicle e) { return "транспорт \"" + e.getVehicleNumber() + "\""; } // Сообщение подтверждения удаления
    @Override protected String getDelSQL() { return "DELETE FROM vehicles WHERE vehicle_id = ?"; } // SQL удаления
    @Override protected int getId(TransportVehicle e) { return e.getVehicleId(); } // Получение ID ТС
    @Override protected String getAddMsg() { return "Транспорт добавлен"; } // Сообщение об успешном добавлении
    @Override protected String getUpdMsg() { return "Транспорт обновлён"; } // Сообщение об успешном обновлении
    @Override protected String getDelMsg() { return "Транспорт удалён"; } // Сообщение об успешном удалении
    @FXML private void handleAdd() { showDialog(null); } // Обработчик добавления
    @FXML private void handleRefresh() { // Обновление списка
        loadTypes();
        loadRoutes();
        refreshData();
        Alert.success("Список обновлён");
    }
}
