package com.frandm.pomodoro;

import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.application.Platform;

public class PomodoroController {

    @FXML private VBox settingsPane;
    @FXML private Label timerLabel, stateLabel;
    @FXML private Spinner<Integer> workSpinner, shortSpinner, longSpinner, intervalSpinner;
    @FXML private ToggleButton autoBreakToggle, autoPomoToggle;
    @FXML private Button startPauseBtn;

    private boolean isSettingsOpen = false;
    private PomodoroEngine engine = new PomodoroEngine();

    @FXML
    public void initialize() {
        DatabaseHandler.initializeDatabase();

        workSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 60, 25));
        shortSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, 5));
        longSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 45, 15));
        intervalSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 4));

        engine.setOnTick(() -> Platform.runLater(() -> timerLabel.setText(engine.getFormattedTime())));
        engine.setOnStateChange(() -> Platform.runLater(this::updateUIFromEngine));

        updateUIFromEngine();
    }

    @FXML
    private void toggleSettings() {
        TranslateTransition transition = new TranslateTransition(Duration.millis(350), settingsPane);
        if (isSettingsOpen) {
            transition.setToY(450);
            applySettings();
        } else {
            transition.setToY(0);
        }
        transition.play();
        isSettingsOpen = !isSettingsOpen;
    }

    private void applySettings() {
        engine.updateSettings(
                workSpinner.getValue(),
                shortSpinner.getValue(),
                longSpinner.getValue(),
                intervalSpinner.getValue(),
                autoBreakToggle.isSelected(),
                autoPomoToggle.isSelected()
        );
    }

    @FXML
    private void handleStartPause() {
        if (engine.getCurrentState() == PomodoroEngine.State.WAITING || engine.getCurrentState() == PomodoroEngine.State.MENU) {
            engine.start();
        } else {
            engine.pause();
        }
    }

    @FXML
    private void handleSkip() {
        engine.skip();
    }

    @FXML
    private void handleFinish() {
        int mins = engine.getRealMinutesElapsed();
        DatabaseHandler.saveSession("Estudio", "Sesión Terminada", "Guardado manual", mins);

        engine.clearElapsedSeconds();
        engine.stop();
        engine.resetTimeForState(PomodoroEngine.State.MENU);
        updateUIFromEngine();
    }

    private void updateUIFromEngine() {
        PomodoroEngine.State current = engine.getCurrentState();

        if (current == PomodoroEngine.State.WAITING || current == PomodoroEngine.State.MENU) {
            startPauseBtn.setText("START");
        } else {
            startPauseBtn.setText("PAUSE");
        }

        switch (current) {
            case WORK -> {
                int session = engine.getSessionCounter() + 1;
                stateLabel.setText(String.format("FOCUS TIME - %d", session));
                stateLabel.setStyle("-fx-text-fill: #e74c3c;");
            }
            case SHORT_BREAK -> {
                stateLabel.setText("BREAK TIME");
                stateLabel.setStyle("-fx-text-fill: #27ae60;");
            }
            case LONG_BREAK -> {
                stateLabel.setText("LONG BREAK TIME");
                stateLabel.setStyle("-fx-text-fill: #27ae60;");
            }
            case WAITING -> {
                stateLabel.setText("PAUSED");
                stateLabel.setStyle("-fx-text-fill: #f39c12;");
            }
            case MENU -> {
                stateLabel.setText("READY TO START");
                stateLabel.setStyle("-fx-text-fill: #7f8c8d;");
            }
        }
    }
}