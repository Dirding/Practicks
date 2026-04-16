package com.example.prilogenie.Utils;

import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

public class Table { // Утилитарный класс для настройки таблиц
    public static <T, S> void centerCol(TableColumn<T, S> col) { // Центрирование содержимого колонки таблицы
        col.setCellFactory(tc -> new TableCell<T, S>() {
            @Override
            protected void updateItem(S item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.toString());
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }
}