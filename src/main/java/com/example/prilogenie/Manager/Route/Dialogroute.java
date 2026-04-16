package com.example.prilogenie.Manager.Route;

import com.example.prilogenie.Class.Route;
import com.example.prilogenie.Class.Stop;
import com.example.prilogenie.Class.TransportType;
import com.example.prilogenie.Manager.BaseDialog;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.Setter;
import java.util.List;

public class Dialogroute extends BaseDialog<Route> { // Контроллер диалога добавления/редактирования маршрута
    @FXML private TextField numFld; // Элементы интерфейса, связанные с FXML разметкой
    @FXML private ComboBox<Stop> stopACombo;
    @FXML private ComboBox<Stop> stopBCombo;
    @FXML private TextField costFld;
    @FXML private ComboBox<TransportType> typeCombo;
    private List<Stop> stops;
    private List<TransportType> types;
    @Setter private List<Route> allRoutes;

    @FXML
    private void initialize() { // Настройка отображения элементов ComboBox
        stopACombo.setCellFactory(c -> new StopListCell());
        stopACombo.setButtonCell(new StopListCell());
        stopBCombo.setCellFactory(c -> new StopListCell());
        stopBCombo.setButtonCell(new StopListCell());
        typeCombo.setCellFactory(c -> new TypeListCell());
        typeCombo.setButtonCell(new TypeListCell());
    }

    public void setEntity(Route e) { this.entity = e; } // Установка редактируемого маршрута

    public void setStops(List<Stop> s) { // Загрузка списка остановок
        this.stops = s;
        stopACombo.setItems(FXCollections.observableArrayList(s));
        stopBCombo.setItems(FXCollections.observableArrayList(s));
        tryFill();
    }

    private void tryFill() { // Заполнение полей если все данные загружены
        if (entity != null && stops != null && types != null) fillFields();
    }

    public void setTransportTypes(List<TransportType> t) { // Загрузка типов транспорта
        this.types = t;
        typeCombo.setItems(FXCollections.observableArrayList(t));
        tryFill();
        if (entity == null && !t.isEmpty()) typeCombo.getSelectionModel().selectFirst();
    }

    @Override
    @FXML
    protected void handleSave() { // Обработчик сохранения
        if (isValid()) {
            if (entity == null) entity = new Route();
            fillEntity();
            saveClicked = true;
            dialogStage.close();
        }
    }

    @Override
    protected boolean isValid() { // Валидация введенных данных
        StringBuilder err = new StringBuilder();
        if (numFld.getText() == null || numFld.getText().trim().isEmpty()) err.append("Введите номер!\n");
        if (stopACombo.getValue() == null) err.append("Выберите А!\n");
        if (stopBCombo.getValue() == null) err.append("Выберите Б!\n");
        if (stopACombo.getValue() != null && stopBCombo.getValue() != null &&
                stopACombo.getValue().equals(stopBCombo.getValue())) err.append("А и Б должны отличаться!\n");
        if (costFld.getText() == null || costFld.getText().trim().isEmpty()) err.append("Введите стоимость!\n");
        else {
            try {
                double c = Double.parseDouble(costFld.getText().trim());
                if (c < 0) err.append("Стоимость должна быть положительной!\n");
            } catch (NumberFormatException e) {
                err.append("Стоимость должна быть числом!\n");
            }
        }
        if (typeCombo.getValue() == null) err.append("Выберите тип!\n");
        if (numFld.getText() != null && !numFld.getText().trim().isEmpty() && typeCombo.getValue() != null && allRoutes != null) {
            String num = numFld.getText().trim();
            int tid = typeCombo.getValue().getTypeId();
            boolean exists = allRoutes.stream()
                    .anyMatch(r -> r.getNumber().equalsIgnoreCase(num) && r.getTypeId() == tid
                            && (entity == null || r.getRouteId() != entity.getRouteId()));
            if (exists) err.append("Маршрут с таким номером и типом уже существует!\n");
        }
        if (!err.isEmpty()) {
            showAlert(err.toString());
            return false;
        }
        return true;
    }

    @Override
    protected void fillEntity() { // Заполнение сущности данными из формы
        entity.setNumber(numFld.getText().trim());
        entity.setStopAId(stopACombo.getValue().getStopId());
        entity.setStopBId(stopBCombo.getValue().getStopId());
        entity.setCost(Double.parseDouble(costFld.getText().trim()));
        entity.setTypeId(typeCombo.getValue().getTypeId());
        entity.setActive(true);
    }

    @Override
    protected void fillFields() { // Заполнение формы данными сущности (при редактировании)
        if (entity != null) {
            numFld.setText(entity.getNumber());
            costFld.setText(String.valueOf(entity.getCost()));
            if (entity.getTypeId() > 0 && types != null) {
                TransportType sel = types.stream().filter(t -> t.getTypeId() == entity.getTypeId()).findFirst().orElse(null);
                if (sel != null) typeCombo.getSelectionModel().select(sel);
            }
            if (entity.getStopAId() > 0 && stops != null) {
                Stop sel = stops.stream().filter(s -> s.getStopId() == entity.getStopAId()).findFirst().orElse(null);
                if (sel != null) stopACombo.getSelectionModel().select(sel);
            }
            if (entity.getStopBId() > 0 && stops != null) {
                Stop sel = stops.stream().filter(s -> s.getStopId() == entity.getStopBId()).findFirst().orElse(null);
                if (sel != null) stopBCombo.getSelectionModel().select(sel);
            }
        }
    }

    private static class StopListCell extends ListCell<Stop> { // Кастомная ячейка для отображения остановки
        @Override protected void updateItem(Stop i, boolean e) {
            super.updateItem(i, e);
            setText(e || i == null ? null : i.getName());
        }
    }

    private static class TypeListCell extends ListCell<TransportType> { // Кастомная ячейка для отображения типа транспорта
        @Override protected void updateItem(TransportType i, boolean e) {
            super.updateItem(i, e);
            setText(e || i == null ? null : i.getTypeName());
        }
    }
}