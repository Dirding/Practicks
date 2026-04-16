package com.example.prilogenie.Utils;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.net.URL;
import java.util.Optional;
import java.util.function.Consumer;

public class Alert { // Утилитарный класс для отображения стилизованных диалоговых окон
    private static final Color BG_DARK = Color.rgb(30, 30, 30); // Цвета оформления
    private static final Color TEXT_WHITE = Color.rgb(220, 220, 220);
    private static final Color GREEN = Color.rgb(0, 200, 80);
    private static final Color RED = Color.rgb(220, 50, 50);
    private static final Color ORANGE = Color.rgb(255, 140, 0);
    private static final Color BLUE = Color.rgb(0, 160, 255);

    public static void style(javafx.scene.control.Alert alert, Class<?> ctx) { // Применение стилей к диалоговому окну
        DialogPane pane = alert.getDialogPane();
        pane.setBackground(new Background(new BackgroundFill(BG_DARK, CornerRadii.EMPTY, Insets.EMPTY)));
        pane.setHeaderText(null);
        pane.setMinWidth(350);
        pane.setMaxWidth(400);
        Label content = (Label) pane.lookup(".content");
        if (content != null) {
            content.setTextFill(TEXT_WHITE);
            content.setFont(Font.font(13));
        }
        pane.getButtonTypes().forEach(btnType -> {
            Button btn = (Button) pane.lookupButton(btnType);
            if (btn != null) {
                btn.setFont(Font.font(12));
                btn.setPrefSize(90, 28);
                btn.setTextFill(TEXT_WHITE);
                btn.setBackground(new Background(new BackgroundFill(
                        btnType == ButtonType.OK || btnType == ButtonType.YES ? GREEN :
                                btnType == ButtonType.CANCEL || btnType == ButtonType.NO ? RED :
                                        Color.rgb(60, 60, 60),
                        new CornerRadii(5), Insets.EMPTY
                )));
            }
        });
        Color typeColor = getColor(alert.getAlertType());
        pane.setStyle("-fx-border-color: " + toRgb(typeColor) + "; -fx-border-width: 3 0 0 0;");
        URL css = ctx.getResource("/com/example/prilogenie/styles.css");
        if (css != null) pane.getStylesheets().add(css.toExternalForm());
    }

    private static Color getColor(javafx.scene.control.Alert.AlertType type) { // Определение цвета для типа алерта
        return switch (type) {
            case ERROR -> RED;
            case WARNING -> ORANGE;
            case INFORMATION -> BLUE;
            default -> GREEN;
        };
    }

    private static String toRgb(Color c) { // Преобразование Color в RGB строку
        return String.format("rgb(%d,%d,%d)", (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
    }

    private static void center(Stage stage) { // Центрирование окна на экране
        stage.sizeToScene();
        stage.centerOnScreen();
    }

    public static void info(String msg) { // Информационное сообщение
        Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setContentText(msg);
            style(alert, Alert.class);
            center((Stage) alert.getDialogPane().getScene().getWindow());
            alert.showAndWait();
        });
    }

    public static void success(String msg) { // Сообщение об успешной операции (автозакрытие через 1.5 сек)
        Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("");
            alert.setContentText(msg);
            style(alert, Alert.class);
            center((Stage) alert.getDialogPane().getScene().getWindow());
            alert.show();
            new Thread(() -> {
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> { if (alert.isShowing()) alert.close(); });
            }).start();
        });
    }

    public static void error(String msg) { // Сообщение об ошибке
        Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setContentText(msg);
            style(alert, Alert.class);
            center((Stage) alert.getDialogPane().getScene().getWindow());
            alert.showAndWait();
        });
    }

    public static void warn(String msg) { // Предупреждение
        Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
            alert.setContentText(msg);
            style(alert, Alert.class);
            center((Stage) alert.getDialogPane().getScene().getWindow());
            alert.showAndWait();
        });
    }

    public static boolean confirm(String msg) { // Подтверждение действия (синхронное)
        return confirm("", msg);
    }

    public static boolean confirm(String title, String msg) { // Подтверждение действия с заголовком
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        style(alert, Alert.class);
        center((Stage) alert.getDialogPane().getScene().getWindow());
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public static void confirmAsync(String msg, Consumer<Boolean> cb) { // Асинхронное подтверждение (не блокирует поток)
        Platform.runLater(() -> cb.accept(confirm(msg)));
    }

    public static void showDeleteConfirm(String item, Runnable onConfirm) { // Подтверждение удаления
        if (confirm("", "Удалить " + item + "?")) onConfirm.run();
    }
}