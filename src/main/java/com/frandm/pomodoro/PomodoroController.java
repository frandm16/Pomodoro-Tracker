package com.frandm.pomodoro;

import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class PomodoroController {

    //region @FXML Components
    @FXML private StackPane rootPane;
    @FXML private VBox settingsPane;
    @FXML private Label timerLabel, stateLabel;
    @FXML private Label workValLabel, shortValLabel, longValLabel, intervalValLabel;
    @FXML private Slider workSlider, shortSlider, longSlider, intervalSlider;
    @FXML private ToggleButton autoBreakToggle, autoPomoToggle;
    @FXML private Button startPauseBtn, skipBtn, finishBtn, settingsBtn;
    //endregion

    //region Variables
    private boolean isSettingsOpen = false;
    private final PomodoroEngine engine = new PomodoroEngine();
    private TranslateTransition settingsAnim;
    //endregion

    //region Initializer
    @FXML
    public void initialize() {
        DatabaseHandler.initializeDatabase();

        settingsPane.setTranslateX(-600);

        setupSlider(workSlider, workValLabel, engine.getWorkMins(), engine::setWorkMins);
        setupSlider(shortSlider, shortValLabel, engine.getShortMins(), engine::setShortMins);
        setupSlider(longSlider, longValLabel, engine.getLongMins(), engine::setLongMins);
        setupSlider(intervalSlider, intervalValLabel, engine.getInterval(), engine::setInterval);

        autoBreakToggle.setSelected(engine.isAutoStartBreaks());
        autoBreakToggle.setText(autoBreakToggle.isSelected() ? "ON" : "OFF");
        autoBreakToggle.selectedProperty().addListener((obs, old, isSelected) -> {
            autoBreakToggle.setText(isSelected ? "ON" : "OFF");
            updateEngineFlags();
        });

        autoPomoToggle.setSelected(engine.isAutoStartPomo());
        autoPomoToggle.setText(autoPomoToggle.isSelected() ? "ON" : "OFF");
        autoPomoToggle.selectedProperty().addListener((obs, old, isSelected) -> {
            autoPomoToggle.setText(isSelected ? "ON" : "OFF");
            updateEngineFlags();
        });

        engine.setOnTick(() -> Platform.runLater(() -> timerLabel.setText(engine.getFormattedTime())));
        engine.setOnStateChange(() -> Platform.runLater(this::updateUIFromEngine));

        updateEngineFlags();
        updateUIFromEngine();
    }
    //endregion

    //region Setup Helpers
    private void setupSlider(Slider slider, Label label, int initialValue, java.util.function.Consumer<Integer> updateAction) {
        slider.setValue(initialValue);
        label.setText(String.valueOf(initialValue));

        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int val = newVal.intValue();
            label.setText(String.valueOf(val));
            updateAction.accept(val);

            if (engine.getCurrentState() == PomodoroEngine.State.MENU) {
                timerLabel.setText(engine.getFormattedTime());
            }
        });
    }

    private void updateEngineFlags() {
        engine.updateSettings(
                (int)workSlider.getValue(),
                (int)shortSlider.getValue(),
                (int)longSlider.getValue(),
                (int)intervalSlider.getValue(),
                autoBreakToggle.isSelected(),
                autoPomoToggle.isSelected()
        );
    }
    //endregion

    //region Button Handlers
    @FXML
    private void toggleSettings() {
        if (settingsAnim != null) settingsAnim.stop();

        settingsAnim = new TranslateTransition(Duration.millis(400), settingsPane);
        settingsAnim.setInterpolator(Interpolator.EASE_BOTH);

        if (isSettingsOpen) {
            settingsAnim.setToX(-600); // hide settings
        } else {
            settingsAnim.setToX(0);    // show settings
        }

        settingsAnim.play();
        isSettingsOpen = !isSettingsOpen;
    }

    @FXML
    private void handleStartPause() {
        PomodoroEngine.State current = engine.getCurrentState();
        if (current == PomodoroEngine.State.WAITING || current == PomodoroEngine.State.MENU) {
            engine.start();
        } else {
            engine.pause();
        }
        updateUIFromEngine();
    }

    @FXML
    private void handleSkip() {
        engine.skip();
    }

    @FXML
    private void handleFinish() {
        int mins = engine.getRealMinutesElapsed();
        DatabaseHandler.saveSession("test", "tema1", "esto es una descripcion test", mins);

        engine.fullReset();
        engine.stop();
        engine.resetTimeForState(PomodoroEngine.State.MENU);
        updateUIFromEngine();
    }
    //endregion

    //region UI Updates
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
                rootPane.setStyle("-fx-background-color: -color-work;");
                int session = engine.getSessionCounter() + 1;
                stateLabel.setText(String.format("Pomodoro - Session %d", session));
            }
            case SHORT_BREAK -> {
                rootPane.setStyle("-fx-background-color: -color-break;");
                stateLabel.setText("Short Break");
            }
            case LONG_BREAK -> {
                rootPane.setStyle("-fx-background-color: -color-long-break;");
                stateLabel.setText("Long Break");
            }
            case MENU -> {
                rootPane.setStyle("-fx-background-color: -color-menu;");
                stateLabel.setText("Pomodoro");
            }
            default -> {}
        }
    }
    //endregion
}