package com.example.prilogenie.Manager;

import com.example.prilogenie.Class.Database;
import com.example.prilogenie.Utils.Alert;
import com.example.prilogenie.Utils.Table;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ResourceBundle;

public abstract class BaseManager<T> implements Initializable { // Абстрактный базовый класс для всех менеджеров (CRUD операции)
    // Абстрактные методы, которые должны реализовать наследники
    protected abstract ObservableList<T> loadData(); // Загрузка данных из БД
    protected abstract void setupCols(); // Настройка колонок таблицы
    protected abstract void saveToDB(T e, boolean upd) throws SQLException; // Сохранение в БД (upd: true - обновление, false - добавление)
    protected abstract void showDialog(T e); // Открытие диалога для добавления/редактирования
    protected abstract String getDelMsg(T e); // Сообщение для подтверждения удаления
    protected abstract String getDelSQL(); // SQL запрос для удаления
    protected abstract int getId(T e); // Получение ID сущности
    protected abstract String getAddMsg(); // Сообщение об успешном добавлении
    protected abstract String getUpdMsg(); // Сообщение об успешном обновлении
    protected abstract String getDelMsg(); // Сообщение об успешном удалении

    protected TableView<T> tableView; // Таблица для отображения данных
    protected TableColumn<T, Void> actionsColumn; // Колонка с кнопками действий
    protected boolean hasToggleButton = false; // Флаг наличия кнопки переключения статуса

    @Override
    public void initialize(URL url, ResourceBundle rb) { // Метод инициализации (переопределяется в наследниках)
    }

    public void refreshData() { // Обновление данных в таблице
        if (tableView != null) {
            ObservableList<T> data = loadData();
            Platform.runLater(() -> {
                tableView.setItems(data);
                tableView.refresh();
            });
        }
    }

    protected void setupActCol() { // Настройка колонки с кнопками действий (редактирование, удаление, переключение статуса)
        if (actionsColumn == null) return;
        actionsColumn.setCellFactory(col -> new TableCell<T, Void>() {
            private final HBox btns = new HBox(5);
            private final Button editBtn = new Button("Изменить");
            private final Button delBtn = new Button("Удалить");
            private final Button toggleBtn = new Button();
            {
                editBtn.getStyleClass().addAll("btn-lg", "btn-orange");
                delBtn.getStyleClass().addAll("btn-lg", "btn-red");
                btns.setAlignment(Pos.CENTER);
                btns.getChildren().addAll(editBtn, delBtn);
                if (hasToggleButton) {
                    btns.getChildren().add(toggleBtn);
                }
                editBtn.setOnAction(e -> { // Обработчик редактирования
                    T item = getTableRow().getItem();
                    if (item != null) showDialog(item);
                });
                delBtn.setOnAction(e -> { // Обработчик удаления
                    T item = getTableRow().getItem();
                    if (item != null) handleDelete(item);
                });
                if (hasToggleButton) {
                    toggleBtn.setOnAction(e -> { // Обработчик переключения статуса
                        T item = getTableRow().getItem();
                        if (item != null) handleToggle(item);
                    });
                }
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    if (hasToggleButton) {
                        updateToggleButton(getTableRow().getItem(), toggleBtn);
                    }
                    setGraphic(btns);
                    setAlignment(Pos.CENTER);
                }
            }
        });
        actionsColumn.setResizable(false);
        actionsColumn.setSortable(false);
        actionsColumn.setMinWidth(350);
        actionsColumn.setPrefWidth(380);
        actionsColumn.setMaxWidth(420);
    }

    protected void updateToggleButton(T item, Button toggleBtn) { // Обновление текста кнопки переключения (переопределяется в наследниках)
    }

    protected void handleToggle(T item) { // Обработка переключения статуса (переопределяется в наследниках)
    }

    protected void handleDelete(T e) { // Удаление сущности с подтверждением
        Alert.showDeleteConfirm(getDelMsg(e), () -> {
            try (Connection conn = Database.getConnection();
                 PreparedStatement ps = conn.prepareStatement(getDelSQL())) {
                ps.setInt(1, getId(e));
                if (ps.executeUpdate() > 0) {
                    refreshData();
                    Alert.success(getDelMsg());
                }
            } catch (SQLException ex) {
                Alert.error("Ошибка: " + ex.getMessage());
            }
        });
    }

    protected void handleSaveResult(T old, T nw, boolean upd) { // Обработка результата сохранения (после закрытия диалога)
        try {
            saveToDB(nw, upd);
            refreshData();
            Alert.success(upd ? getUpdMsg() : getAddMsg());
        } catch (SQLException e) {
            Alert.error("Ошибка: " + e.getMessage());
        }
    }

    protected void centerCol(TableColumn<?, ?> col) { // Центрирование колонки таблицы
        Table.centerCol(col);
    }
}