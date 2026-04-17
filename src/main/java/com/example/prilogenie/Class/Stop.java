package com.example.prilogenie.Class;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class Stop { // Модель остановки общественного транспорта
    private int stopId; // Уникальный идентификатор остановки
    private String name; // Название остановки
    private String address; // Адрес расположения остановки
    public int getId() { return stopId; } // Геттер для совместимости с TableView
    public void setId(int id) { this.stopId = id; } // Сеттер для совместимости
}
