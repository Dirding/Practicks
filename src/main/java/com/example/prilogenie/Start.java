package com.example.prilogenie;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Start extends Application { // Точка входа в приложение
    private static final String FXML_PATH = "/com/example/prilogenie/Reg_Log/login.fxml";

    @Override
    public void start(Stage stage) { // Запуск приложения
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(FXML_PATH));
            Scene scene = new Scene(loader.load());
            stage.setTitle("");
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}