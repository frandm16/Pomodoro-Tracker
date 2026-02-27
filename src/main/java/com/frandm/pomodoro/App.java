package com.frandm.pomodoro;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class App extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("main_view.fxml"));


        Scene scene = new Scene(fxmlLoader.load());
        scene.getStylesheets().add(
                Objects.requireNonNull(App.class.getResource("/com/frandm/pomodoro/styles.css")).toExternalForm()
        );

        stage.setTitle("Pomodoro Tracker");
        stage.setMaximized(true);
        stage.setResizable(true);

        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}