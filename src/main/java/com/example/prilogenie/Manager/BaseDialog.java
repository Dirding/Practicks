package com.example.prilogenie.Manager;

import com.example.prilogenie.Utils.Alert;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;

public abstract class BaseDialog<T> { // Абстрактный базовый класс для диалогов (добавление/редактирование)
    @Setter protected Stage dialogStage; // Сцена диалогового окна
    @Getter protected boolean saveClicked = false; // Флаг: был ли нажат Save
    @Getter protected T entity; // Редактируемая сущность

    @FXML protected void handleSave() { // Обработчик кнопки сохранения
        if (!isValid()) return;
        fillEntity();
        saveClicked = true;
        dialogStage.close();
    }

    @FXML protected void handleCancel() { dialogStage.close(); } // Обработчик кнопки отмены

    protected abstract boolean isValid(); // Проверка валидности введенных данных
    protected abstract void fillEntity(); // Заполнение сущности данными из формы
    protected abstract void fillFields(); // Заполнение формы данными сущности (при редактировании)

    public void setEntity(T e) { // Установка сущности для редактирования
        this.entity = e;
        if (e != null) fillFields();
    }

    protected void showAlert(String msg) { // Показ сообщения об ошибке
        javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        a.setContentText(msg);
        a.initOwner(dialogStage);
        Alert.style(a, getClass());
        a.showAndWait();
    }
}