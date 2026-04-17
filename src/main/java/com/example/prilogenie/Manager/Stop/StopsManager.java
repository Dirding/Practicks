package com.example.prilogenie.Manager.Stop;

import com.example.prilogenie.Class.Database;
import com.example.prilogenie.Class.Stop;
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

public class StopsManager extends BaseManager<Stop> { // Контроллер управления остановками
    @FXML private TableView<Stop> table; // Элементы интерфейса, связанные с FXML разметкой
    @FXML private TableColumn<Stop, String> nameCol;
    @FXML private TableColumn<Stop, String> addrCol;
    @FXML private TableColumn<Stop, Void> actCol;
    @Setter @Getter private ObservableList<Stop> list = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) { // Инициализация контроллера
        this.tableView = table;
        this.actionsColumn = actCol;
        setupCols();
        setupActCol();
        refreshData();
    }

    @Override
    protected void setupCols() { // Настройка колонок таблицы
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        addrCol.setCellValueFactory(new PropertyValueFactory<>("address"));
        Table.centerCol(nameCol);
        Table.centerCol(addrCol);
        table.widthProperty().addListener((obs, o, n) -> { // Адаптивная ширина колонок
            double w = n.doubleValue();
            double aw = actCol.getWidth();
            if (aw < 10) aw = 210;
            double rw = w - aw - 20;
            if (rw > 0) {
                nameCol.setPrefWidth(rw * 0.4);
                addrCol.setPrefWidth(rw * 0.6);
            }
        });
        table.setSelectionModel(null);
    }

    @Override
    protected ObservableList<Stop> loadData() { // Загрузка остановок из БД
        ObservableList<Stop> res = FXCollections.observableArrayList();
        String sql = "SELECT stop_id, name, address FROM stops ORDER BY name";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Stop s = new Stop();
                s.setStopId(rs.getInt("stop_id"));
                s.setName(rs.getString("name"));
                s.setAddress(rs.getString("address"));
                res.add(s);
            }
            list.setAll(res);
        } catch (SQLException e) {
            e.printStackTrace();
            Alert.error("Ошибка загрузки: " + e.getMessage());
        }
        return res;
    }

    @Override
    protected void saveToDB(Stop s, boolean upd) throws SQLException { // Сохранение остановки в БД
        String sql = upd ?
                "UPDATE stops SET name = ?, address = ? WHERE stop_id = ?" :
                "INSERT INTO stops (name, address) VALUES (?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getAddress());
            if (upd) ps.setInt(3, s.getStopId());
            ps.executeUpdate();
            if (!upd) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) s.setStopId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().equals("23505")) {
                throw new SQLException("Название уже существует");
            } else throw e;
        }
    }

    @Override
    protected void showDialog(Stop s) { // Открытие диалога добавления/редактирования
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("dialog_stop.fxml"));
            GridPane page = loader.load();
            Dialogstop ctrl = loader.getController();
            ctrl.setEntity(s);
            ctrl.setAllStops(table.getItems());
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
            dlg.setMinWidth(650);
            dlg.setMinHeight(300);
            dlg.setOnShown(e -> {
                if (table != null && table.getScene() != null) {
                    Stage owner = (Stage) table.getScene().getWindow();
                    dlg.setX(owner.getX() + owner.getWidth()/2 - dlg.getWidth()/2);
                    dlg.setY(owner.getY() + owner.getHeight()/2 - dlg.getHeight()/2);
                } else dlg.centerOnScreen();
            });
            dlg.showAndWait();
            if (ctrl.isSaveClicked()) {
                Stop res = ctrl.getEntity();
                handleSaveResult(s, res, s != null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Alert.error("Ошибка: " + e.getMessage());
        }
    }

    @Override protected String getDelMsg(Stop e) { return "остановку \"" + e.getName() + "\""; } // Сообщение подтверждения удаления
    @Override protected String getDelSQL() { return "DELETE FROM stops WHERE stop_id = ?"; } // SQL удаления
    @Override protected int getId(Stop e) { return e.getStopId(); } // Получение ID остановки
    @Override protected String getAddMsg() { return "Остановка добавлена"; } // Сообщение об успешном добавлении
    @Override protected String getUpdMsg() { return "Остановка обновлена"; } // Сообщение об успешном обновлении
    @Override protected String getDelMsg() { return "Остановка удалена"; } // Сообщение об успешном удалении
    @FXML private void handleAdd() { showDialog(null); } // Обработчик добавления
    @FXML private void handleRefresh() { refreshData(); Alert.success("Список обновлён"); } // Обновление списка
}
