package com.example.prilogenie.Admin;

import com.example.prilogenie.Class.Database;
import com.example.prilogenie.Utils.SessionManager;
import com.example.prilogenie.Class.User;
import com.example.prilogenie.Utils.Alert;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.Objects;
import java.util.ResourceBundle;

public class Admin implements Initializable { // Контроллер административной панели. Обеспечивает управление пользователями, фильтрацию и просмотр статистики системы
    @FXML private TableView<User> table; // Элементы интерфейса, связанные с FXML разметкой
    @FXML private TableColumn<User, String> emailCol;
    @FXML private TableColumn<User, String> loginCol;
    @FXML private TableColumn<User, String> roleCol;
    @FXML private TableColumn<User, Boolean> activeCol;
    @FXML private TableColumn<User, Void> actCol;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ComboBox<String> statusFilter;

    @Override // Метод инициализации контроллера. Вызывается автоматически при загрузке FXML
    public void initialize(URL url, ResourceBundle rb) {
        setupCols();
        setupFilters();
        loadUsers();
    }

    private void setupCols() { // Настройка визуального отображения колонок таблицы. Используется PropertyValueFactory для автоматического извлечения данных из модели User
        table.setSelectionModel(null);
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        loginCol.setCellValueFactory(new PropertyValueFactory<>("login"));
        roleCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getRole()));
        activeCol.setCellValueFactory(new PropertyValueFactory<>("active"));
        com.example.prilogenie.Utils.Table.centerCol(emailCol);
        com.example.prilogenie.Utils.Table.centerCol(loginCol);
        com.example.prilogenie.Utils.Table.centerCol(roleCol);
        activeCol.setCellFactory(col -> new TableCell<>() {
            private final CheckBox cb = new CheckBox();
            { cb.setDisable(true); cb.setOpacity(1); cb.setAlignment(Pos.CENTER); }
            @Override protected void updateItem(Boolean i, boolean e) {
                super.updateItem(i, e);
                if (e || i == null) setGraphic(null);
                else { cb.setSelected(i); setGraphic(cb); setAlignment(Pos.CENTER); }
            }
        });
        setupActCol();
    }

    private void setupActCol() { // Кастомная колонка с кнопками действий (редактирование/удаление)
        actCol.setCellFactory(col -> new TableCell<>() {
            private final HBox box = new HBox(5);
            private final Button edit = new Button("Изменить");
            private final Button del = new Button("Удалить");
            private final Button toggle = new Button();
            {
                edit.getStyleClass().addAll("btn-lg", "btn-orange");
                del.getStyleClass().addAll("btn-lg", "btn-red");
                box.setAlignment(Pos.CENTER);
                box.getChildren().addAll(edit, del, toggle);
                edit.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    if (u.getId() == SessionManager.getCurrentUserId()) {
                        Alert.warn("Нельзя редактировать самого себя");
                        return;
                    }
                    Edit(u);
                });
                del.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    if (u.getId() == SessionManager.getCurrentUserId()) {
                        Alert.warn("Нельзя удалить самого себя");
                        return;
                    }
                    Delete(u);
                });
                toggle.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    if (u.getId() == SessionManager.getCurrentUserId()) {
                        Alert.warn("Нельзя деактивировать самого себя");
                        return;
                    }
                    handleToggle(u);
                });
            }

            @Override protected void updateItem(Void i, boolean e) { // Логика кнопок будет вызывать методы управления пользователями
                super.updateItem(i, e);
                if (e || getTableRow() == null || getTableRow().getItem() == null) setGraphic(null);
                else {
                    User u = getTableRow().getItem();
                    toggle.getStyleClass().removeAll("btn-green", "btn-blue", "btn-lg");

                    if (u.isActive()) {
                        toggle.setText("Деактивировать");
                        toggle.getStyleClass().addAll("btn-lg", "btn-blue");
                    } else {
                        toggle.setText("Активировать");
                        toggle.getStyleClass().addAll("btn-lg", "btn-green");
                    }
                    setGraphic(box);
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    private void setupFilters() { // Заполнение комбобоксов для фильтрации пользователей по ролям и статусу
        roleFilter.setItems(FXCollections.observableArrayList("Все роли", "PASSENGER", "MANAGER", "ADMIN"));
        statusFilter.setItems(FXCollections.observableArrayList("Все статусы", "Активные", "Неактивные"));
        roleFilter.setValue("Все роли");
        statusFilter.setValue("Все статусы");
        roleFilter.setOnAction(e -> loadUsers()); // Слушатели изменений фильтров для мгновенного обновления таблицы
        statusFilter.setOnAction(e -> loadUsers());
    }

    @FXML
    void loadUsers() { // Основной метод загрузки списка пользователей. Реализует SQL-запрос с учетом выбранных фильтров
        StringBuilder sql = new StringBuilder(
                "SELECT u.user_id, u.email, u.login, r.name as role_name, u.active, u.role_id " +
                        "FROM users u JOIN roles r ON u.role_id = r.role_id WHERE 1=1");
        if (!"Все роли".equals(roleFilter.getValue())) // Динамическое формирование SQL запроса
            sql.append(" AND r.name = '").append(roleFilter.getValue()).append("'");
        if ("Активные".equals(statusFilter.getValue())) sql.append(" AND u.active = true");
        else if ("Неактивные".equals(statusFilter.getValue())) sql.append(" AND u.active = false");
        sql.append(" ORDER BY u.user_id");
        ObservableList<User> list = FXCollections.observableArrayList();
        try (Connection conn = Database.getConnection(); // Обращение к БД идет через ресурсный блок try-with-resources для автоматического закрытия соединений
             PreparedStatement ps = conn.prepareStatement(sql.toString());
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User u = new User();
                u.setUserId(rs.getInt("user_id"));
                u.setEmail(rs.getString("email"));
                u.setLogin(rs.getString("login"));
                u.setRoleId(rs.getInt("role_id"));
                u.setActive(rs.getBoolean("active"));
                list.add(u);
            }
            table.setItems(list);
        } catch (SQLException e) {
            Alert.error("Ошибка загрузки: " + e.getMessage());
        }
    }

    @FXML protected void handleLoadUsers() { loadUsers(); Alert.success("Список обновлен"); } // Обработчик обновления списка пользователей

    @FXML protected void handleAddUser() { openDialog(null); } // Обработчик добавления нового пользователя

    private void Edit(User u) { openDialog(u); } // Обработчик кнопки редактирования

    private void Delete(User u) { // Удаление пользователя из системы. Реализовано с асинхронным подтверждением действия
        Alert.showDeleteConfirm(u.getLogin(), () -> {
            String sql = "DELETE FROM users WHERE user_id = ?";
            try (Connection conn = Database.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, u.getId());
                if (ps.executeUpdate() > 0) { loadUsers(); Alert.success("Удален"); }
            } catch (SQLException e) {
                Alert.error("Ошибка удаления");
            }
        });
    }

    private void handleToggle(User u) { // Переключение статуса активности пользователя (бан/разбан)
        String act = u.isActive() ? "Деактивировать" : "Активировать";
        Alert.confirmAsync(act + " пользователя " + u.getLogin() + "?", res -> {
            if (!res) return;
            String sql = "UPDATE users SET active = ? WHERE user_id = ?";
            try (Connection conn = Database.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setBoolean(1, !u.isActive());
                ps.setInt(2, u.getId());
                if (ps.executeUpdate() > 0) { loadUsers(); Alert.success("Статус изменен"); }
            } catch (SQLException e) {
                Alert.error("Ошибка");
            }
        });
    }

    private void openDialog(User u) { // Открытие модального окна для добавления или редактирования пользователя
        try {
            String fxml = u == null ? "add_user_admin.fxml" : "edit_user_admin.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            if (u != null) {
                EditUserAdmin ctrl = loader.getController();
                ctrl.setUserData(u);
            }
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setWidth(700);
            stage.setHeight(550);
            stage.setResizable(false);
            stage.showAndWait();
            loadUsers();
        } catch (IOException e) {
            Alert.error("Ошибка открытия формы");
        }
    }

    @FXML protected void handleStatistics() { // Метод получения статистики системы. Работа со сложными JOIN-запросами и агрегатными функциями
        try {
            String stats = String.format(
                    "Всего: %s\nАктивных: %s\nПассажиров: %s\nМенеджеров: %s\nАдминов: %s",
                    getCount("SELECT COUNT(*) FROM users"),
                    getCount("SELECT COUNT(*) FROM users WHERE active = true"),
                    getCount("SELECT COUNT(*) FROM users u JOIN roles r ON u.role_id = r.role_id WHERE r.name = 'PASSENGER'"),
                    getCount("SELECT COUNT(*) FROM users u JOIN roles r ON u.role_id = r.role_id WHERE r.name = 'MANAGER'"),
                    getCount("SELECT COUNT(*) FROM users u JOIN roles r ON u.role_id = r.role_id WHERE r.name = 'ADMIN'")
            );
            Alert.info(stats);
        } catch (SQLException e) {
            Alert.error("Ошибка статистики");
        }
    }

    private String getCount(String sql) throws SQLException { // Вспомогательный метод для выполнения одиночных агрегатных запросов
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? String.valueOf(rs.getInt(1)) : "0";
        }
    }

    @FXML protected void handleLogout() { // Метод выхода из системы. Использует асинхронное подтверждение (Alert.confirmAsync), чтобы не блокировать основной поток приложения
        Alert.confirmAsync("Выйти?", res -> {
            if (res) {
                SessionManager.clearSession();
                ((Stage) table.getScene().getWindow()).close();
                try {
                    Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/com/example/prilogenie/Reg_Log/login.fxml")));
                    Stage stage = new Stage();
                    stage.setScene(new Scene(root));
                    stage.setMaximized(true);
                    stage.show();
                } catch (IOException e) {
                    Alert.error("Ошибка");
                }
            }
        });
    }
}