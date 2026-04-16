package com.example.prilogenie.Manager.Vehicle;

import com.example.prilogenie.Class.Route;
import com.example.prilogenie.Class.TransportType;
import com.example.prilogenie.Class.TransportVehicle;
import com.example.prilogenie.Manager.BaseDialog;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import lombok.Setter;

import java.util.List;

public class Dialogvehicle extends BaseDialog<TransportVehicle> { // Контроллер диалога добавления/редактирования ТС
    @FXML private TextField numFld; // Элементы интерфейса, связанные с FXML разметкой
    @FXML private TextField capFld;
    @FXML private ComboBox<TransportType> typeCombo;
    @FXML private ComboBox<Route> routeCombo;
    private List<TransportType> types;
    private List<Route> allRoutes;
    @Setter private List<TransportVehicle> allVehicles; // Список всех ТС для проверки уникальности номера

    @FXML
    public void initialize() { // Инициализация компонентов
        setupTypeCell();
        setupRouteCell();
    }

    private void setupTypeCell() { // Настройка отображения типа транспорта в ComboBox
        typeCombo.setCellFactory(c -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(TransportType i, boolean e) {
                super.updateItem(i, e);
                setText(e || i == null ? null : i.getTypeName());
            }
        });
        typeCombo.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(TransportType i, boolean e) {
                super.updateItem(i, e);
                setText(e || i == null ? null : i.getTypeName());
            }
        });
    }

    private void setupRouteCell() { // Настройка отображения маршрута в ComboBox
        routeCombo.setCellFactory(c -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(Route i, boolean e) {
                super.updateItem(i, e);
                setText(e || i == null ? null : i.getNumber());
            }
        });
        routeCombo.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(Route i, boolean e) {
                super.updateItem(i, e);
                setText(e || i == null ? null : i.getNumber());
            }
        });
    }

    public void setTransportTypes(List<TransportType> transportTypes) { // Установка списка типов транспорта
        this.types = transportTypes;
        if (typeCombo != null) {
            loadTypes();
        }
    }

    public void setAllRoutes(List<Route> routes) { // Установка списка маршрутов
        this.allRoutes = routes;
        if (routeCombo != null) {
            loadRoutes();
        }
    }

    public void loadTypes() { // Загрузка типов в ComboBox
        typeCombo.getItems().clear();
        if (types == null || types.isEmpty()) {
            showAlert("Нет доступных типов");
            return;
        }
        typeCombo.getItems().addAll(types);
        if (!typeCombo.getItems().isEmpty()) {
            typeCombo.getSelectionModel().selectFirst();
        }
    }

    public void loadRoutes() { // Загрузка маршрутов в ComboBox
        routeCombo.getItems().clear();
        Route none = new Route();
        none.setRouteId(0);
        none.setNumber("Без маршрута");
        routeCombo.getItems().add(none);
        if (allRoutes != null && !allRoutes.isEmpty()) {
            for (Route r : allRoutes) {
                Route displayRoute = new Route();
                displayRoute.setRouteId(r.getRouteId());
                displayRoute.setNumber(getRouteText(r));
                routeCombo.getItems().add(displayRoute);
            }
        }
        if (!routeCombo.getItems().isEmpty()) {
            routeCombo.getSelectionModel().selectFirst();
        }
    }

    private String getRouteText(Route r) { // Форматирование отображения маршрута с типом
        if (types != null) {
            return types.stream()
                    .filter(t -> t.getTypeId() == r.getTypeId())
                    .map(TransportType::getTypeName)
                    .findFirst()
                    .map(n -> r.getNumber() + " (" + n + ")")
                    .orElse(r.getNumber());
        }
        return r.getNumber();
    }

    @Override
    @FXML
    protected void handleSave() { // Обработчик сохранения
        if (isValid()) {
            if (entity == null) {
                entity = new TransportVehicle();
            }
            fillEntity();
            saveClicked = true;
            dialogStage.close();
        }
    }

    @Override
    protected boolean isValid() { // Валидация введенных данных
        StringBuilder err = new StringBuilder();
        if (isEmpty(numFld)) {
            err.append("Введите гос. номер!\n");
        }
        if (isEmpty(capFld)) {
            err.append("Введите вместимость!\n");
        }
        if (typeCombo.getValue() == null) {
            err.append("Выберите тип!\n");
        }
        if (routeCombo.getValue() == null) {
            err.append("Выберите маршрут!\n");
        }
        if (!isEmpty(numFld) && allVehicles != null) {
            String inputNum = numFld.getText().trim();
            boolean exists = allVehicles.stream()
                    .anyMatch(v -> v.getVehicleNumber().equalsIgnoreCase(inputNum)
                            && (entity == null || v.getVehicleId() != entity.getVehicleId()));
            if (exists) {
                err.append("Номер уже существует!\n");
            }
        }
        if (!err.isEmpty()) {
            showAlert(err.toString());
            return false;
        }
        return true;
    }

    private boolean isEmpty(TextField f) { // Проверка пустого поля
        return f.getText() == null || f.getText().trim().isEmpty();
    }

    @Override
    protected void fillEntity() { // Заполнение сущности данными из формы
        entity.setVehicleNumber(numFld.getText().trim());
        entity.setCapacity(Integer.parseInt(capFld.getText().trim()));

        TransportType selectedType = typeCombo.getValue();
        if (selectedType != null) {
            entity.setTypeId(selectedType.getTypeId());
        }
        Route selectedRoute = routeCombo.getValue();
        if (selectedRoute != null && selectedRoute.getRouteId() != 0) {
            entity.setRouteId(selectedRoute.getRouteId());
        } else {
            entity.setRouteId(null);
        }
    }

    @Override
    protected void fillFields() { // Заполнение формы данными сущности (при редактировании)
        if (entity == null) {
            return;
        }
        numFld.setText(entity.getVehicleNumber());
        capFld.setText(String.valueOf(entity.getCapacity()));
        typeCombo.getItems().stream()
                .filter(t -> t.getTypeId() == entity.getTypeId())
                .findFirst()
                .ifPresent(t -> typeCombo.getSelectionModel().select(t));
        if (entity.getRouteId() == null) {
            if (!routeCombo.getItems().isEmpty()) {
                routeCombo.getSelectionModel().selectFirst();
            }
        } else {
            routeCombo.getItems().stream()
                    .filter(r -> r.getRouteId() == entity.getRouteId())
                    .findFirst()
                    .ifPresentOrElse(
                            r -> routeCombo.getSelectionModel().select(r),
                            () -> {
                                if (!routeCombo.getItems().isEmpty()) {
                                    routeCombo.getSelectionModel().selectFirst();
                                }
                            }
                    );
        }
    }

    public void setVehicle(TransportVehicle v) { // Установка ТС для редактирования
        setEntity(v);
        if (v != null) {
            fillFields();
        }
    }

    public TransportVehicle getVehicle() { // Получение ТС
        return getEntity();
    }
}