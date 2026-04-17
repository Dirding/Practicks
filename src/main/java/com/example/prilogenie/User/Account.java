package com.example.prilogenie.User;

import com.example.prilogenie.Class.Database;
import com.example.prilogenie.Class.Route;
import com.example.prilogenie.Utils.SessionManager;
import com.example.prilogenie.Utils.Alert;
import com.example.prilogenie.Utils.Table;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.*;

public class Account implements Initializable { // Контроллер личного кабинета пользователя
    @FXML private TextField emailFld; // Элементы интерфейса, связанные с FXML разметкой
    @FXML private TextField loginFld;
    @FXML private TableView<Route> favTable;
    @FXML private TableColumn<Route, String> numCol;
    @FXML private TableColumn<Route, String> nameCol;
    @FXML private TableColumn<Route, Void> actCol;
    private Map<Integer, String> typeCache = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) { // Инициализация контроллера
        loadTypes();
        loadUser();
        setupFavTable();
        loadFavs();
    }

    private void loadTypes() { // Загрузка типов транспорта
        String sql = "SELECT type_id, name FROM transport_types";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) typeCache.put(rs.getInt("type_id"), rs.getString("name"));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadUser() { // Загрузка данных текущего пользователя
        emailFld.setText(Optional.ofNullable(SessionManager.getCurrentUserEmail()).orElse(""));
        loginFld.setText(Optional.ofNullable(SessionManager.getCurrentUsername()).orElse(""));
        emailFld.setEditable(false);
        loginFld.setEditable(false);
    }

    private void setupFavTable() { // Настройка таблицы избранного
        favTable.setSelectionModel(null);
        favTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        numCol.setCellValueFactory(new PropertyValueFactory<>("number"));
        nameCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                cd.getValue().getStopAName() + " - " + cd.getValue().getStopBName()));
        numCol.prefWidthProperty().bind(favTable.widthProperty().multiply(0.2));
        nameCol.prefWidthProperty().bind(favTable.widthProperty().multiply(0.65));
        actCol.prefWidthProperty().bind(favTable.widthProperty().multiply(0.15));
        Table.centerCol(numCol);
        Table.centerCol(nameCol);
        setupActCol();
    }

    private void setupActCol() { // Настройка колонки с кнопкой удаления
        actCol.setCellFactory(col -> new TableCell<Route, Void>() {
            private final Label delLbl = new Label("✖");
            private final HBox box = new HBox(delLbl);
            {
                delLbl.setStyle("-fx-cursor: hand; -fx-font-size: 16px; -fx-text-fill: #ff4444;");
                delLbl.getStyleClass().add("delete-label");
                delLbl.setOnMouseClicked(e -> {
                    Route r = getTableView().getItems().get(getIndex());
                    if (r != null) {
                        handleDelFav(r);
                    }
                });
                box.setAlignment(Pos.CENTER);
                box.setPrefWidth(50);
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        });
    }

    private void loadFavs() { // Загрузка избранных маршрутов
        ObservableList<Route> list = FXCollections.observableArrayList();
        String sql = "SELECT r.route_id, r.number, r.type_id, sa.name AS stop_a_name, sb.name AS stop_b_name " +
                "FROM favorites fr JOIN routes r ON fr.route_id = r.route_id " +
                "JOIN stops sa ON r.stop_a_id = sa.stop_id JOIN stops sb ON r.stop_b_id = sb.stop_id " +
                "WHERE fr.user_id = ? ORDER BY fr.created DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, SessionManager.getCurrentUserId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Route r = new Route();
                r.setRouteId(rs.getInt("route_id"));
                r.setNumber(rs.getString("number"));
                r.setTypeId(rs.getInt("type_id"));
                r.setStopAName(rs.getString("stop_a_name"));
                r.setStopBName(rs.getString("stop_b_name"));
                list.add(r);
            }
            favTable.setItems(list);
        } catch (SQLException e) {
            e.printStackTrace();
            Alert.error("Ошибка загрузки избранного");
        }
    }


    private void handleDelFav(Route r) { // Удаление из избранного
        Alert.showDeleteConfirm("маршрут №" + r.getNumber(), () -> {
            String sql = "DELETE FROM favorites WHERE user_id = ? AND route_id = ?";
            try (Connection conn = Database.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, SessionManager.getCurrentUserId());
                ps.setInt(2, r.getRouteId());
                if (ps.executeUpdate() > 0) {
                    loadFavs();
                    Alert.success("Маршрут удален из избранного");
                }
            } catch (SQLException e) {
                Alert.error("Ошибка при удалении");
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void handleBack() { // Возврат к каталогу
        ((Stage) emailFld.getScene().getWindow()).close();
        navigate("/com/example/prilogenie/User/transport.fxml");
    }

    @FXML
    private void handleLogout() { // Выход из системы
        Alert.confirmAsync("Вы уверены, что хотите выйти?", res -> {
            if (res) forceLogout();
        });
    }

    private void forceLogout() { // Принудительный выход
        SessionManager.clearSession();
        ((Stage) emailFld.getScene().getWindow()).close();
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/com/example/prilogenie/Reg_Log/login.fxml")));
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            Alert.error("Ошибка при выходе");
            e.printStackTrace();
        }
    }

    private void navigate(String fxml) { // Навигация между окнами
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            Alert.error("Ошибка навигации");
            e.printStackTrace();
        }
    }
}