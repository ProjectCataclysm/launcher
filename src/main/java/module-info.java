module ru.cataclysm.main {
    requires java.desktop;
    requires java.logging;
    requires java.management;
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlinx.coroutines.javafx;
    requires kotlinx.coroutines.core;
    requires kotlinx.serialization.json;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
    requires okhttp3;
    requires jlibtorrent;


    opens ru.cataclysm to javafx.fxml, javafx.graphics;
    exports ru.cataclysm to javafx.fxml, javafx.graphics;
}