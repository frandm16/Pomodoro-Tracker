package com.frandm.pomodoro;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

public class PomodoroController {
    @FXML private Label timerLabel, stateLabel, sessionsLabel;
    @FXML private TextField workTimeField, shortTimeField, longTimeField;
    @FXML private Button startButton, pauseButton;

    private PomodoroEngine engine;

    @FXML
    public void initialize() {
        DatabaseHandler.initializeDatabase();

        engine = new PomodoroEngine(25, 5, 15, 4);

        engine.setOnTick(() -> timerLabel.setText(engine.getFormattedTime()));

        engine.setOnStateChange(() -> {
            updateUI();
        });
    }

    private void updateUI() {
        PomodoroEngine.State current = engine.getCurrentState();
        stateLabel.setText("STAGE: " + current.name());
        sessionsLabel.setText("Sessions completed: " + engine.getSessionCounter());

        switch (current) {
            case WORK -> stateLabel.setStyle("-fx-text-fill: #e74c3c;"); // Rojo
            case SHORT_BREAK, LONG_BREAK -> stateLabel.setStyle("-fx-text-fill: #27ae60;"); // Verde
            case WAITING -> stateLabel.setStyle("-fx-text-fill: #f39c12;"); // Naranja
        }
    }

    @FXML
    private void handleStart() {
        try {
            engine.updateConfig(
                    Integer.parseInt(workTimeField.getText()),
                    Integer.parseInt(shortTimeField.getText()),
                    Integer.parseInt(longTimeField.getText()),
                    4
            );

            engine.start();
            startButton.setDisable(true);
            pauseButton.setDisable(false);
        } catch (NumberFormatException e) {
            System.err.println("Invalid input in time fields");
        }
    }

    @FXML
    private void handlePause() {
        engine.pause();
        startButton.setDisable(false);
        pauseButton.setDisable(true);
    }

    @FXML
    private void handleFinish() {
        engine.stop();
        int minutes = engine.getElapsedMinutes();
        DatabaseHandler.saveSession("test", "tema1", "", minutes);

        handleReset();
        System.out.println("[DEBUG] saved: " + minutes + " min");
    }

    @FXML
    private void handleReset() {
        engine.stop();
        engine.resetToState(PomodoroEngine.State.WORK);
        timerLabel.setText(engine.getFormattedTime());
        stateLabel.setText("READY TO FOCUS");
        stateLabel.setStyle("-fx-text-fill: #34495e;");
        startButton.setDisable(false);
        pauseButton.setDisable(true);
    }
}