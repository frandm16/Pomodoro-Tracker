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

import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.util.Duration;
//endregion

//region JavaFX UI Controls & Application
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import javafx.scene.media.AudioClip;
import java.net.URL;
//endregion

public class PomodoroController {

    public GridPane heatmapGrid;
    public Pane monthLabelContainer;
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
    @FXML private Label workValLabel, shortValLabel, longValLabel, intervalValLabel, alarmVolumeValLabel;
    @FXML private Slider workSlider, shortSlider, longSlider, intervalSlider, alarmVolumeSlider;
    @FXML private ToggleButton autoBreakToggle, autoPomoToggle, countBreakTime;
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
        ConfigManager.load(engine);

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
        setupSlider(alarmVolumeSlider, alarmVolumeValLabel, engine.getAlarmSoundVolume(), engine::setAlarmSoundVolume);

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

        countBreakTime.setSelected(engine.isCountBreakTime());
        countBreakTime.setText(countBreakTime.isSelected() ? "ON" : "OFF");
        countBreakTime.selectedProperty().addListener((_, _, isSelected) -> {
            countBreakTime.setText(isSelected ? "ON" : "OFF");
            updateEngineFlags();
        });


        engine.setOnTick(() -> Platform.runLater(() -> {
            timerLabel.setText(engine.getFormattedTime());
            updateProgressCircle();
        }));
        engine.setOnStateChange(() -> Platform.runLater(this::updateUIFromEngine));
        engine.setOnTimerFinished(() -> Platform.runLater(this::playAlarmSound));

        drawHeatmap();
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
                autoPomoToggle.isSelected(),
                countBreakTime.isSelected(),
                (int)alarmVolumeSlider.getValue()
        );

    }

    private void showMainView() {
        switchPanels(statsContainer, mainContainer);
    }

    private void showStatsView() {
        if (statsContainer.isVisible()) return;

        ObservableList<Session> datos = DatabaseHandler.getAllSessions();
        sessionsTable.setItems(datos);

        drawHeatmap();

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
            updateEngineFlags();
            ConfigManager.save(engine);
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
        int minutes = engine.getRealMinutesElapsed();
        DatabaseHandler.saveSession("test", "tema1", "esto es una descripcion test", minutes);

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
        countBreakTime.setSelected(engine.isCountBreakTime());

        updateEngineFlags();
        updateUIFromEngine();
        ConfigManager.save(engine);
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
                alarm.setVolume((double) engine.getAlarmSoundVolume() /100); // Volumen de 0.0 a 1.0
                alarm.play();
            } else {
                System.err.println("No se encontró el archivo de sonido.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    //region HeatMap

    private void drawHeatmap() {
        heatmapGrid.getChildren().clear();
        monthLabelContainer.getChildren().clear();

        java.util.Map<java.time.LocalDate, Integer> data = DatabaseHandler.getMinutesPerDayLastYear();
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate startDate = today.minusWeeks(52).with(java.time.DayOfWeek.MONDAY);

        String lastMonthName = "";
        double cellWidth = 12 + 3;

        for (int week = 0; week <= 52; week++) {
            for (int day = 0; day < 7; day++) {
                java.time.LocalDate date = startDate.plusWeeks(week).plusDays(day);
                if (date.isAfter(today)) continue;

                // heatmap month name
                String currentMonthName = date.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()).toUpperCase();
                if (!currentMonthName.equals(lastMonthName)) {
                    Label monthLabel = new Label(currentMonthName);
                    monthLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 9px; -fx-font-weight: bold;");

                    monthLabel.setLayoutX(week * cellWidth + 10);
                    monthLabel.setLayoutY(5);

                    monthLabelContainer.getChildren().add(monthLabel);
                    lastMonthName = currentMonthName;
                }

                // heatmap slot
                javafx.scene.shape.Rectangle rect = new javafx.scene.shape.Rectangle(12, 12);
                rect.setArcWidth(3);
                rect.setArcHeight(3);
                rect.getStyleClass().add("heatmap-cell");
                rect.setFill(getHeatmapColor(data.getOrDefault(date, 0)));

                // tooltip
                String tooltipText = String.format("%s\n%.1f h", date.toString(), (float)data.getOrDefault(date, 0)/60);
                javafx.scene.control.Tooltip tt = new javafx.scene.control.Tooltip(tooltipText);
                tt.setShowDelay(javafx.util.Duration.millis(50));
                javafx.scene.control.Tooltip.install(rect, tt);

                heatmapGrid.add(rect, week, day);
            }
        }
    }


    private javafx.scene.paint.Color getHeatmapColor(int minutes) {
        if (minutes == 0)   return javafx.scene.paint.Color.web("#ebedf0"); // Sin actividad
        if (minutes < 30)  return javafx.scene.paint.Color.web("#9be9a8"); // Poca
        if (minutes < 90)  return javafx.scene.paint.Color.web("#40c463"); // Media
        if (minutes < 180) return javafx.scene.paint.Color.web("#30a14e"); // Alta
        return javafx.scene.paint.Color.web("#216e39"); // Muy alta
    }
    //endregion
}