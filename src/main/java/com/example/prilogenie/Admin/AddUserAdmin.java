package com.example.prilogenie.Admin;

import com.example.prilogenie.Class.Database;
import com.example.prilogenie.Utils.Alert;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AddUserAdmin { // Контроллер формы добавления нового пользователя администратором
    @FXML private TextField emailFld; // Элементы интерфейса, связанные с FXML разметкой
    @FXML private TextField loginFld;
    @FXML private PasswordField passFld;
    @FXML private PasswordField confirmFld;
    @FXML private ComboBox<String> roleCombo;
    @FXML private CheckBox activeChk;

    @FXML // Метод инициализации контроллера. Вызывается автоматически при загрузке FXML
    public void initialize() {
        roleCombo.getItems().addAll("PASSENGER", "MANAGER"); // Заполнение ролей (админ создается отдельно)
        roleCombo.setValue("PASSENGER");
        activeChk.setSelected(true);
        loginFld.textProperty().addListener((obs, old, nw) -> { // Валидация логина: только буквы
            if (!nw.isEmpty() && !nw.matches("[а-яА-ЯёЁa-zA-Z]*")) loginFld.setText(old);
        });
    }


    @FXML // Обработчик сохранения нового пользователя
    private void handleSave() {
        String email = emailFld.getText().trim();
        String login = loginFld.getText().trim();
        String role = roleCombo.getValue();
        String pass = passFld.getText();
        String confirm = confirmFld.getText();
        if (email.isEmpty() || login.isEmpty() || role == null || pass.isEmpty()) { // Валидация обязательных полей
            Alert.error("Заполните все поля");
            return;
        }
        if (!email.matches("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$")) { // Проверка корректности email
            Alert.error("Некорректный email");
            return;
        }
        if (pass.length() < 8 || pass.length() > 20) { // Проверка длины пароля
            Alert.error("Пароль 8-20 символов");
            return;
        }
        if (!isValidPassword(pass)) { // Проверка сложности пароля
            Alert.error("Пароль должен содержать A-Z, a-z, 0-9 и спецсимволы");
            return;
        }
        if (!pass.equals(confirm)) { // Проверка совпадения паролей
            Alert.error("Пароли не совпадают");
            return;
        }
        int roleId = switch (role) { case "MANAGER" -> 2; default -> 1; }; // Преобразование строковой роли в числовой ID
        String sql = "INSERT INTO users (email, login, password, role_id, active) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection(); // Автоматическое закрытие соединения
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, login);
            ps.setString(3, pass);
            ps.setInt(4, roleId);
            ps.setBoolean(5, activeChk.isSelected());
            if (ps.executeUpdate() > 0) { // Успешное выполнение запроса
                Alert.success("Пользователь создан");
                ((Stage) emailFld.getScene().getWindow()).close(); // Закрытие окна
            }
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) Alert.error("Email/логин уже существует"); // Обработка нарушения уникальности
            else Alert.error("Ошибка: " + e.getMessage());
        }
    }

    private boolean isValidPassword(String p) { // Проверка сложности пароля: заглавные, строчные, цифры, спецсимволы
        return p.matches(".*[A-Z].*") && p.matches(".*[a-z].*") &&
                p.matches(".*[0-9].*") && p.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");
    }

    @FXML private void handleCancel() { ((Stage) emailFld.getScene().getWindow()).close(); } // Закрытие формы без сохранения
}