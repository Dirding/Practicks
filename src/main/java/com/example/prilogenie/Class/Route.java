package com.example.prilogenie.Class;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class Route { // Модель маршрута общественного транспорта
    private int routeId; // Уникальный идентификатор маршрута
    private String number; // Номер маршрута
    private int typeId; // ID типа транспорта (ссылка на TransportType)
    private String stopAName; // Название начальной остановки
    private String stopBName; // Название конечной остановки
    private int stopAId; // ID начальной остановки
    private int stopBId; // ID конечной остановки
    private double cost; // Стоимость проезда
    private boolean active; // Статус активности маршрута
}

