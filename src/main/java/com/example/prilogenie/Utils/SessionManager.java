package com.example.prilogenie.Utils;

import lombok.Getter;
import lombok.Setter;

public class SessionManager { // Управление сессией текущего пользователя
    @Setter @Getter private static int currentUserId = -1; // ID текущего пользователя
    private static String currentUserLogin = null; // Логин текущего пользователя
    @Setter @Getter private static String currentUserRole = null; // Роль текущего пользователя
    @Setter @Getter private static String currentUserEmail = null; // Email текущего пользователя

    public static void clearSession() { // Очистка сессии при выходе
        currentUserId = -1;
        currentUserLogin = null;
        currentUserRole = null;
        currentUserEmail = null;
    }

    public static String getCurrentUsername() { return currentUserLogin; } // Получение логина
    public static boolean isAdmin() { return "ADMIN".equals(currentUserRole); } // Проверка роли администратора
    public static boolean isManager() { return "MANAGER".equals(currentUserRole); } // Проверка роли менеджера
    public static boolean isPassenger() { return "PASSENGER".equals(currentUserRole); } // Проверка роли пассажира
    public static void setCurrentUsername(String login) { currentUserLogin = login; } // Установка логина
}