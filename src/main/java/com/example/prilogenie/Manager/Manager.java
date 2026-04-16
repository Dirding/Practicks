package com.example.prilogenie.Manager;

import com.example.prilogenie.Utils.SessionManager;
import com.example.prilogenie.Utils.Alert;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class Manager implements Initializable { // Контроллер главного окна менеджера (вкладки управления)
    @FXML private TabPane tabPane; // Элементы интерфейса, связанные с FXML разметкой
    @FXML private Tab routesTab, stopsTab, vehiclesTab, scheduleTab, routeStopsTab;

    @Override
    public void initialize(URL url, ResourceBundle rb) { // Инициализация контроллера
        Platform.runLater(() -> { // Отложенная загрузка вкладок
            loadTab(routesTab, "/com/example/prilogenie/Manager/Route/route_manager.fxml");
            loadTab(stopsTab, "/com/example/prilogenie/Manager/Stop/stop_manager.fxml");
            loadTab(vehiclesTab, "/com/example/prilogenie/Manager/Vehicle/vehicle_manager.fxml");
            loadTab(scheduleTab, "/com/example/prilogenie/Manager/Schedule/schedule_manager.fxml");
            loadTab(routeStopsTab, "/com/example/prilogenie/Manager/RouteStop/routestop_manager.fxml");
        });
    }

    private void loadTab(Tab t, String fxml) { // Загрузка содержимого вкладки
        try {
            t.setContent(FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxml))));
        } catch (IOException e) {
            Alert.error("Ошибка загрузки вкладки: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() { // Выход из системы
        Alert.confirmAsync("Выйти?", res -> {
            if (res) {
                SessionManager.clearSession();
                ((Stage) tabPane.getScene().getWindow()).close();
                try {
                    Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/com/example/prilogenie/Reg_Log/login.fxml")));
                    Stage stage = new Stage();
                    stage.setScene(new Scene(root));
                    stage.setMaximized(true);
                    stage.show();
                } catch (IOException e) { Alert.error("Ошибка"); }
            }
        });
    }
}