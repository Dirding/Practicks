package com.example.prilogenie.User;

import com.example.prilogenie.Class.Route;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import java.io.InputStream;
import java.util.function.Consumer;

public class RouteCard { // Контроллер карточки маршрута для отображения в каталоге
    @FXML private HBox root; // Элементы интерфейса, связанные с FXML разметкой
    @FXML private ImageView icon;
    @FXML private Label numLabel;
    @FXML private Label nameLabel;
    @FXML private Label timeLabel;
    @FXML private Label statusLabel;
    private Image busImg;
    private Image trolleyImg;
    private Image tramImg;
    private Image trainImg;
    private Image defaultImg;

    private Image loadImg(String path) { // Загрузка изображения
        try {
            InputStream is = getClass().getResourceAsStream(path);
            return is != null ? new Image(is) : null;
        } catch (Exception e) {
            return null;
        }
    }

    @FXML
    public void initialize() { // Инициализация - загрузка иконок
        busImg = loadImg("/images/Bus.png");
        trolleyImg = loadImg("/images/Trolleybus.png");
        tramImg = loadImg("/images/Tram.png");
        trainImg = loadImg("/images/Train.png");
        defaultImg = loadImg("/images/Default.png");
        if (busImg == null) busImg = defaultImg;
        if (trolleyImg == null) trolleyImg = defaultImg;
        if (tramImg == null) tramImg = defaultImg;
        if (trainImg == null) trainImg = defaultImg;
    }


    public void setData(Route r, String stopA, String stopB, String type, Consumer<Route> listener) { // Заполнение карточки данными
        if (numLabel != null) numLabel.setText("Маршрут №" + r.getNumber());
        if (nameLabel != null) nameLabel.setText(stopA + " - " + stopB);
        Image sel = defaultImg;
        switch (type) {
            case "BUS": sel = busImg; break;
            case "TROLLEYBUS": sel = trolleyImg; break;
            case "TRAM": sel = tramImg; break;
            case "TRAIN": sel = trainImg; break;
        }
        if (icon != null) icon.setImage(sel);
        if (timeLabel != null) timeLabel.setVisible(false);
        if (statusLabel != null) statusLabel.setVisible(false);
        if (root != null) {
            root.setOnMouseClicked(e -> {
                if (listener != null) listener.accept(r);
            });
        }
    }
}