package com.example.prilogenie.Manager.RouteStop;

import com.example.prilogenie.Class.*;
import com.example.prilogenie.Manager.BaseDialog;
import com.example.prilogenie.Utils.Alert;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Dialogroutestop extends BaseDialog<Route> { // Контроллер диалога управления остановками маршрута
    @FXML private TableView<RouteStop> table; // Элементы интерфейса, связанные с FXML разметкой
    @FXML private TableColumn<RouteStop, String> nameCol;
    @FXML private TableColumn<RouteStop, Integer> orderCol;
    @FXML private TableColumn<RouteStop, Void> actCol;
    @FXML private ComboBox<Stop> stopCombo;
    @FXML private ComboBox<String> dirBox;
    private Route route;
    private final ObservableList<RouteStop> list = FXCollections.observableArrayList();
    private List<RouteStop> original = new ArrayList<>();
    private int startId;
    private int endId;
    private RouteStopManager mgr;

    @FXML
    public void initialize() { // Инициализация компонентов
        dirBox.getItems().addAll("A_TO_B", "B_TO_A");
        dirBox.setValue("A_TO_B");
        table.setSortPolicy(t -> false);
        nameCol.setCellValueFactory(new PropertyValueFactory<>("stopName"));
        orderCol.setCellValueFactory(new PropertyValueFactory<>("stopOrder"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        orderCol.prefWidthProperty().bind(table.widthProperty().multiply(0.1));
        nameCol.prefWidthProperty().bind(table.widthProperty().multiply(0.75));
        actCol.prefWidthProperty().bind(table.widthProperty().multiply(0.15));
        setupCellFactories();
        table.setItems(list);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        dirBox.valueProperty().addListener((obs, o, n) -> {
            loadStops();
            saveOriginal();
        });
        setupStopCombo();
        setupActCol();
        loadAllStops();
    }

    private void setupCellFactories() { // Настройка отображения ячеек таблицы
        orderCol.setCellFactory(col -> new TableCell<RouteStop, Integer>() {
            @Override protected void updateItem(Integer i, boolean e) {
                super.updateItem(i, e);
                if (e || i == null) { setText(null); setGraphic(null); }
                else { setText(i.toString()); getStyleClass().add("order-cell"); }
            }
        });
        nameCol.setCellFactory(col -> new TableCell<RouteStop, String>() {
            @Override protected void updateItem(String i, boolean e) {
                super.updateItem(i, e);
                if (e || i == null) { setText(null); setGraphic(null); }
                else { setText(i); getStyleClass().add("stop-name-cell"); }
            }
        });
    }

    private void setupActCol() { // Настройка колонки с кнопкой удаления
        actCol.setCellFactory(col -> new TableCell<RouteStop, Void>() {
            private final Label delLbl = new Label("✖");
            private final HBox box = new HBox(delLbl);
            {
                delLbl.getStyleClass().add("route-stop-delete-label");
                delLbl.setOnMouseClicked(e -> {
                    RouteStop rs = getTableView().getItems().get(getIndex());
                    handleDelete(rs);
                });
                box.setAlignment(Pos.CENTER);
                box.setPrefWidth(50);
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    RouteStop rs = getTableRow().getItem();
                    if (isFixed(rs)) {  // Показываем крестик только если остановка не является начальной/конечной
                        setGraphic(null);
                    } else {
                        setGraphic(box);
                    }
                }
            }
        });
    }

    public void setRoute(Route r) { // Установка маршрута
        this.route = r;
        this.entity = r;
        this.startId = r.getStopAId();
        this.endId = r.getStopBId();
        loadStops();
        saveOriginal();
    }

    public void setRouteStopManager(RouteStopManager manager) { // Установка менеджера для обновления
        this.mgr = manager;
    }

    private void saveOriginal() { // Сохранение исходного состояния
        original.clear();
        for (RouteStop rs : list) original.add(copy(rs));
    }

    private RouteStop copy(RouteStop o) { // Копирование остановки маршрута
        return new RouteStop(o.getRouteStopId(), o.getRouteId(), o.getStopId(), o.getDirection(),
                o.getStopOrder(), o.getStopName(), o.getRouteNumber(), o.getAddress());
    }

    private void restoreOriginal() { // Восстановление исходного состояния
        list.clear();
        for (RouteStop rs : original) list.add(copy(rs));
        table.refresh();
    }

    private void setupStopCombo() { // Настройка ComboBox для выбора остановок
        stopCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Stop i, boolean e) {
                super.updateItem(i, e);
                setText(e || i == null ? null : i.getName());
            }
        });
        stopCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Stop i, boolean e) {
                super.updateItem(i, e);
                setText(e || i == null ? null : i.getName());
            }
        });
    }

    private void loadAllStops() { // Загрузка всех остановок из БД
        String sql = "SELECT * FROM stops ORDER BY name";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            ObservableList<Stop> l = FXCollections.observableArrayList();
            while (rs.next()) {
                l.add(new Stop(rs.getInt("stop_id"), rs.getString("name"), rs.getString("address")));
            }
            stopCombo.setItems(l);
        } catch (Exception e) {
            showAlert(e.getMessage());
        }
    }

    private void loadStops() { // Загрузка остановок маршрута из БД
        list.clear();
        String sql = "SELECT rs.*, s.name, s.address FROM route_stops rs JOIN stops s " +
                "ON rs.stop_id = s.stop_id WHERE rs.route_id=? AND rs.direction=? ORDER BY rs.stop_order";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, route.getRouteId());
            ps.setString(2, dirBox.getValue());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new RouteStop(
                        rs.getInt("route_stop_id"),
                        rs.getInt("route_id"),
                        rs.getInt("stop_id"),
                        rs.getString("direction"),
                        rs.getInt("stop_order"),
                        rs.getString("name"),
                        route.getNumber(),
                        rs.getString("address")
                ));
            }
        } catch (Exception e) {
            showAlert(e.getMessage());
        }
    }

    @FXML
    private void handleAdd() { // Добавление остановки в маршрут
        Stop s = stopCombo.getValue();
        if (s == null) return;
        if (list.stream().anyMatch(rs -> rs.getStopId() == s.getStopId())) {
            showAlert("Остановка уже добавлена");
            return;
        }
        if ("A_TO_B".equals(dirBox.getValue())) {
            if (s.getStopId() == startId) { showAlert("Начальная А уже добавлена"); return; }
            if (s.getStopId() == endId) { showAlert("Конечная Б уже добавлена"); return; }
        } else {
            if (s.getStopId() == endId) { showAlert("Начальная Б уже добавлена"); return; }
            if (s.getStopId() == startId) { showAlert("Конечная А уже добавлена"); return; }
        }
        int pos;
        if (list.isEmpty()) pos = 0;
        else pos = list.size() - 1;
        RouteStop nw = new RouteStop(0, route.getRouteId(), s.getStopId(), dirBox.getValue(),
                pos + 1, s.getName(), route.getNumber(), s.getAddress());
        if (pos < list.size()) list.add(pos, nw);
        else list.add(nw);
        refreshOrder();
        selectStop(s.getStopId());
    }

    private void selectStop(int id) { // Выбор остановки в таблице
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getStopId() == id) {
                table.getSelectionModel().select(i);
                table.scrollTo(i);
                break;
            }
        }
    }

    private void handleDelete(RouteStop rs) { // Удаление остановки с подтверждением
        if (rs == null || isFixed(rs)) return;
        if (Alert.confirm("Удалить остановку \"" + rs.getStopName() + "\"?")) {
            list.remove(rs);
            refreshOrder();
        }
    }

    private void refreshOrder() { // Обновление порядка остановок
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setStopOrder(i + 1);
        }
        table.refresh();
    }

    @FXML
    private void handleUp() { // Перемещение остановки вверх
        int i = table.getSelectionModel().getSelectedIndex();
        if (i <= 0) return;
        RouteStop sel = list.get(i);
        RouteStop up = list.get(i - 1);
        if (isFixed(sel) || isFixed(up)) {
            showAlert("Нельзя менять начальную/конечную остановки");
            return;
        }
        list.set(i, up);
        list.set(i - 1, sel);
        refreshOrder();
        table.getSelectionModel().select(i - 1);
    }

    @FXML
    private void handleDown() { // Перемещение остановки вниз
        int i = table.getSelectionModel().getSelectedIndex();
        if (i < 0 || i >= list.size() - 1) return;
        RouteStop sel = list.get(i);
        RouteStop down = list.get(i + 1);
        if (isFixed(sel) || isFixed(down)) {
            showAlert("Нельзя менять начальную/конечную остановки");
            return;
        }
        list.set(i, down);
        list.set(i + 1, sel);
        refreshOrder();
        table.getSelectionModel().select(i + 1);
    }

    private boolean isFixed(RouteStop rs) { // Проверка, является ли остановка начальной/конечной
        return rs.getStopId() == startId || rs.getStopId() == endId;
    }

    @Override
    protected void fillEntity() {} // Не используется

    @Override
    protected void handleSave() { // Сохранение изменений
        if (saveStops()) {
            Alert.success("Сохранено");
            saveOriginal();
            super.handleSave();
        }
    }

    @Override
    protected void handleCancel() { // Отмена изменений
        restoreOriginal();
        super.handleCancel();
    }

    private boolean saveStops() { // Сохранение остановок в БД
        boolean hasStart = false, hasEnd = false;
        for (RouteStop rs : list) {
            if ("A_TO_B".equals(dirBox.getValue())) {
                if (rs.getStopId() == startId) hasStart = true;
                if (rs.getStopId() == endId) hasEnd = true;
            } else {
                if (rs.getStopId() == endId) hasStart = true;
                if (rs.getStopId() == startId) hasEnd = true;
            }
        }
        if (!hasStart || !hasEnd) {
            showAlert("Начальная и конечная остановки должны быть в списке");
            return false;
        }
        String del = "DELETE FROM route_stops WHERE route_id=? AND direction=?";
        String ins = "INSERT INTO route_stops(route_id,stop_id,direction,stop_order) VALUES(?,?,?,?)";
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement d = conn.prepareStatement(del)) {
                    d.setInt(1, route.getRouteId());
                    d.setString(2, dirBox.getValue());
                    d.executeUpdate();
                }
                try (PreparedStatement i = conn.prepareStatement(ins)) {
                    for (RouteStop rs : list) {
                        i.setInt(1, route.getRouteId());
                        i.setInt(2, rs.getStopId());
                        i.setString(3, dirBox.getValue());
                        i.setInt(4, rs.getStopOrder());
                        i.addBatch();
                    }
                    i.executeBatch();
                }
                conn.commit();
                if (mgr != null) javafx.application.Platform.runLater(() -> mgr.refreshRoutes());
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            showAlert("Ошибка: " + e.getMessage());
            return false;
        }
    }

    @Override protected boolean isValid() { return true; } // Валидация. Реализация пустая, потому что интерфейс требует объявить
    @Override protected void fillFields() {} // Заполнение полей. Реализация пустая, потому что интерфейс требует объявить
}