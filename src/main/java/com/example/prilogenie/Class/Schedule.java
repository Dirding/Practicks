package com.example.prilogenie.Class;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.sql.Time;

@NoArgsConstructor @Getter @Setter
public class Schedule { // Модель расписания движения транспорта. Содержит время отправления для каждого рейса
    private int scheduleId; // Уникальный идентификатор расписания
    private int routeId; // ID маршрута
    private int vehicleId; // ID транспортного средства
    private int trip; // Номер рейса (1-5 для каждого направления)
    private String direction; // Направление движения (A->B или B->A)
    private Time time; // Время отправления
    private String vehicleNumber; // Номер транспортного средства
    private String routeNumber; // Номер маршрута

    // JavaFX свойства для привязки к таблице с временем по рейсам (A направление 1-5, B направление 1-5)
    private final ObjectProperty<Time> timeA1 = new SimpleObjectProperty<>();
    private final ObjectProperty<Time> timeA2 = new SimpleObjectProperty<>();
    private final ObjectProperty<Time> timeA3 = new SimpleObjectProperty<>();
    private final ObjectProperty<Time> timeA4 = new SimpleObjectProperty<>();
    private final ObjectProperty<Time> timeA5 = new SimpleObjectProperty<>();
    private final ObjectProperty<Time> timeB1 = new SimpleObjectProperty<>();
    private final ObjectProperty<Time> timeB2 = new SimpleObjectProperty<>();
    private final ObjectProperty<Time> timeB3 = new SimpleObjectProperty<>();
    private final ObjectProperty<Time> timeB4 = new SimpleObjectProperty<>();
    private final ObjectProperty<Time> timeB5 = new SimpleObjectProperty<>();

    // Property-методы для JavaFX биндинга
    public ObjectProperty<Time> timeA1Property() { return timeA1; }
    public ObjectProperty<Time> timeA2Property() { return timeA2; }
    public ObjectProperty<Time> timeA3Property() { return timeA3; }
    public ObjectProperty<Time> timeA4Property() { return timeA4; }
    public ObjectProperty<Time> timeA5Property() { return timeA5; }
    public ObjectProperty<Time> timeB1Property() { return timeB1; }
    public ObjectProperty<Time> timeB2Property() { return timeB2; }
    public ObjectProperty<Time> timeB3Property() { return timeB3; }
    public ObjectProperty<Time> timeB4Property() { return timeB4; }
    public ObjectProperty<Time> timeB5Property() { return timeB5; }

    // Геттеры и сеттеры для работы с Property полями
    public Time getTimeA1() { return timeA1.get(); }
    public void setTimeA1(Time t) { this.timeA1.set(t); }
    public Time getTimeA2() { return timeA2.get(); }
    public void setTimeA2(Time t) { this.timeA2.set(t); }
    public Time getTimeA3() { return timeA3.get(); }
    public void setTimeA3(Time t) { this.timeA3.set(t); }
    public Time getTimeA4() { return timeA4.get(); }
    public void setTimeA4(Time t) { this.timeA4.set(t); }
    public Time getTimeA5() { return timeA5.get(); }
    public void setTimeA5(Time t) { this.timeA5.set(t); }
    public Time getTimeB1() { return timeB1.get(); }
    public void setTimeB1(Time t) { this.timeB1.set(t); }
    public Time getTimeB2() { return timeB2.get(); }
    public void setTimeB2(Time t) { this.timeB2.set(t); }
    public Time getTimeB3() { return timeB3.get(); }
    public void setTimeB3(Time t) { this.timeB3.set(t); }
    public Time getTimeB4() { return timeB4.get(); }
    public void setTimeB4(Time t) { this.timeB4.set(t); }
    public Time getTimeB5() { return timeB5.get(); }
    public void setTimeB5(Time t) { this.timeB5.set(t); }
}