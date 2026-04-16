package com.example.prilogenie.Class;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class RouteStop { // Модель связи маршрута с остановками (промежуточные точки)
    private int routeStopId; // Уникальный идентификатор записи
    private int routeId; // ID маршрута
    private int stopId; // ID остановки
    private String direction; // Направление движения (A->B или B->A)
    private int stopOrder; // Порядковый номер остановки в маршруте
    private String stopName; // Название остановки
    private String routeNumber; // Номер маршрута
    private String address; // Адрес остановки
    public int getId() { return routeStopId; } // Геттер для совместимости с TableView
    public void setId(int id) { this.routeStopId = id; } // Сеттер для совместимости
}