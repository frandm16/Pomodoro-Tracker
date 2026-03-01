package com.frandm.pomodoro;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class StudyTimelineDashboard extends ScrollPane {

    private static final String STYLE_CELL = "-fx-background-color: #1E2732; -fx-border-color: #2C3E50; -fx-border-width: 0.5;";
    private static final String COLOR_SESSION = "#8E44AD";
    private static final double HOUR_WIDTH = 150;
    private static final double ROW_HEIGHT = 40;
    private final GridPane gridContent;

    public StudyTimelineDashboard() {
        this.gridContent = new GridPane();
        this.setContent(this.gridContent);
        this.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
        this.setVbarPolicy(ScrollBarPolicy.NEVER);
        this.setPannable(true);
        this.setFitToWidth(false);

        this.parentProperty().addListener((obs, oldParent, newParent) -> {
            if (newParent != null) {
                this.maxWidthProperty().bind(
                        ((Region) newParent).widthProperty().multiply(0.5)
                );
            }
        });

        this.gridContent.setHgap(0);
        this.gridContent.setVgap(0);
        this.gridContent.setAlignment(Pos.CENTER);
        this.gridContent.setPadding(new Insets(10));
        loadTimelineData();
    }

    private void loadTimelineData() {
        this.gridContent.getChildren().clear();

        for (int hour = 0; hour < 24; hour++) {
            Label hourLabel = new Label(String.format("%02d:00", hour));
            hourLabel.setStyle("-fx-text-fill: #95A5A6; -fx-font-size: 10px;");
            hourLabel.setPrefWidth(HOUR_WIDTH);
            hourLabel.setAlignment(Pos.CENTER);
            this.gridContent.add(hourLabel, hour + 1, 0);
        }

        LocalDate startDate = LocalDate.now().minusDays(6);
        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);

            Label dateLabel = new Label(date.format(DateTimeFormatter.ofPattern("EEE, dd")));
            dateLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
            dateLabel.setPrefWidth(60);
            this.gridContent.add(dateLabel, 0, i + 1);

            Pane dayTimelinePane = new Pane();
            dayTimelinePane.setPrefSize(HOUR_WIDTH * 24, ROW_HEIGHT);
            dayTimelinePane.setStyle(STYLE_CELL);

            for (int h = 0; h < 24; h++) {
                Rectangle gridLine = new Rectangle(h * HOUR_WIDTH, 0, 0.5, ROW_HEIGHT);
                gridLine.setFill(Color.web("#2C3E50"));
                dayTimelinePane.getChildren().add(gridLine);
            }

            List<Session> sessionsToday = DatabaseHandler.getSessionsByDate(date);
            for (Session session : sessionsToday) {
                drawSessionBlock(dayTimelinePane, session);
            }

            this.gridContent.add(dayTimelinePane, 1, i + 1, 24, 1);
        }
    }

    private void drawSessionBlock(Pane pane, Session session) {
        String sql = "SELECT event_timestamp, event_type FROM session_events WHERE session_id = ? ORDER BY event_timestamp";
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        try (Connection conn = DriverManager.getConnection(DatabaseHandler.getDatabaseUrl());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, session.getId());
            ResultSet rs = pstmt.executeQuery();

            LocalDateTime currentStart = null;

            while (rs.next()) {
                LocalDateTime eventTime = LocalDateTime.parse(rs.getString("event_timestamp"), formatter);
                String eventType = rs.getString("event_type");

                if (eventType.equals("started")) {
                    currentStart = eventTime;
                } else if (eventType.equals("paused")) {
                    drawSegment(pane, session, currentStart, eventTime, "studying");
                    currentStart = eventTime;
                } else if (eventType.equals("resumed")) {
                    drawSegment(pane, session, currentStart, eventTime, "paused");
                    currentStart = eventTime;
                } else if (eventType.equals("finalized")) {
                    drawSegment(pane, session, currentStart, eventTime, "studying");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void drawSegment(Pane pane, Session session, LocalDateTime start, LocalDateTime end, String type) {
        double startX = calculateStartX(start.toString());
        long duration = Duration.between(start, end).toMinutes();
        double width = duration * (HOUR_WIDTH / 60.0);

        double height = type.equals("paused") ? (ROW_HEIGHT - 12) : (ROW_HEIGHT - 10);
        double yPos = type.equals("paused") ? 6 : 5;

        Rectangle block = new Rectangle(startX, yPos, Math.max(2, width), height);
        block.setArcWidth(5);
        block.setArcHeight(5);

        block.setFill(Color.web(type.equals("paused") ? "#FFF082" : COLOR_SESSION));

        Tooltip.install(block, new Tooltip(
                session.getSubject() + " - " + type.toUpperCase() + "\n" +
                        duration + " min\n" +
                        start.toLocalTime() + " - " + end.toLocalTime()
        ));

        pane.getChildren().add(block);
    }

    private double calculateStartX(String timestamp) {
        int hours = Integer.parseInt(timestamp.substring(11, 13));
        int minutes = Integer.parseInt(timestamp.substring(14, 16));
        return (hours + (minutes / 60.0)) * HOUR_WIDTH;
    }
}