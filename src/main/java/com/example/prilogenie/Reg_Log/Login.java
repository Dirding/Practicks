package com.example.prilogenie.Reg_Log;

import com.example.prilogenie.Class.Database;
import com.example.prilogenie.Utils.SessionManager;
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class Login { // Контроллер формы входа в систему
    @FXML private TextField loginFld; // Элементы интерфейса, связанные с FXML разметкой
    @FXML private TextField visiblePassFld;
    @FXML private PasswordField passFld;
    @FXML private ToggleButton toggleBtn;
    @FXML private Label errorLbl;

    @FXML
    public void initialize() { // Инициализация компонентов
        visiblePassFld.textProperty().bindBidirectional(passFld.textProperty());
        toggleBtn.selectedProperty().addListener((obs, old, sel) -> { // Переключение видимости пароля
            passFld.setVisible(!sel);
            visiblePassFld.setVisible(sel);
        });
        setupLength(loginFld, 35);
        setupLength(passFld, 20);
        setupLength(visiblePassFld, 20);
        if (errorLbl != null) { // Очистка ошибки при вводе
            loginFld.textProperty().addListener((obs, o, n) -> clearError());
            passFld.textProperty().addListener((obs, o, n) -> clearError());
            visiblePassFld.textProperty().addListener((obs, o, n) -> clearError());
        }
    }

    private void setupLength(TextField f, int max) { // Ограничение длины ввода
        f.textProperty().addListener((obs, old, nw) -> {
            if (nw != null && nw.length() > max) f.setText(old);
        });
    }

    private void clearError() { // Очистка сообщения об ошибке
        errorLbl.setVisible(false);
        errorLbl.setText("");
    }

    @FXML
    protected void handleLogin() { // Обработчик входа
        String login = loginFld.getText().trim();
        String pass = passFld.getText();
        if (login.isEmpty() || pass.isEmpty()) {
            showError("Заполните поля");
            return;
        }
        if (validateUser(login, pass)) {
            handleSuccess();
        } else {
            showError("Неверный логин/email или пароль");
        }
    }

    private boolean validateUser(String login, String pass) { // Проверка учетных данных
        String sql = "SELECT u.user_id, u.email, u.login, r.name FROM users u " +
                "JOIN roles r ON u.role_id = r.role_id " +
                "WHERE (u.email = ? OR u.login = ?) AND u.password = ? AND u.active = true";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, login);
            ps.setString(2, login);
            ps.setString(3, pass);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                SessionManager.setCurrentUserId(rs.getInt("user_id"));
                SessionManager.setCurrentUsername(rs.getString("login"));
                SessionManager.setCurrentUserEmail(rs.getString("email"));
                SessionManager.setCurrentUserRole(rs.getString("name"));
                return true;
            }
        } catch (SQLException e) {
            showError("Ошибка БД");
        }
        return false;
    }

    private void handleSuccess() { // Успешный вход - переход в соответствующее окно
        try {
            String role = SessionManager.getCurrentUserRole();
            String fxml;
            switch (role) {
                case "ADMIN": fxml = "/com/example/prilogenie/Admin/admin.fxml"; break;
                case "MANAGER": fxml = "/com/example/prilogenie/Manager/manager.fxml"; break;
                default: fxml = "/com/example/prilogenie/User/transport.fxml";
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Stage cur = (Stage) loginFld.getScene().getWindow();
            boolean max = cur.isMaximized();
            double w = cur.getWidth();
            double h = cur.getHeight();
            cur.setScene(new Scene(root));
            cur.setWidth(w);
            cur.setHeight(h);
            cur.setMaximized(max);
        } catch (IOException e) {
            Alert.error("Ошибка: " + e.getMessage());
        }
    }

    @FXML
    protected void handleRegister() { switchToRegister(); } // Переход к регистрации

    private void switchToRegister() { // Переключение на форму регистрации
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(Start.class.getResource("/com/example/prilogenie/Reg_Log/register.fxml")));
            Parent root = loader.load();
            Stage cur = (Stage) loginFld.getScene().getWindow();
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

    private void showError(String msg) { // Показ сообщения об ошибке
        errorLbl.setText(msg);
        errorLbl.setVisible(true);
    }
}
