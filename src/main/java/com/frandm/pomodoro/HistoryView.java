package com.frandm.pomodoro;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class HistoryView extends StackPane {

    private final Map<String, String> tagColors;
    private final VBox tagsGridRoot;
    private final VBox detailRoot;
    private final Label detailTitle;
    private final VBox sessionsContainer;
    private final VBox tasksSummaryContainer;
    private final Button loadMoreBtn;
    private final ScrollPane detailScrollPane;

    private String currentTag = null;
    private int currentOffset = 0;
    private final int PAGE_SIZE = 50;
    private LocalDate lastDate = null;
    private VBox lastSessionsContainer = null;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public HistoryView(Map<String, String> tagColors) {
        this.tagColors = tagColors;

        tagsGridRoot = new VBox(25);
        tagsGridRoot.setPadding(new Insets(30));
        tagsGridRoot.setAlignment(Pos.TOP_LEFT);

        detailRoot = new VBox(20);
        detailRoot.setPadding(new Insets(20));
        detailRoot.setVisible(false);

        Button backBtn = new Button("← Back");
        backBtn.getStyleClass().add("button-secondary");
        backBtn.setOnAction(e -> showTagsGrid());

        detailTitle = new Label();
        detailTitle.getStyleClass().add("big-card-title");

        HBox viewSelector = new HBox(10);
        Button btnTimeline = new Button("History");
        Button btnTasks = new Button("Tasks");
        btnTimeline.getStyleClass().addAll("title-button", "active");
        btnTasks.getStyleClass().add("title-button");

        sessionsContainer = new VBox(15);
        tasksSummaryContainer = new VBox(10);
        tasksSummaryContainer.setVisible(false);
        tasksSummaryContainer.setManaged(false);

        btnTimeline.setOnAction(e -> toggleView(true, btnTimeline, btnTasks));
        btnTasks.setOnAction(e -> toggleView(false, btnTasks, btnTimeline));

        viewSelector.getChildren().addAll(btnTimeline, btnTasks);

        loadMoreBtn = new Button("Load more");
        loadMoreBtn.getStyleClass().add("button-secondary");
        loadMoreBtn.setOnAction(e -> loadMore());

        VBox detailContent = new VBox(20, detailTitle, viewSelector, sessionsContainer, tasksSummaryContainer, loadMoreBtn);
        detailScrollPane = new ScrollPane(detailContent);
        detailScrollPane.setFitToWidth(true);
        detailScrollPane.getStyleClass().add("setup-scroll");

        detailRoot.getChildren().addAll(backBtn, detailScrollPane);

        this.getChildren().addAll(tagsGridRoot, detailRoot);
        refreshTagsGrid();
    }

    public void refreshTagsGrid() {
        this.currentTag = null;
        this.currentOffset = 0;
        this.lastDate = null;
        this.lastSessionsContainer = null;

        sessionsContainer.getChildren().clear();
        tasksSummaryContainer.getChildren().clear();

        tagsGridRoot.setVisible(true);
        detailRoot.setVisible(false);

        tagsGridRoot.getChildren().clear();
        Label title = new Label("Focus Area");
        title.getStyleClass().add("big-card-title");

        FlowPane grid = new FlowPane(20, 20);
        Map<String, String> updatedTags = DatabaseHandler.getTagColors();
        updatedTags.forEach((name, color) -> grid.getChildren().add(createTagCard(name, color)));

        tagsGridRoot.getChildren().addAll(title, grid);
    }

    private VBox createTagCard(String name, String color) {
        VBox card = new VBox(15);
        card.getStyleClass().add("tag-explorer-card");
        card.setPrefWidth(200);

        Region dot = new Region();
        dot.setPrefSize(14, 14);
        dot.setMaxSize(14, 14);
        dot.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 50%;");

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("history-card-title");

        card.getChildren().addAll(dot, nameLabel);
        card.setOnMouseClicked(e -> openTagDetail(name));
        return card;
    }

    private void openTagDetail(String tagName) {
        this.currentTag = tagName;
        this.currentOffset = 0;
        this.lastDate = null;
        this.lastSessionsContainer = null;

        detailTitle.setText(tagName);
        sessionsContainer.getChildren().clear();
        tasksSummaryContainer.getChildren().clear();

        tagsGridRoot.setVisible(false);
        detailRoot.setVisible(true);

        loadMore();
        loadTasksSummary(tagName);
    }

    private void loadTasksSummary(String tagName) {
        Map<String, Integer> summary = DatabaseHandler.getTaskSummaryByTag(tagName);
        summary.forEach((task, minutes) -> tasksSummaryContainer.getChildren().add(createTaskSummaryRow(task, minutes)));
    }

    private HBox createTaskSummaryRow(String task, int minutes) {
        HBox row = new HBox(15);
        row.getStyleClass().add("task-summary-row");
        row.setAlignment(Pos.CENTER_LEFT);

        Label name = new Label(task == null || task.isEmpty() ? "General" : task);
        name.getStyleClass().add("history-card-title");
        HBox.setHgrow(name, Priority.ALWAYS);

        Label time = new Label(minutes + " min");
        time.setStyle("-fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold; -fx-text-fill: -color-accent;");

        row.getChildren().addAll(name, time);
        return row;
    }

    private void toggleView(boolean showTimeline, Button active, Button inactive) {
        active.getStyleClass().add("active");
        inactive.getStyleClass().remove("active");

        sessionsContainer.setVisible(showTimeline);
        sessionsContainer.setManaged(showTimeline);
        loadMoreBtn.setVisible(showTimeline && !sessionsContainer.getChildren().isEmpty());

        tasksSummaryContainer.setVisible(!showTimeline);
        tasksSummaryContainer.setManaged(!showTimeline);
    }

    private void showTagsGrid() {
        detailRoot.setVisible(false);
        tagsGridRoot.setVisible(true);
        refreshTagsGrid();
    }

    private void loadMore() {
        List<Session> sessions = DatabaseHandler.getSessionsByTagPaged(currentTag, PAGE_SIZE, currentOffset);

        if (sessions.isEmpty()) {
            loadMoreBtn.setVisible(false);
            return;
        }

        for (Session s : sessions) {
            LocalDate sessionDate = LocalDateTime.parse(s.getStartDate(), DATE_FORMATTER).toLocalDate();
            if (lastDate == null || !sessionDate.equals(lastDate)) {
                createNewDayBlock(sessionDate);
                lastDate = sessionDate;
            }
            lastSessionsContainer.getChildren().add(createTimelineCard(s));
        }

        currentOffset += PAGE_SIZE;
        loadMoreBtn.setVisible(sessions.size() == PAGE_SIZE);
    }

    private void createNewDayBlock(LocalDate date) {
        HBox dayRow = new HBox(15);
        dayRow.setAlignment(Pos.TOP_LEFT);

        VBox dateBox = new VBox(-2);
        dateBox.setAlignment(Pos.TOP_CENTER);
        dateBox.setMinWidth(70);
        dateBox.setPadding(new Insets(10, 0, 0, 0));

        Label dayNum = new Label(String.valueOf(date.getDayOfMonth()));
        dayNum.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: -color-accent;");

        String month = date.format(DateTimeFormatter.ofPattern("MMM", new Locale("es"))).toUpperCase();
        Label monthName = new Label(month.replace(".", ""));
        monthName.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-fg-muted; -fx-font-weight: bold;");

        dateBox.getChildren().addAll(dayNum, monthName);

        lastSessionsContainer = new VBox(15);
        HBox.setHgrow(lastSessionsContainer, Priority.ALWAYS);
        lastSessionsContainer.setStyle("-fx-border-color: -color-border-subtle; -fx-border-width: 0 0 0 2; -fx-padding: 0 0 30 20;");

        dayRow.getChildren().addAll(dateBox, lastSessionsContainer);
        sessionsContainer.getChildren().add(dayRow);
    }

    private VBox createTimelineCard(Session s) {
        VBox card = new VBox(10);
        card.getStyleClass().add("timeline-card");
        card.setPadding(new Insets(15));

        Label sessionTitle = new Label(s.getTitle());
        sessionTitle.getStyleClass().add("history-card-title");
        sessionTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        HBox tagsContainer = new HBox(8);
        tagsContainer.setAlignment(Pos.CENTER_LEFT);

        Label taskLabel = new Label(s.getTask());
        taskLabel.getStyleClass().add("task-badge");
        tagsContainer.getChildren().add(taskLabel);

        VBox details = new VBox(12);
        details.setManaged(false);
        details.setVisible(false);

        String start = s.getStartDate().length() >= 16 ? s.getStartDate().substring(11, 16) : "--:--";
        String end = s.getEndDate() != null && s.getEndDate().length() >= 16 ? s.getEndDate().substring(11, 16) : "--:--";

        Label timeRange = new Label(start + " — " + end + " (" + s.getTotalMinutes() + " min)");
        timeRange.setStyle("-fx-text-fill: -text-muted; -fx-font-size: 12px;");

        Label desc = new Label(s.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: -text-muted; -fx-font-style: italic; -fx-font-size: 11px;");

        details.getChildren().addAll(timeRange, desc);

        card.setOnMouseClicked(e -> {
            boolean isExpanded = details.isVisible();
            details.setVisible(!isExpanded);
            details.setManaged(!isExpanded);
            if (!isExpanded) card.getStyleClass().add("card-expanded");
            else card.getStyleClass().remove("card-expanded");
        });

        card.getChildren().addAll(sessionTitle, tagsContainer, details);
        return card;
    }
}