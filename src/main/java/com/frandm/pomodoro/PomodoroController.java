package com.frandm.pomodoro;

import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.ObservableList;
//region JavaFX Animation & Layout
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;

import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.util.Duration;
//endregion

//region JavaFX UI Controls & Application
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import javafx.scene.media.AudioClip;
import java.net.URL;
//endregion

public class PomodoroController {

    @FXML private Arc progressArc;
    @FXML private TableView<Session> sessionsTable;
    @FXML private TableColumn<Session, String> colDate;
    @FXML private TableColumn<Session, String> colSubject;
    @FXML private TableColumn<Session, String> colTopic;
    @FXML private TableColumn<Session, Integer> colDuration;

    public VBox mainContainer;
    public VBox statsContainer;
    //region @FXML Components
    @FXML private StackPane rootPane;
    @FXML private VBox settingsPane;
    @FXML private Label timerLabel, stateLabel;
    @FXML private Label workValLabel, shortValLabel, longValLabel, intervalValLabel;
    @FXML private Slider workSlider, shortSlider, longSlider, intervalSlider;
    @FXML private ToggleButton autoBreakToggle, autoPomoToggle;
    @FXML private Button startPauseBtn, skipBtn, finishBtn, menuBtn, statsBtn;
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

        // --- CONFIGURACIÓN DE LA TABLA ---
        // El nombre entre comillas DEBE coincidir con el nombre del atributo en la clase Session
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        colTopic.setCellValueFactory(new PropertyValueFactory<>("topic"));
        colDuration.setCellValueFactory(new PropertyValueFactory<>("duration"));
        // ---------------------------------

        settingsPane.setTranslateX(-600);

        setupSlider(workSlider, workValLabel, engine.getWorkMins(), engine::setWorkMins);
        setupSlider(shortSlider, shortValLabel, engine.getShortMins(), engine::setShortMins);
        setupSlider(longSlider, longValLabel, engine.getLongMins(), engine::setLongMins);
        setupSlider(intervalSlider, intervalValLabel, engine.getInterval(), engine::setInterval);

        autoBreakToggle.setSelected(engine.isAutoStartBreaks());
        autoBreakToggle.setText(autoBreakToggle.isSelected() ? "ON" : "OFF");
        autoBreakToggle.selectedProperty().addListener((_, _, isSelected) -> {
            autoBreakToggle.setText(isSelected ? "ON" : "OFF");
            updateEngineFlags();
        });

        autoPomoToggle.setSelected(engine.isAutoStartPomo());
        autoPomoToggle.setText(autoPomoToggle.isSelected() ? "ON" : "OFF");
        autoPomoToggle.selectedProperty().addListener((_, _, isSelected) -> {
            autoPomoToggle.setText(isSelected ? "ON" : "OFF");
            updateEngineFlags();
        });

        engine.setOnTick(() -> Platform.runLater(() -> {
            timerLabel.setText(engine.getFormattedTime());
            updateProgressCircle();
        }));
        engine.setOnStateChange(() -> Platform.runLater(this::updateUIFromEngine));
        engine.setOnTimerFinished(() -> Platform.runLater(() -> {
            playAlarmSound();
        }));

        updateEngineFlags();
        updateUIFromEngine();
    }
    //endregion

    //region Setup Helpers
    private void setupSlider(Slider slider, Label label, int initialValue, java.util.function.Consumer<Integer> updateAction) {
        slider.setValue(initialValue);
        label.setText(String.valueOf(initialValue));

        slider.valueProperty().addListener((_, _, newVal) -> {
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

    private void showMainView() {
        switchPanels(statsContainer, mainContainer);
    }

    private void showStatsView() {
        if (statsContainer.isVisible()) return;

        ObservableList<Session> datos = DatabaseHandler.getAllSessions();
        sessionsTable.setItems(datos);

        switchPanels(mainContainer, statsContainer);
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

    @FXML
    private void handleResetTimeSettings() {
        engine.resetToDefaults();

        workSlider.setValue(engine.getWorkMins());
        shortSlider.setValue(engine.getShortMins());
        longSlider.setValue(engine.getLongMins());
        intervalSlider.setValue(engine.getInterval());

        autoBreakToggle.setSelected(engine.isAutoStartBreaks());
        autoPomoToggle.setSelected(engine.isAutoStartPomo());

        updateUIFromEngine();
    }

    @FXML
    private void handleNavClick(ActionEvent event) {
        Button source = (Button) event.getSource();

        menuBtn.getStyleClass().remove("active");
        statsBtn.getStyleClass().remove("active");

        source.getStyleClass().add("active");

        if (source == menuBtn) {
            showMainView();
        } else {
            showStatsView();
        }
    }
    //endregion

    //region UI Updates
    private void updateUIFromEngine() {
        PomodoroEngine.State current = engine.getCurrentState();
        PomodoroEngine.State logical = engine.getLogicalState();

        boolean isMenu = (current == PomodoroEngine.State.MENU);

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
                animateBackgroundTransition("-color-work");
                int session = engine.getSessionCounter() + 1;
                stateLabel.setText(String.format("Pomodoro - #%d", session));
            }
            case SHORT_BREAK -> {
                animateBackgroundTransition("-color-break");
                stateLabel.setText("Short Break");
            }
            case LONG_BREAK -> {
                animateBackgroundTransition("-color-long-break");
                stateLabel.setText("Long Break");
            }
            case MENU -> {
                animateBackgroundTransition("-color-menu");
                stateLabel.setText("Pomodoro");
            }
            default -> {}
        }
    }
    //endregion

    //region Animations
    private void animateBackgroundTransition(String cssVar) {
        Background currentBg = rootPane.getBackground();
        Color start = (currentBg != null && !currentBg.getFills().isEmpty())
                ? (Color) currentBg.getFills().getFirst().getFill()
                : Color.web("#34495E");

        rootPane.setStyle("-fx-background-color: " + cssVar + ";");
        rootPane.applyCss();

        Timeline fade = getTimeline(start);
        fade.play();
    }

    private Timeline getTimeline(Color start) {
        Background nextBackground = rootPane.getBackground();

        Color target = (nextBackground != null && !nextBackground.getFills().isEmpty())
                ? (Color) nextBackground.getFills().getFirst().getFill()
                : start;

        var colorProp = new javafx.beans.property.SimpleObjectProperty<>(start);
        colorProp.addListener((_, _, n) -> rootPane.setBackground(new Background(new BackgroundFill(n, null, null))));


        return new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(colorProp, start)),
                new KeyFrame(Duration.millis(200), new KeyValue(colorProp, target))
        );
    }

    private void switchPanels(VBox toHide, VBox toShow) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), toHide);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(_ -> {
            toHide.setVisible(false);
            toHide.setManaged(false);

            toShow.setVisible(true);
            toShow.setManaged(true);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(150), toShow);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    private void updateProgressCircle() {

        double remaining = engine.getSecondsRemaining();
        double total = engine.getTotalSecondsForCurrentState();
        double elapsed = total - remaining;
        double ratio = (total > 0) ? (elapsed/total) : 0;
        double angle = ratio * -360;

        Platform.runLater(() -> {
            progressArc.setLength(angle);
        });
    }

    //endregion

    private void playAlarmSound() {
        try {
            // buscamos el archivo en src/main/resources/com/frandm/pomodoro/sounds/
            URL soundUrl = getClass().getResource("sounds/birds.mp3");

            if (soundUrl != null) {
                AudioClip alarm = new AudioClip(soundUrl.toExternalForm());
                alarm.setVolume(0.5); // Volumen de 0.0 a 1.0
                alarm.play();
            } else {
                System.err.println("No se encontró el archivo de sonido.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}