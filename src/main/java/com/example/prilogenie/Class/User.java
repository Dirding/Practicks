package com.example.prilogenie.Class;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class User { // Модель пользователя системы (пассажир, менеджер, администратор)
    private int userId; // Уникальный идентификатор пользователя
    private int roleId; // ID роли (1 - пассажир, 2 - менеджер, 3 - администратор)
    private String email; // Адрес электронной почты (уникальный)
    private String login; // Логин для входа (уникальный)
    private String password; // Пароль
    private boolean active; // Статус активности (заблокирован/активен)

    public int getId() { return userId; } // Геттер для совместимости с TableView
    public void setId(int id) { this.userId = id; } // Сеттер для совместимости

    public String getRole() { // Преобразование ID роли в строковое представление
        return switch (roleId) {
            case 2 -> "MANAGER";
            case 3 -> "ADMIN";
            default -> "PASSENGER";
        };
    }
}

