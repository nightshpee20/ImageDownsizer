module uni.fmi.imagedownsizer {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens uni.fmi.imagedownsizer to javafx.fxml;
    exports uni.fmi.imagedownsizer;
}