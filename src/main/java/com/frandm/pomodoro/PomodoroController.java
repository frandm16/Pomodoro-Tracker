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
    @FXML private Button startPauseBtn, skipBtn, finishBtn, settingsBtn;

    private boolean isSettingsOpen = false;
    private PomodoroEngine engine = new PomodoroEngine();

    @FXML
    public void initialize() {
        DatabaseHandler.initializeDatabase();

        workSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 60, engine.getWorkMins()));
        shortSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, engine.getShortMins()));
        longSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 45, engine.getLongMins()));
        intervalSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, engine.getInterval()));
        autoBreakToggle.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            autoBreakToggle.setText(isSelected ? "ON" : "OFF");
        });
        autoPomoToggle.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            autoPomoToggle.setText(isSelected ? "ON" : "OFF");
        });

        applySettings();

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

        engine.fullReset();
        engine.stop();
        engine.resetTimeForState(PomodoroEngine.State.MENU);
        updateUIFromEngine();
    }

    private void updateUIFromEngine() {
        PomodoroEngine.State current = engine.getCurrentState();
        PomodoroEngine.State logical = engine.getLogicalState();

        boolean isMenu = (current == PomodoroEngine.State.MENU);
        settingsBtn.setVisible(isMenu);
        settingsBtn.setManaged(isMenu);

        boolean isWaitingOrMenu = (current == PomodoroEngine.State.WAITING || isMenu);
        startPauseBtn.setText(isWaitingOrMenu ? "START" : "PAUSE");

        boolean isRunning = (current != PomodoroEngine.State.WAITING && !isMenu);
        skipBtn.setVisible(isRunning);
        skipBtn.setManaged(isRunning);

        boolean hasStarted = (!isMenu);
        finishBtn.setVisible(hasStarted);
        finishBtn.setManaged(hasStarted);

        switch (logical) {
            case WORK -> {
                int session = engine.getSessionCounter() + 1;
                stateLabel.setText(String.format("POMODORO - %d", session));
                stateLabel.setStyle("-fx-text-fill: #ffffff;");
            }
            case SHORT_BREAK -> {
                stateLabel.setText("SHORT BREAK");
                stateLabel.setStyle("-fx-text-fill: #ffffff;");
            }
            case LONG_BREAK -> {
                stateLabel.setText("LONG BREAK");
                stateLabel.setStyle("-fx-text-fill: #ffffff;");
            }
            case MENU -> {
                stateLabel.setText("");
                stateLabel.setStyle("-fx-text-fill: #ffffff;");
            }
        }
    }
}