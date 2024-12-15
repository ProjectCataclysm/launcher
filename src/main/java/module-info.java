module ru.cataclysm.cataclysm {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;


    opens ru.cataclysm to javafx.fxml;
    exports ru.cataclysm;
}