package com.example.prilogenie.Class;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class Database { // Класс управления подключением к базе данных. Реализует паттерн Singleton для единого соединения
    private static final String URL = "jdbc:postgresql://localhost:5432/Cursovay"; // URL подключения к PostgreSQL
    private static final String USER = "postgres"; // Имя пользователя БД
    private static final String PASSWORD = "ReckoRd2123"; // Пароль пользователя БД
    private static Connection connection; // Единое соединение для всего приложения

    public static Connection getConnection() throws SQLException { // Получение активного соединения с БД.
        // Создает новое если закрыто или отсутствует
        if (connection == null || connection.isClosed()) {
            Properties props = new Properties(); // Настройки подключения в виде свойств
            props.setProperty("user", USER);
            props.setProperty("password", PASSWORD);
            connection = DriverManager.getConnection(URL, props); // Установка соединения через DriverManager
        }
        return connection;
    }
}