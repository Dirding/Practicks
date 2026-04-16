package com.example.prilogenie.Manager.Stop;

import com.example.prilogenie.Class.Stop;
import com.example.prilogenie.Manager.BaseDialog;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import lombok.Setter;
import java.util.List;

public class Dialogstop extends BaseDialog<Stop> { // Контроллер диалога добавления/редактирования остановки
    @FXML private TextField nameFld; // Элементы интерфейса, связанные с FXML разметкой
    @FXML private TextField addrFld;
    @Setter private List<Stop> allStops; // Список всех остановок для проверки уникальности

    @Override
    @FXML
    protected void handleSave() { // Обработчик сохранения
        if (isValid()) {
            if (entity == null) entity = new Stop();
            fillEntity();
            saveClicked = true;
            dialogStage.close();
        }
    }

    @Override
    protected boolean isValid() { // Валидация введенных данных
        StringBuilder err = new StringBuilder();
        String name = nameFld.getText() == null ? "" : nameFld.getText().trim();
        String addr = addrFld.getText() == null ? "" : addrFld.getText().trim();
        if (name.isEmpty()) err.append("Введите название!\n");
        if (addr.isEmpty()) err.append("Введите адрес!\n");
        if (!name.isEmpty() && allStops != null) {
            boolean dup = allStops.stream()
                    .anyMatch(s -> s.getName().trim().equalsIgnoreCase(name)
                            && (entity == null || s.getStopId() != entity.getStopId()));
            if (dup) err.append("Название уже существует!\n");
        }
        if (!err.isEmpty()) {
            showAlert(err.toString());
            return false;
        }
        return true;
    }

    @Override
    protected void fillEntity() { // Заполнение сущности данными из формы
        entity.setName(nameFld.getText().trim());
        entity.setAddress(addrFld.getText().trim());
    }

    @Override
    protected void fillFields() { // Заполнение формы данными сущности (при редактировании)
        if (entity != null) {
            nameFld.setText(entity.getName());
            addrFld.setText(entity.getAddress());
        }
    }
}