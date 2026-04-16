package com.example.prilogenie.Admin;

import com.example.prilogenie.Class.Database;
import com.example.prilogenie.Class.User;
import com.example.prilogenie.Utils.Alert;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class EditUserAdmin { // Контроллер формы редактирования пользователя администратором
    @FXML private TextField emailFld; // Элементы интерфейса, связанные с FXML разметкой
    @FXML private TextField loginFld;
    @FXML private PasswordField passFld;
    @FXML private ComboBox<String> roleCombo;
    @FXML private CheckBox activeChk;
    private User curUser;

    @FXML // Метод инициализации контроллера. Вызывается автоматически при загрузке FXML
    public void initialize() {
        roleCombo.getItems().addAll("PASSENGER", "MANAGER"); // Доступные роли для назначения
        loginFld.textProperty().addListener((obs, old, nw) -> { // Валидация логина: только буквы
            if (!nw.isEmpty() && !nw.matches("[а-яА-ЯёЁa-zA-Z]*")) loginFld.setText(old);
        });
    }

    public void setUserData(User u) { // Загрузка данных пользователя в форму
        this.curUser = u;
        emailFld.setText(u.getEmail());
        loginFld.setText(u.getLogin());
        if ("ADMIN".equals(u.getRole())) { // Защита от изменения роли администратора
            roleCombo.setValue("ADMIN");
            roleCombo.setDisable(true); // Блокировка выбора роли для админов
            Alert.info("Редактирование администратора ограничено");
        } else {
            roleCombo.setValue(u.getRole());
        }
        activeChk.setSelected(u.isActive());
    }

    @FXML // Обработчик сохранения изменений
    private void handleSave() {
        String email = emailFld.getText().trim();
        String login = loginFld.getText().trim();
        String role = roleCombo.getValue();
        String nwPass = passFld.getText();
        if (email.isEmpty() || login.isEmpty() || role == null) { // Валидация обязательных полей
            Alert.error("Заполните email, логин и роль");
            return;
        }
        if (!email.matches("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$")) { // Проверка корректности email
            Alert.error("Некорректный email");
            return;
        }
        if (!nwPass.isEmpty() && (nwPass.length() < 8 || nwPass.length() > 20)) { // Валидация пароля только если он был введен (опциональное изменение)
            Alert.error("Пароль 8-20 символов");
            return;
        }
        if (!nwPass.isEmpty() && !isValidPassword(nwPass)) {
            Alert.error("Пароль должен содержать A-Z, a-z, 0-9 и спецсимволы");
            return;
        }
        int roleId; // Определение ID роли с защитой для администратора
        if ("ADMIN".equals(curUser.getRole())) roleId = 3; // Админ сохраняет свою роль
        else roleId = switch (role) { case "MANAGER" -> 2; default -> 1; };
        StringBuilder sql = new StringBuilder("UPDATE users SET email = ?, login = ?, role_id = ?, active = ?"); // Динамическое построение SQL запроса: пароль обновляется только если введен
        if (!nwPass.isEmpty()) sql.append(", password = ?");
        sql.append(" WHERE user_id = ?");
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setString(idx++, email);
            ps.setString(idx++, login);
            ps.setInt(idx++, roleId);
            ps.setBoolean(idx++, activeChk.isSelected());
            if (!nwPass.isEmpty()) ps.setString(idx++, nwPass);
            ps.setInt(idx, curUser.getId());
            if (ps.executeUpdate() > 0) { // Успешное обновление
                Alert.success("Обновлено");
                ((Stage) emailFld.getScene().getWindow()).close(); // Закрытие окна
            }
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) Alert.error("Email/логин уже существует"); // Обработка нарушения уникальности email/логина
            else Alert.error("Ошибка: " + e.getMessage());
        }
    }

    private boolean isValidPassword(String p) { // Проверка сложности пароля
        return p.matches(".*[A-Z].*") && p.matches(".*[a-z].*") &&
                p.matches(".*[0-9].*") && p.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");
    }

    @FXML private void handleCancel() { ((Stage) emailFld.getScene().getWindow()).close(); } // Закрытие формы без сохранения
}