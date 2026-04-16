module com.example.prilogenie {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires javafx.graphics;
    requires static lombok;
    requires static org.jetbrains.annotations;

    opens com.example.prilogenie.Reg_Log to javafx.fxml;
    opens com.example.prilogenie.Admin to javafx.fxml;
    opens com.example.prilogenie.Manager to javafx.fxml;
    opens com.example.prilogenie.User to javafx.fxml;
    opens com.example.prilogenie.Manager.Route to javafx.fxml;
    opens com.example.prilogenie.Manager.Stop to javafx.fxml;
    opens com.example.prilogenie.Manager.Vehicle to javafx.fxml;
    opens com.example.prilogenie.Manager.Schedule to javafx.fxml;
    opens com.example.prilogenie.Manager.RouteStop to javafx.fxml;
    opens com.example.prilogenie.Class to javafx.base;
    opens com.example.prilogenie.Utils to javafx.base, javafx.fxml;

    exports com.example.prilogenie;
    exports com.example.prilogenie.Manager.Schedule;
    exports com.example.prilogenie.Manager.RouteStop;
}