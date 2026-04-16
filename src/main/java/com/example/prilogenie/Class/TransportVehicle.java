package com.example.prilogenie.Class;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class TransportVehicle { // Модель транспортного средства (автобус, троллейбус и т.д.)
    private int vehicleId; // Уникальный идентификатор ТС
    private String number; // Государственный/бортовой номер
    private int typeId; // ID типа транспорта (ссылка на TransportType)
    private Integer routeId; // ID назначенного маршрута (может быть null если не закреплен)
    private int capacity; // Вместимость транспортного средства (количество мест)

    public int getId() { return vehicleId; } // Геттер для совместимости с TableView
    public void setId(int id) { this.vehicleId = id; } // Сеттер для совместимости
    public String getVehicleNumber() { return number; } // Геттер для получения номера ТС
    public void setVehicleNumber(String n) { this.number = n; } // Сеттер для установки номера
}