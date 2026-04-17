package com.example.prilogenie.Class;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class TransportType { // Модель типа транспорта (автобус, троллейбус, трамвай, поезд)
    private int typeId; // Уникальный идентификатор типа
    private String name; // Название типа (например, "Автобус", "Троллейбус")
    private String typeCode; // Кодовое обозначение (BUS, TROLLEYBUS, TRAM, TRAIN)

    public String getTypeName() { // Геттер для получения названия типа
        return name;
    }

    public static String toCode(String name) { // Преобразование названия в код типа
        if (name == null) return "BUS";
        return switch (name.toUpperCase()) {
            case "TROLLEYBUS", "ТРОЛЛЕЙБУС" -> "TROLLEYBUS";
            case "TRAM", "ТРАМВАЙ" -> "TRAM";
            case "TRAIN", "ПОЕЗД" -> "TRAIN";
            default -> "BUS";
        };
    }


    public static String toRussian(String code) { // Преобразование кода в русское название
        if (code == null) return "Неизвестно";
        return switch (code.toUpperCase()) {
            case "BUS" -> "Автобус";
            case "TROLLEYBUS" -> "Троллейбус";
            case "TRAM" -> "Трамвай";
            case "TRAIN" -> "Поезд";
            default -> code;
        };
    }
}