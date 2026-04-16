package com.example.prilogenie.Manager.Schedule;

import com.example.prilogenie.Class.Database;
import com.example.prilogenie.Class.Route;
import com.example.prilogenie.Class.Stop;
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
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ScheduleManager extends BaseManager<Route> { // Контроллер управления расписанием
    @FXML private TableView<Route> table; // Элементы интерфейса, связанные с FXML разметкой
    @FXML private TableColumn<Route, String> numCol;
    @FXML private TableColumn<Route, String> typeCol;
    @FXML private TableColumn<Route, String> fromCol;
    @FXML private TableColumn<Route, String> toCol;
    @FXML private TableColumn<Route, Void> actCol;
    private ObservableList<Route> list = FXCollections.observableArrayList();
    private List<Stop> stops = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) { // Инициализация контроллера
        this.tableView = table;
        this.actionsColumn = actCol;
        loadStops();
        setupCols();
        Platform.runLater(() -> refreshData());
    }

    private void loadStops() { // Загрузка остановок из БД
        stops.clear();
        String sql = "SELECT stop_id, name FROM stops ORDER BY name";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                stops.add(new Stop(rs.getInt("stop_id"), rs.getString("name"), ""));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Alert.error("Ошибка загрузки остановок: " + e.getMessage());
        }
    }

    @Override
    protected void setupCols() { // Настройка колонок таблицы
        numCol.setCellValueFactory(new PropertyValueFactory<>("number"));
        typeCol.setCellValueFactory(c -> {
            int id = c.getValue().getTypeId();
            String n;
            switch (id) {
                case 1: n = "Автобус"; break;
                case 2: n = "Троллейбус"; break;
                case 3: n = "Трамвай"; break;
                case 4: n = "Поезд"; break;
                default: n = "Неизвестно";
            }
            return new SimpleStringProperty(n);
        });
        fromCol.setCellValueFactory(c -> new SimpleStringProperty(getStopName(c.getValue().getStopAId())));
        toCol.setCellValueFactory(c -> new SimpleStringProperty(getStopName(c.getValue().getStopBId())));
        numCol.setMinWidth(100);
        typeCol.setMinWidth(100);
        fromCol.setMinWidth(200);
        toCol.setMinWidth(200);
        Table.centerCol(numCol);
        Table.centerCol(typeCol);
        Table.centerCol(fromCol);
        Table.centerCol(toCol);
        table.setItems(list);
        table.setSelectionModel(null);
        setupActCol();
        table.setRowFactory(tv -> new TableRow<Route>() {
            @Override protected void updateItem(Route i, boolean e) {
                super.updateItem(i, e);
            }
        });
    }

    private String getStopName(int id) { // Получение названия остановки по ID
        return stops.stream().filter(s -> s.getStopId() == id).map(Stop::getName).findFirst().orElse("?");
    }

    @Override
    protected void setupActCol() { // Настройка колонки с кнопкой "Расписание"
        if (actCol != null) {
            actCol.setCellFactory(col -> new TableCell<Route, Void>() {
                private final Button btn = new Button("Расписание");
                {
                    btn.getStyleClass().addAll("btn-lg", "btn-orange");
                    btn.setPrefWidth(130);
                    btn.setAlignment(Pos.CENTER);
                    btn.setOnAction(e -> {
                        Route r = getTableRow().getItem();
                        if (r != null) showDialog(r);
                    });
                }
                @Override
                protected void updateItem(Void i, boolean e) {
                    super.updateItem(i, e);
                    if (e || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                    } else {
                        setGraphic(btn);
                        setAlignment(Pos.CENTER);
                    }
                }
            });
        }
    }

    @Override
    protected ObservableList<Route> loadData() { // Загрузка маршрутов из БД
        ObservableList<Route> res = FXCollections.observableArrayList();
        String sql = "SELECT route_id, number, type_id, stop_a_id, stop_b_id, active FROM routes ORDER BY number";
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
                r.setActive(rs.getBoolean("active"));
                res.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Alert.error("Ошибка загрузки: " + e.getMessage());
        }
        return res;
    }

    @Override
    protected void showDialog(Route r) { // Открытие диалога редактирования расписания
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/prilogenie/Manager/Schedule/dialog_schedle.fxml"));
            VBox page = loader.load();
            Dialogschedule ctrl = loader.getController();
            ctrl.setRoute(r);
            Stage dlg = new Stage();
            ctrl.setDialogStage(dlg);
            Scene scene = new Scene(page);
            dlg.setScene(scene);
            if (table != null && table.getScene() != null) {
                dlg.initOwner(table.getScene().getWindow());
                dlg.initModality(Modality.WINDOW_MODAL);
            }
            dlg.setResizable(true);
            dlg.setMinWidth(1300);
            dlg.setMinHeight(600);
            dlg.setOnShown(e -> {
                Stage owner = (Stage) table.getScene().getWindow();
                dlg.setX(owner.getX() + owner.getWidth()/2 - dlg.getWidth()/2);
                dlg.setY(owner.getY() + owner.getHeight()/2 - dlg.getHeight()/2);
            });
            dlg.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            Alert.error("Ошибка загрузки: " + e.getMessage());
        }
    }

    @Override protected void saveToDB(Route e, boolean u) {} // Сохранение. Реализация пустая, потому что интерфейс требует объявить
    @Override protected String getDelMsg(Route e) { return null; } // Сообщение удаления. Реализация пустая, потому что интерфейс требует объявить
    @Override protected String getDelSQL() { return null; } // SQL удаления. Реализация пустая, потому что интерфейс требует объявить
    @Override protected int getId(Route e) { return e.getRouteId(); } // Получение ID маршрута
    @Override protected String getAddMsg() { return null; } // Сообщение добавления. Реализация пустая, потому что интерфейс требует объявить
    @Override protected String getUpdMsg() { return null; } // Сообщение обновления. Реализация пустая, потому что интерфейс требует объявить
    @Override protected String getDelMsg() { return null; } // Сообщение удаления. Реализация пустая, потому что интерфейс требует объявить
    @FXML public void handleRefresh() { loadStops(); refreshData(); Alert.success("Список обновлён"); } // Обновление списка
}