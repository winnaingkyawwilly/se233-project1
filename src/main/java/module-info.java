module com.project.imagewatermark {
    requires javafx.controls;
    requires javafx.fxml;


    requires com.almasb.fxgl.all;
    requires java.desktop;
    requires javafx.swing;
    requires org.apache.commons.compress;

    opens com.project.imagewatermark to javafx.fxml;
    opens com.project.imagewatermark.controller to javafx.fxml;

    exports com.project.imagewatermark;
    exports com.project.imagewatermark.controller;
}