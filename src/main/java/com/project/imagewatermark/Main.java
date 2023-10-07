package com.project.imagewatermark;

import com.project.imagewatermark.controller.DragHandler;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(Main.class.getResource("design.fxml"));
        stage.setTitle("Photo Edit");
        stage.setScene(new Scene(root));
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        cleanUpTemp();
        super.stop();

    }

    public void cleanUpTemp() throws IOException {
        String tmpDir = System.getProperty("java.io.tmpdir") + "photo-edit/unzip/";
        File tmpFolder = new File(tmpDir);
        if (tmpFolder.exists()) {

            Stream<Path> walk = Files.walk(tmpFolder.toPath());
            walk.map(java.nio.file.Path::toFile)
                    .sorted((o1, o2) -> -o1.compareTo(o2))
                    .forEach(File::delete);

            walk.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}