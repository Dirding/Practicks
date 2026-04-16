package com.example.prilogenie.Manager.Schedule;

import com.example.prilogenie.Class.Database;
import com.example.prilogenie.Class.Route;
import com.example.prilogenie.Class.Schedule;
import com.example.prilogenie.Class.TransportVehicle;
import com.example.prilogenie.Manager.BaseDialog;
import com.example.prilogenie.Utils.Alert;
import com.example.prilogenie.Utils.Table;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.sql.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Dialogschedule extends BaseDialog<Schedule> { // Контроллер диалога редактирования расписания
    @FXML private TableView<Schedule> table; // Элементы интерфейса, связанные с FXML разметкой
    @FXML private TableColumn<Schedule, String> tripCol;
    @FXML private TableColumn<Schedule, Time> timeACol1, timeACol2, timeACol3, timeACol4, timeACol5;
    @FXML private TableColumn<Schedule, Time> timeBCol1, timeBCol2, timeBCol3, timeBCol4, timeBCol5;
    @FXML private Button cancelBtn;
    private Route route;
    private final ObservableList<Schedule> list = FXCollections.observableArrayList();
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
    private boolean saving = false;

    @FXML
    public void initialize() { // Инициализация таблицы и настроек редактирования
        table.getSelectionModel().setCellSelectionEnabled(true);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setItems(list);
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupCols();
    }

    private void setupCols() { // Настройка колонок таблицы расписания
        tripCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getVehicleNumber()));
        tripCol.setEditable(false);
        tripCol.setPrefWidth(100);
        Table.centerCol(tripCol);
        setupTimeCol(timeACol1, "A1", 0);
        setupTimeCol(timeACol2, "A2", 1);
        setupTimeCol(timeACol3, "A3", 2);
        setupTimeCol(timeACol4, "A4", 3);
        setupTimeCol(timeACol5, "A5", 4);
        setupTimeCol(timeBCol1, "B1", 0);
        setupTimeCol(timeBCol2, "B2", 1);
        setupTimeCol(timeBCol3, "B3", 2);
        setupTimeCol(timeBCol4, "B4", 3);
        setupTimeCol(timeBCol5, "B5", 4);
    }

    private void setupTimeCol(TableColumn<Schedule, Time> col, String id, int idx) { // Настройка колонки времени с валидацией
        col.setCellValueFactory(c -> {
            switch (id) {
                case "A1": return c.getValue().timeA1Property();
                case "A2": return c.getValue().timeA2Property();
                case "A3": return c.getValue().timeA3Property();
                case "A4": return c.getValue().timeA4Property();
                case "A5": return c.getValue().timeA5Property();
                case "B1": return c.getValue().timeB1Property();
                case "B2": return c.getValue().timeB2Property();
                case "B3": return c.getValue().timeB3Property();
                case "B4": return c.getValue().timeB4Property();
                case "B5": return c.getValue().timeB5Property();
                default: return null;
            }
        });
        col.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<Time>() {
            @Override
            public String toString(Time t) {
                return t == null ? "" : t.toLocalTime().format(fmt);
            }
            @Override
            public Time fromString(String s) {
                try {
                    if (s == null || s.trim().isEmpty()) return null;
                    return Time.valueOf(LocalTime.parse(s.trim(), fmt));
                } catch (Exception e) {
                    showError("Неверный формат. Используйте ЧЧ:ММ");
                    return null;
                }
            }
        }));
        col.getStyleClass().addAll("time-column", id);
        col.setPrefWidth(70);
        col.setStyle("-fx-alignment: CENTER;");
        final int colIdx = idx;
        final String dir = id.startsWith("A") ? "A" : "B";
        col.setOnEditCommit(e -> { // Обработчик редактирования ячейки с валидацией последовательности
            Schedule s = e.getRowValue();
            Time nw = e.getNewValue();
            // Валидация A время должно быть позже предыдущего B
            if (id.startsWith("A") && colIdx > 0) {
                Time prevB = getBTimeByIndex(s, colIdx - 1);
                if (prevB != null) {
                    if (nw == null) {
                        showError(String.format("Время А%d должно быть позже Б%d (%s)",
                                colIdx + 1, colIdx, prevB.toLocalTime().format(fmt)));
                        table.refresh();
                        return;
                    }
                    if (!nw.toLocalTime().isAfter(prevB.toLocalTime())) {
                        showError(String.format("Время А%d (%s) должно быть позже Б%d (%s)",
                                colIdx + 1, nw.toLocalTime().format(fmt),
                                colIdx, prevB.toLocalTime().format(fmt)));
                        table.refresh();
                        return;
                    }
                }
            }
            // Валидация B время должно быть позже текущего A
            if (id.startsWith("B")) {
                Time currentA = getATimeByIndex(s, colIdx);
                if (currentA != null) {
                    if (nw == null) {
                        showError(String.format("Время Б%d должно быть позже А%d (%s)",
                                colIdx + 1, colIdx + 1, currentA.toLocalTime().format(fmt)));
                        table.refresh();
                        return;
                    }
                    if (!nw.toLocalTime().isAfter(currentA.toLocalTime())) {
                        showError(String.format("Время Б%d (%s) должно быть позже А%d (%s)",
                                colIdx + 1, nw.toLocalTime().format(fmt),
                                colIdx + 1, currentA.toLocalTime().format(fmt)));
                        table.refresh();
                        return;
                    }
                }
            }
            if (!validateDirectionTime(s, dir, colIdx, nw)) {
                table.refresh();
                return;
            }
            if (id.startsWith("A")) {
                Time currentB = getBTimeByIndex(s, colIdx);
                if (currentB != null && nw != null && !nw.toLocalTime().isBefore(currentB.toLocalTime())) {
                    showError(String.format("Время А%d (%s) должно быть раньше Б%d (%s)",
                            colIdx + 1, nw.toLocalTime().format(fmt),
                            colIdx + 1, currentB.toLocalTime().format(fmt)));
                    table.refresh();
                    return;
                }
            }
            updateField(s, id, nw);
            table.refresh();
        });
    }

    private boolean validateDirectionTime(Schedule s, String dir, int curIdx, Time nw) { // Проверка возрастания времени в направлении
        if (nw == null) return true;
        LocalTime nwL = nw.toLocalTime();
        Time[] times = dir.equals("A") ?
                new Time[]{s.getTimeA1(), s.getTimeA2(), s.getTimeA3(), s.getTimeA4(), s.getTimeA5()} :
                new Time[]{s.getTimeB1(), s.getTimeB2(), s.getTimeB3(), s.getTimeB4(), s.getTimeB5()};
        for (int i = 0; i < curIdx; i++) {
            if (times[i] != null && !nwL.isAfter(times[i].toLocalTime())) {
                showError(String.format("Время в направлении %s должно идти по возрастанию",
                        dir.equals("A") ? "А→Б" : "Б→А"));
                return false;
            }
        }
        return true;
    }

    private Time getATimeByIndex(Schedule s, int index) { // Получение времени A по индексу
        switch (index) {
            case 0: return s.getTimeA1();
            case 1: return s.getTimeA2();
            case 2: return s.getTimeA3();
            case 3: return s.getTimeA4();
            case 4: return s.getTimeA5();
            default: return null;
        }
    }

    private Time getBTimeByIndex(Schedule s, int index) { // Получение времени B по индексу
        switch (index) {
            case 0: return s.getTimeB1();
            case 1: return s.getTimeB2();
            case 2: return s.getTimeB3();
            case 3: return s.getTimeB4();
            case 4: return s.getTimeB5();
            default: return null;
        }
    }

    private void updateField(Schedule s, String id, Time t) { // Обновление поля расписания
        switch (id) {
            case "A1": s.setTimeA1(t); break;
            case "A2": s.setTimeA2(t); break;
            case "A3": s.setTimeA3(t); break;
            case "A4": s.setTimeA4(t); break;
            case "A5": s.setTimeA5(t); break;
            case "B1": s.setTimeB1(t); break;
            case "B2": s.setTimeB2(t); break;
            case "B3": s.setTimeB3(t); break;
            case "B4": s.setTimeB4(t); break;
            case "B5": s.setTimeB5(t); break;
        }
    }

    public void setRoute(Route r) { // Установка маршрута и загрузка расписания
        this.route = r;
        loadSchedule();
    }

    private void loadSchedule() { // Загрузка списка транспорта маршрута
        list.clear();
        List<TransportVehicle> vehicles = loadVehicles();
        int cnt = 1;
        for (TransportVehicle v : vehicles) {
            Schedule s = new Schedule();
            s.setRouteId(route.getRouteId());
            s.setVehicleId(v.getVehicleId());
            s.setVehicleNumber(v.getVehicleNumber());
            s.setTrip(cnt++);
            clearTimes(s);
            list.add(s);
        }
        loadTimes();
    }

    private void clearTimes(Schedule s) { // Очистка всех времен
        s.setTimeA1(null); s.setTimeA2(null); s.setTimeA3(null);
        s.setTimeA4(null); s.setTimeA5(null);
        s.setTimeB1(null); s.setTimeB2(null); s.setTimeB3(null);
        s.setTimeB4(null); s.setTimeB5(null);
    }

    private List<TransportVehicle> loadVehicles() { // Загрузка транспортных средств маршрута
        List<TransportVehicle> l = new ArrayList<>();
        String sql = "SELECT vehicle_id, number FROM vehicles WHERE route_id = ? ORDER BY number";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, route.getRouteId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TransportVehicle v = new TransportVehicle();
                v.setVehicleId(rs.getInt("vehicle_id"));
                v.setVehicleNumber(rs.getString("number"));
                l.add(v);
            }
        } catch (Exception e) {
            showError("Ошибка загрузки транспорта: " + e.getMessage());
        }
        return l;
    }

    private void loadTimes() { // Загрузка существующего расписания
        String sql = "SELECT vehicle_id, direction, time, " +
                "ROW_NUMBER() OVER (PARTITION BY vehicle_id, direction ORDER BY time) as idx " +
                "FROM schedules WHERE route_id = ? ORDER BY vehicle_id, direction, idx";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, route.getRouteId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int vid = rs.getInt("vehicle_id");
                String dir = rs.getString("direction");
                Time t = rs.getTime("time");
                int idx = rs.getInt("idx") - 1;
                for (Schedule s : list) {
                    if (s.getVehicleId() == vid) {
                        setTimeFromDB(s, dir, idx, t);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            showError("Ошибка загрузки расписания: " + e.getMessage());
        }
    }

    private void setTimeFromDB(Schedule s, String dir, int idx, Time t) { // Установка времени из БД
        if (idx < 0 || idx >= 5) return;
        if ("A_TO_B".equals(dir)) {
            switch (idx) {
                case 0: s.setTimeA1(t); break;
                case 1: s.setTimeA2(t); break;
                case 2: s.setTimeA3(t); break;
                case 3: s.setTimeA4(t); break;
                case 4: s.setTimeA5(t); break;
            }
        } else if ("B_TO_A".equals(dir)) {
            switch (idx) {
                case 0: s.setTimeB1(t); break;
                case 1: s.setTimeB2(t); break;
                case 2: s.setTimeB3(t); break;
                case 3: s.setTimeB4(t); break;
                case 4: s.setTimeB5(t); break;
            }
        }
    }

    @Override
    @FXML
    protected void handleSave() { // Сохранение расписания
        if (saving) return;
        try {
            saving = true;
            if (!validateAll()) {
                saving = false;
                return;
            }
            clearAll();
            saveAll();
            dialogStage.close();
            Alert.success("Расписание сохранено");
        } catch (Exception e) {
            Alert.error("Ошибка: " + e.getMessage());
        } finally {
            saving = false;
        }
    }

    private boolean validateAll() { // Валидация всего расписания
        for (Schedule s : list) {
            if (!validateFullSequence(s)) {
                return false;
            }
            if (validateDirection(s, "A")) {
                showError("Транспорт " + s.getVehicleNumber() + ": время А→Б должно возрастать");
                return false;
            }
            if (validateDirection(s, "B")) {
                showError("Транспорт " + s.getVehicleNumber() + ": время Б→А должно возрастать");
                return false;
            }
        }
        return true;
    }

    private boolean validateFullSequence(Schedule s) { // Проверка полной последовательности A→B→A→B...
        for (int i = 0; i < 4; i++) {
            Time currentB = getBTimeByIndex(s, i);
            Time nextA = getATimeByIndex(s, i + 1);
            if (currentB != null && nextA != null && !nextA.toLocalTime().isAfter(currentB.toLocalTime())) {
                showError(String.format("Транспорт %s: время А%d (%s) должно быть позже Б%d (%s)",
                        s.getVehicleNumber(), i + 2, nextA.toLocalTime().format(fmt),
                        i + 1, currentB.toLocalTime().format(fmt)));
                return false;
            }
        }
        for (int i = 0; i < 5; i++) {
            Time aTime = getATimeByIndex(s, i);
            Time bTime = getBTimeByIndex(s, i);
            if (aTime != null && bTime != null && !aTime.toLocalTime().isBefore(bTime.toLocalTime())) {
                showError(String.format("Транспорт %s: время А%d (%s) должно быть раньше Б%d (%s)",
                        s.getVehicleNumber(), i + 1, aTime.toLocalTime().format(fmt),
                        i + 1, bTime.toLocalTime().format(fmt)));
                return false;
            }
        }
        return true;
    }

    private boolean validateDirection(Schedule s, String dir) { // Проверка возрастания в одном направлении
        Time[] times = dir.equals("A") ?
                new Time[]{s.getTimeA1(), s.getTimeA2(), s.getTimeA3(), s.getTimeA4(), s.getTimeA5()} :
                new Time[]{s.getTimeB1(), s.getTimeB2(), s.getTimeB3(), s.getTimeB4(), s.getTimeB5()};
        Time last = null;
        for (Time t : times) {
            if (t != null) {
                if (last != null && (t.toLocalTime().isBefore(last.toLocalTime()) ||
                        t.toLocalTime().equals(last.toLocalTime()))) return true;
                last = t;
            }
        }
        return false;
    }

    private void clearAll() { // Очистка старого расписания
        String sql = "DELETE FROM schedules WHERE route_id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, route.getRouteId());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка очистки", e);
        }
    }

    private void saveAll() { // Сохранение нового расписания
        String sql = "INSERT INTO schedules (route_id, vehicle_id, direction, time) VALUES (?, ?, ?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            c.setAutoCommit(false);
            for (Schedule s : list) {
                saveTimes(ps, s);
            }
            ps.executeBatch();
            c.commit();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка сохранения", e);
        }
    }

    private void saveTimes(PreparedStatement ps, Schedule s) throws SQLException { // Сохранение времен для ТС
        int vid = s.getVehicleId();
        saveDirTimes(ps, vid, "A_TO_B",
                s.getTimeA1(), s.getTimeA2(), s.getTimeA3(), s.getTimeA4(), s.getTimeA5());
        saveDirTimes(ps, vid, "B_TO_A",
                s.getTimeB1(), s.getTimeB2(), s.getTimeB3(), s.getTimeB4(), s.getTimeB5());
    }

    private void saveDirTimes(PreparedStatement ps, int vid, String dir, Time... times) throws SQLException { // Сохранение времен для направления
        for (Time t : times) {
            if (t != null) {
                ps.setInt(1, route.getRouteId());
                ps.setInt(2, vid);
                ps.setString(3, dir);
                ps.setTime(4, t);
                ps.addBatch();
            }
        }
    }

    @Override
    @FXML
    protected void handleCancel() { // Отмена
        dialogStage.close();
    }

    private void showError(String msg) { // Показ ошибки
        javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        a.setTitle("");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.initOwner(dialogStage);
        com.example.prilogenie.Utils.Alert.style(a, getClass());
        a.showAndWait();
    }

    @Override
    protected boolean isValid() { return true; } // Валидация. Реализация пустая, потому что интерфейс требует объявить
    @Override
    protected void fillEntity() {} // Заполнение сущности. Реализация пустая, потому что интерфейс требует объявить
    @Override
    protected void fillFields() {} // Заполнение полей. Реализация пустая, потому что интерфейс требует объявить

    public void setDialogStage(Stage s) { // Установка сцены диалога
        this.dialogStage = s;
    }
}