package com.frandm.pomodoro;

//region Imports
import javafx.scene.chart.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.ObservableList;
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
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.media.AudioClip;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
//endregion

public class PomodoroController {
    public Slider widthSlider;
    public Label widthSliderValLabel;
    public ColumnConstraints colRightStats, colCenterStats, colLeftStats;
    public ComboBox subjectComboBox;
    @FXML private AreaChart<String, Number> weeklyLineChart;
    @FXML private CategoryAxis weeksXAxis;
    public PieChart subjectsPieChart;
    public Button historyBtn;
    public VBox historyContainer;
    @FXML private Label streakLabel;
    @FXML private Label timeThisWeekLabel;
    @FXML private Label timeLastMonthLabel;
    @FXML private Label tasksLabel;
    @FXML private Label bestDayLabel;
    //region @FXML Components
    @FXML private VBox statsPlaceholder;
    @FXML private VBox statsContainer;
    @FXML private Arc progressArc;
    @FXML private TableView<Session> sessionsTable;
    @FXML private TableColumn<Session, String> colDate, colSubject, colTopic;
    @FXML private TableColumn<Session, Integer> colDuration;
    @FXML private StackPane rootPane;
    @FXML private VBox settingsPane, mainContainer;
    @FXML private Label timerLabel, stateLabel;
    @FXML private Label workValLabel, shortValLabel, longValLabel, intervalValLabel, alarmVolumeValLabel;
    @FXML private Slider workSlider, shortSlider, longSlider, intervalSlider, alarmVolumeSlider;
    @FXML private ToggleButton autoBreakToggle, autoPomoToggle, countBreakTime;
    @FXML private Button startPauseBtn, skipBtn, finishBtn, menuBtn, statsBtn;
    //endregion

    //region Variables
    private boolean isSettingsOpen = false;
    private final PomodoroEngine engine = new PomodoroEngine();
    private StatsDashboard statsDashboard;
    private TranslateTransition settingsAnim;
    //endregion

    //region Initializer
    @FXML
    public void initialize() {
        DatabaseHandler.initializeDatabase();
        //DatabaseHandler.generateRandomPomodoros();
        ConfigManager.load(engine);

        // config de la tabla
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        colTopic.setCellValueFactory(new PropertyValueFactory<>("topic"));
        colDuration.setCellValueFactory(new PropertyValueFactory<>("duration"));
        // ---------------------------------

        // dashboard
        statsDashboard = new StatsDashboard();
        statsPlaceholder.getChildren().add(statsDashboard);


        settingsPane.setTranslateX(-600);

        setupSlider(workSlider, workValLabel, engine.getWorkMins(), engine::setWorkMins, "");
        setupSlider(shortSlider, shortValLabel, engine.getShortMins(), engine::setShortMins, "");
        setupSlider(longSlider, longValLabel, engine.getLongMins(), engine::setLongMins, "");
        setupSlider(intervalSlider, intervalValLabel, engine.getInterval(), engine::setInterval, "");
        setupSlider(alarmVolumeSlider, alarmVolumeValLabel, engine.getAlarmSoundVolume(), engine::setAlarmSoundVolume, "%");
        setupSlider(widthSlider,widthSliderValLabel,engine.getWidthStats(), engine::setWidthStats, "%");


        colCenterStats.percentWidthProperty().bind(widthSlider.valueProperty());
        colLeftStats.percentWidthProperty().bind(widthSlider.valueProperty().multiply(-1).add(100).divide(2));
        colRightStats.percentWidthProperty().bind(widthSlider.valueProperty().multiply(-1).add(100).divide(2));

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


        updateEngineFlags();
        updateUIFromEngine();
    }
    //endregion

    //region Setup Helpers
    private void setupSlider(Slider slider, Label label, int initialValue, java.util.function.Consumer<Integer> updateAction, String text) {
        slider.setValue(initialValue);
        label.setText(String.valueOf(initialValue) + text);

        slider.valueProperty().addListener((_, _, newVal) -> {
            int val = newVal.intValue();
            label.setText(String.valueOf(val) + text);
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
                (int)alarmVolumeSlider.getValue(),
                (int)widthSlider.getValue()
        );

    }
    private void showMainView() {
        Region currentVisible = statsContainer.isVisible() ? statsContainer : historyContainer;
        if (mainContainer.isVisible()) return;
        switchPanels(currentVisible, mainContainer);
    }
    private void showStatsView() {
        if (statsContainer.isVisible()) return;

        ObservableList<Session> data = DatabaseHandler.getAllSessions();
        statsDashboard.updateHeatmap(DatabaseHandler.getMinutesPerDayLastYear());
        updateStatsCards(data);

        Region currentVisible = mainContainer.isVisible() ? mainContainer : historyContainer;
        switchPanels(currentVisible, statsContainer);
    }
    private void showHistoryView() {
        if (historyContainer.isVisible()) return;

        ObservableList<Session> data = DatabaseHandler.getAllSessions();
        sessionsTable.setItems(data);

        Region currentVisible = mainContainer.isVisible() ? mainContainer : statsContainer;
        switchPanels(currentVisible, historyContainer);
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

        DatabaseHandler.saveSession("test", "topic1", "description test", minutes);

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
        Button clickedBtn = (Button) event.getSource();

        menuBtn.getStyleClass().remove("active");
        statsBtn.getStyleClass().remove("active");
        historyBtn.getStyleClass().remove("active");
        clickedBtn.getStyleClass().add("active");

        if (clickedBtn == menuBtn) {
            showMainView();
        } else if (clickedBtn == statsBtn) {
            showStatsView();
        } else if (clickedBtn == historyBtn) {
            showHistoryView();
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
    private void switchPanels(Region toHide, Region toShow) {
        toShow.setOpacity(0.0);
        toHide.setOpacity(1.0);
        toShow.setVisible(true);
        toShow.setManaged(true);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), toHide);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        fadeOut.setOnFinished(_ -> {
            toHide.setVisible(false);
            toHide.setManaged(false);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toShow);
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

        Platform.runLater(() -> progressArc.setLength(angle));
    }

    //endregion

    //region Stats Logic
    private static final java.time.format.DateTimeFormatter DATE_FORMATTER =
            new java.time.format.DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd")
                    .optionalStart()
                    .appendPattern(" HH:mm:ss")
                    .optionalEnd()
                    .toFormatter();

    private void updateStatsCards(ObservableList<Session> sessions) {
        if (sessions == null) return;
        java.time.LocalDate today = java.time.LocalDate.now();

        updateTimeThisWeek(sessions, today);
        updateTimeLastMonth(sessions, today);
        calculateStreak(sessions);
        updateBestDay(sessions);
        tasksLabel.setText(String.valueOf(sessions.size()));
        updateSubjectsChart(sessions);
        updateWeeklyChart(sessions);
    }

    private void updateTimeLastMonth(ObservableList<Session> sessions, LocalDate today) {
        LocalDate firstDayLastMonth = today.minusMonths(1).withDayOfMonth(1);
        LocalDate lastDayLastMonth = today.minusMonths(1).with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());

        double minsLastMonth = sessions.stream()
                .filter(s -> {
                    LocalDate d = LocalDate.parse(s.getDate(), DATE_FORMATTER);
                    return !d.isBefore(firstDayLastMonth) && !d.isAfter(lastDayLastMonth);
                })
                .mapToDouble(Session::getDuration)
                .sum();
        timeLastMonthLabel.setText(String.format("%.1fh", minsLastMonth / 60));
    }

    private void updateTimeThisWeek(ObservableList<Session> sessions, LocalDate today) {
        java.time.LocalDate startOfWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        double minsThisWeek = sessions.stream()
                .filter(s -> {
                    LocalDate d = LocalDate.parse(s.getDate(), DATE_FORMATTER);
                    return !d.isBefore(startOfWeek);
                })
                .mapToDouble(Session::getDuration)
                .sum();
        timeThisWeekLabel.setText(String.format("%.1fh", minsThisWeek / 60));
    }

    private void updateBestDay(ObservableList<Session> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            bestDayLabel.setText("-");
            return;
        }

        String bestDay = sessions.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        s -> java.time.LocalDate.parse(s.getDate().substring(0, 10), DATE_FORMATTER).getDayOfWeek(),
                        java.util.stream.Collectors.summingInt(Session::getDuration)
                ))
                .entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(entry -> {
                    String dayName = entry.getKey().getDisplayName(
                            java.time.format.TextStyle.FULL,
                             java.util.Locale.getDefault()
                    );
                    return dayName.substring(0, 1).toUpperCase() + dayName.substring(1);
                })
                .orElse("-");
        bestDayLabel.setText(bestDay);
    }

    private void calculateStreak(ObservableList<Session> sessions) {
        java.util.Set<java.time.LocalDate> dates = sessions.stream()
                .map(s -> java.time.LocalDate.parse(s.getDate(), DATE_FORMATTER))
                .collect(java.util.stream.Collectors.toSet());

        int streak = 0;
        java.time.LocalDate check = java.time.LocalDate.now();
        if (!dates.contains(check)) check = check.minusDays(1);

        while (dates.contains(check)) {
            streak++;
            check = check.minusDays(1);
        }
        streakLabel.setText(streak + " Days");
    }

    private void updateSubjectsChart(ObservableList<Session> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            subjectsPieChart.getData().clear();
            return;
        }

        java.util.Map<String, Integer> timeBySubject = sessions.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        Session::getSubject,
                        java.util.stream.Collectors.summingInt(Session::getDuration)
                ));

        javafx.collections.ObservableList<PieChart.Data> pieData = javafx.collections.FXCollections.observableArrayList();

        timeBySubject.forEach((subject, totalMinutes) -> {
            float hours = (float) totalMinutes / 60;

            String label = String.format("%s (%.1fh)", subject, hours);

            PieChart.Data data = new PieChart.Data(label, hours);
            pieData.add(data);
        });

        subjectsPieChart.setData(pieData);

        for (PieChart.Data data : subjectsPieChart.getData()) {
            double sliceValue = data.getPieValue();
            double totalValue = pieData.stream().mapToDouble(PieChart.Data::getPieValue).sum();
            double percent = (sliceValue / totalValue) * 100;

            Tooltip tt = new Tooltip(String.format("%.1f%%\n%s", percent, data.getName()));
            tt.getStyleClass().add("heatmap-tooltip");
            tt.setShowDelay(Duration.millis(75));

            Tooltip.install(data.getNode(), tt);

            data.getNode().setOnMouseEntered(_ -> data.getNode().setStyle("-fx-opacity: 0.75; -fx-cursor: hand;"));
            data.getNode().setOnMouseExited(_ -> data.getNode().setStyle("-fx-opacity: 1.0;"));
        }
    }

    private void updateWeeklyChart(ObservableList<Session> sessions) {
        weeklyLineChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        java.time.format.DateTimeFormatter dateFormatter =
                java.time.format.DateTimeFormatter.ofPattern("dd MMM", java.util.Locale.getDefault());

        java.time.format.DateTimeFormatter labelFormatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM");

        for (int i = 11; i >= 0; i--) {
            LocalDate endOfWeek = LocalDate.now().minusWeeks(i).with(java.time.DayOfWeek.SUNDAY);
            LocalDate startOfWeek = endOfWeek.minusDays(6);

            double totalHours = sessions.stream()
                    .filter(s -> {
                        LocalDate d = LocalDate.parse(s.getDate().substring(0, 10), DATE_FORMATTER);
                        return !d.isBefore(startOfWeek) && !d.isAfter(endOfWeek);
                    })
                    .mapToDouble(Session::getDuration)
                    .sum() / 60;

            String label = startOfWeek.format(labelFormatter);
            XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(label, totalHours);

            dataPoint.setExtraValue(new LocalDate[]{startOfWeek, endOfWeek});
            series.getData().add(dataPoint);
        }

        weeklyLineChart.getData().add(series);

        for (XYChart.Data<String, Number> data : series.getData()) {
            LocalDate[] dates = (LocalDate[]) data.getExtraValue();
            LocalDate start = dates[0];
            LocalDate end = dates[1];

            Tooltip tooltip = new Tooltip(String.format("%s - %s\n%.1fh", start.format(dateFormatter), end.format(dateFormatter), data.getYValue().doubleValue()));

            tooltip.setShowDelay(Duration.millis(50));
            tooltip.getStyleClass().add("heatmap-tooltip");

            Tooltip.install(data.getNode(), tooltip);

            data.getNode().setOnMouseEntered(e -> {
                data.getNode().setScaleX(1.5);
                data.getNode().setScaleY(1.5);
                data.getNode().setCursor(javafx.scene.Cursor.HAND);
            });

            data.getNode().setOnMouseExited(e -> {
                data.getNode().setScaleX(1.0);
                data.getNode().setScaleY(1.0);
            });
        }
    }
//endregion

    private void playAlarmSound() {
        try {
            URL soundUrl = getClass().getResource("sounds/birds.mp3");

            if (soundUrl != null) {
                AudioClip alarm = new AudioClip(soundUrl.toExternalForm());
                alarm.setVolume((double) engine.getAlarmSoundVolume() /100); // 0.0 a 1.0
                alarm.play();
            } else {
                System.err.println("No se encontró el archivo de sonido.");
            }
        } catch (Exception e) {
            System.err.println("Error :" + e.getMessage());
        }
    }
}