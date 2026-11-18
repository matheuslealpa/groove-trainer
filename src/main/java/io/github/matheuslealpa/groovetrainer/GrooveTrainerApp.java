package io.github.matheuslealpa.groovetrainer;

import io.github.matheuslealpa.groovetrainer.audio.MetronomeEngine;
import io.github.matheuslealpa.groovetrainer.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

public class GrooveTrainerApp extends Application {

    private final MetronomeEngine engine = new MetronomeEngine();

    @Override
    public void start(Stage stage) {
        MainView view = new MainView(engine);
        Scene scene = new Scene(view.getRoot(), 620, 520);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("ui/style.css")).toExternalForm());
        view.installShortcuts(scene);

        stage.getIcons().add(new Image(Objects.requireNonNull(
                getClass().getResource("ui/icon.png")).toExternalForm()));
        stage.setTitle("Groove Trainer");
        stage.setMinWidth(560);
        stage.setMinHeight(480);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        engine.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
