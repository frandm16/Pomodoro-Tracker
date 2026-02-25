package com.frandm.pomodoro;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class PomodoroController {
    @FXML private Label timerLabel;
    @FXML private ComboBox<String> subjectSelector;
    @FXML private Button startButton, pauseButton, finishButton, resetButton;

    private PomodoroEngine pomodoroEngine;
    private final int DEFAULT_MINUTES = 45;

    @FXML
    public void initialize() {
        DatabaseHandler.initializeDatabase();
        subjectSelector.getItems().addAll("test1", "test2"); // debug
        subjectSelector.getSelectionModel().selectFirst(); // debug

        pomodoroEngine = new PomodoroEngine(DEFAULT_MINUTES);
        pomodoroEngine.setOnTick(() -> timerLabel.setText(pomodoroEngine.getFormattedTime()));
    }

    @FXML
    private void handleStart() {
        pomodoroEngine.start();
        toggleButtons(true);
        System.out.println("[DEBUG] pulsado start");
    }

    @FXML
    private void handlePause() {
        pomodoroEngine.pause();
        toggleButtons(false);
        System.out.println("[DEBUG] pulsado pause");
    }

    @FXML
    private void handleReset() {
        pomodoroEngine.reset();
        timerLabel.setText(pomodoroEngine.getFormattedTime());
        toggleButtons(false);
        System.out.println("[DEBUG] pulsado reset");
    }

    @FXML
    private void handleFinish() {
        pomodoroEngine.stop();
        int minutesDone = pomodoroEngine.getElapsedMinutes();
        String subject = subjectSelector.getValue();

        // only adds to the database if minutes studied >=1
        if (minutesDone >= 1) {
            DatabaseHandler.saveSession(subject, minutesDone);
        }

        handleReset(); // reset timer
        System.out.println("[DEBUG] terminado");
    }

    private void toggleButtons(boolean running) {
        startButton.setDisable(running);
        pauseButton.setDisable(!running);
    }

}