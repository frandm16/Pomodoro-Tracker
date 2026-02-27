package com.frandm.pomodoro;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;

public class StatsDashboard extends VBox {

    private GridPane heatmapGrid;
    private Pane monthLabelContainer;
    private final double CELL_SIZE = 25.0;
    private final double HGAP = 6.0;

    public StatsDashboard() {
        this.getStyleClass().add("stats-dashboard");
        this.setAlignment(Pos.TOP_CENTER);
        this.setMaxWidth(Double.MAX_VALUE);

        initializeHeatmapSection();
    }

    private void initializeHeatmapSection() {

        VBox heatmapContainer = new VBox();
        heatmapContainer.getStyleClass().add("heatmap-container");

        monthLabelContainer = new Pane();
        monthLabelContainer.getStyleClass().add("month-label-container");

        heatmapGrid = new GridPane();
        heatmapGrid.getStyleClass().add("heatmap-grid");
        heatmapGrid.setHgap(HGAP);
        heatmapGrid.setVgap(HGAP);

        heatmapContainer.getChildren().addAll(monthLabelContainer, heatmapGrid);

        HBox hbar = new HBox(5);
        hbar.setAlignment(Pos.CENTER_RIGHT);
        hbar.setPadding(new Insets(10, 0, 0, 0));

        Label less = new Label("Less");
        less.getStyleClass().add("legend-text");
        Label more = new Label("More");
        more.getStyleClass().add("legend-text");

        hbar.getChildren().add(less);

        String[] colorClasses = {"cell-empty", "cell-low", "cell-medium", "cell-high", "cell-extreme"};
        for (String colorClass : colorClasses) {
            Rectangle rect = new Rectangle(12, 12);
            rect.setArcWidth(4);
            rect.setArcHeight(4);
            rect.getStyleClass().add(colorClass);
            hbar.getChildren().add(rect);
        }
        hbar.getChildren().add(more);



        this.getChildren().addAll(heatmapContainer, hbar);
    }

    public void updateHeatmap(Map<LocalDate, Integer> data) {
        heatmapGrid.getChildren().clear();
        monthLabelContainer.getChildren().clear();

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusWeeks(52).with(java.time.DayOfWeek.MONDAY);

        int lastMonthValue = -1;
        int lastMonthColumn = -10;
        double cellFullWidth = CELL_SIZE + HGAP;
        for (int week = 0; week <= 52; week++) {
            for (int day = 0; day < 7; day++) {
                LocalDate date = startDate.plusWeeks(week).plusDays(day);
                if (date.isAfter(today)) continue;

                int currentMonthValue = date.getMonthValue();
                if (currentMonthValue != lastMonthValue && (week - lastMonthColumn) > 2) {
                    addMonthLabel(date, week, cellFullWidth);
                    lastMonthValue = currentMonthValue;
                    lastMonthColumn = week;
                }

                Rectangle rect = createHeatmapRect(data.getOrDefault(date, 0), date);
                heatmapGrid.add(rect, week, day);
            }
        }
    }

    private void addMonthLabel(LocalDate date, int week, double cellWidth) {
        String name = date.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault());

        if (!name.isEmpty()) {
            name = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
        }

        if (name.endsWith(".")) {
            name = name.substring(0, name.length() - 1);
        }

        Label label = new Label(name);
        label.getStyleClass().add("month-label");
        label.setLayoutX(week * cellWidth);

        monthLabelContainer.getChildren().add(label);
    }

    private Rectangle createHeatmapRect(int minutes, LocalDate date) {
        Rectangle rect = new Rectangle(CELL_SIZE, CELL_SIZE);
        rect.getStyleClass().add("heatmap-cell");

        if (minutes == 0) rect.getStyleClass().add("cell-empty");
        else if (minutes < 60)  rect.getStyleClass().add("cell-low");     // < 1h
        else if (minutes < 150) rect.getStyleClass().add("cell-medium");  // 1h - 2.5h
        else if (minutes < 250) rect.getStyleClass().add("cell-high");    // 2.5h - 4h
        else rect.getStyleClass().add("cell-extreme");

        String monthName = date.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault());
        if (!monthName.isEmpty()) {
            monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1).toLowerCase();
        }
        String weekDayName = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault());
        if(!weekDayName.isEmpty()) {
            weekDayName = weekDayName.substring(0,1).toUpperCase() + weekDayName.substring(1).toLowerCase();
        }

        String tooltipDate = weekDayName + ", " + monthName + " " + date.getDayOfMonth() + ", " + date.getYear();

        Tooltip tt = new Tooltip(String.format("%.1f", minutes/60.0) + "h \n" + tooltipDate);
        tt.getStyleClass().add("heatmap-tooltip");
        tt.setShowDelay(Duration.millis(75));
        Tooltip.install(rect, tt);

        return rect;
    }

}