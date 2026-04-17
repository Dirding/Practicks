package com.example.prilogenie.Reg_Log;

import com.example.prilogenie.Class.Database;
import com.example.prilogenie.Start;
import com.example.prilogenie.Utils.Alert;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

public class Register { // Контроллер формы регистрации
    @FXML private TextField emailFld; // Элементы интерфейса, связанные с FXML разметкой
    @FXML private TextField loginFld;
    @FXML private TextField visiblePassFld;
    @FXML private TextField visibleConfirmFld;
    @FXML private PasswordField passFld;
    @FXML private PasswordField confirmFld;
    @FXML private ToggleButton togglePassBtn;
    @FXML private ToggleButton toggleConfirmBtn;
    @FXML private Label errorLbl;
    private static final int MAX_LEN = 20;
    private static final int MAX_EMAIL = 35;

    @FXML
    public void initialize() { // Инициализация компонентов
        setupVisibility(passFld, visiblePassFld, togglePassBtn);
        setupVisibility(confirmFld, visibleConfirmFld, toggleConfirmBtn);
        setupLength(emailFld, MAX_EMAIL);
        setupLength(loginFld, MAX_LEN);
        setupLength(passFld, MAX_LEN);
        setupLength(confirmFld, MAX_LEN);
        setupLength(visiblePassFld, MAX_LEN);
        setupLength(visibleConfirmFld, MAX_LEN);
        loginFld.textProperty().addListener((obs, old, nw) -> { // Валидация логина: только буквы
            if (!nw.matches("[а-яА-ЯёЁa-zA-Z]*")) loginFld.setText(old);
        });
        setupClearError();
        emailFld.setTooltip(new Tooltip("Email: user@example.com\nМакс. " + MAX_EMAIL + " симв."));
        loginFld.setTooltip(new Tooltip("Только буквы\nМакс. " + MAX_LEN + " симв."));
        passFld.setTooltip(new Tooltip("8-20 симв., A-Z, a-z, 0-9, спецсимволы"));
    }

    private void setupVisibility(PasswordField pass, TextField text, ToggleButton btn) { // Настройка видимости пароля
        btn.selectedProperty().addListener((obs, old, sel) -> {
            pass.setVisible(!sel);
            text.setVisible(sel);
        });
        text.textProperty().bindBidirectional(pass.textProperty());
    }

    private void setupLength(TextField f, int max) { // Ограничение длины ввода
        f.textProperty().addListener((obs, old, nw) -> {
            if (nw != null && nw.length() > max) f.setText(old);
        });
    }

    private void setupClearError() { // Очистка ошибки при вводе
        if (errorLbl == null) return;
        emailFld.textProperty().addListener((obs, o, n) -> clearError());
        loginFld.textProperty().addListener((obs, o, n) -> clearError());
        passFld.textProperty().addListener((obs, o, n) -> clearError());
        confirmFld.textProperty().addListener((obs, o, n) -> clearError());
        visiblePassFld.textProperty().addListener((obs, o, n) -> clearError());
        visibleConfirmFld.textProperty().addListener((obs, o, n) -> clearError());
    }

    private void clearError() { // Очистка сообщения об ошибке
        errorLbl.setVisible(false);
        errorLbl.setText("");
    }

    @FXML
    protected void handleRegister() { // Обработчик регистрации
        String email = emailFld.getText().trim();
        String login = loginFld.getText().trim();
        String pass = passFld.getText();
        String confirm = confirmFld.getText();
        if (email.isEmpty() || login.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
            showError("Заполните все поля");
            return;
        }
        if (!isValidEmail(email)) {
            showError("Некорректный email");
            return;
        }
        if (email.length() > MAX_EMAIL) {
            showError("Email > " + MAX_EMAIL + " символов");
            return;
        }
        if (login.length() > MAX_LEN) {
            showError("Логин > " + MAX_LEN + " символов");
            return;
        }
        if (!login.matches("[а-яА-ЯёЁa-zA-Z]+")) {
            showError("Логин только буквы");
            return;
        }
        if (pass.length() < 8 || pass.length() > 20) {
            showError("Пароль 8-20 символов");
            return;
        }
        if (!isValidPassword(pass)) {
            showError("Пароль должен содержать: A-Z, a-z, 0-9 и спецсимволы");
            return;
        }
        if (!pass.equals(confirm)) {
            showError("Пароли не совпадают");
            return;
        }

        if (saveUser(email, login, pass, 1)) {
            Alert.success("Регистрация успешна!");
            switchToLogin();
        } else {
            showError("Email/логин уже существует");
        }
    }

    private boolean isValidEmail(String e) { // Проверка корректности email
        return e.matches("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");
    }

    private boolean isValidPassword(String p) { // Проверка сложности пароля
        return p.matches(".*[A-Z].*") && p.matches(".*[a-z].*") &&
                p.matches(".*[0-9].*") && p.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");
    }

    private boolean saveUser(String email, String login, String pass, int role) { // Сохранение пользователя в БД
        String sql = "INSERT INTO users (email, login, password, role_id, active) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, login);
            ps.setString(3, pass);
            ps.setInt(4, role);
            ps.setBoolean(5, true);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    private void switchToLogin() { // Переход на форму входа
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(Start.class.getResource("/com/example/prilogenie/Reg_Log/login.fxml")));
            Parent root = loader.load();
            Stage cur = (Stage) emailFld.getScene().getWindow();
            boolean max = cur.isMaximized();
            double w = cur.getWidth();
            double h = cur.getHeight();
            cur.setScene(new Scene(root));
            cur.setWidth(w);
            cur.setHeight(h);
            cur.setMaximized(max);
        } catch (IOException e) {
            showError("Ошибка перехода");
        }
    }

    @FXML
    protected void handleSwitchToLogin() { switchToLogin(); } // Переключение на вход

    private void showError(String msg) { // Показ сообщения об ошибке
        errorLbl.setText(msg);
        errorLbl.setVisible(true);
    }
}
