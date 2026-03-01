package com.frandm.pomodoro;


import java.sql.*;

public class Session {
    private int id;
    private final String timestamp;
    private final String date;
    private final String subject;
    private final String topic;
    private final String description;
    private final int duration;

    public Session(int id, String timestamp, String date, String subject, String topic, String description, int duration) {
        this.id = id;
        this.timestamp = timestamp;
        this.date = date;
        this.subject = subject;
        this.topic = topic;
        this.description = description;
        this.duration = duration;
    }

    public int getId() { return id;}
    public String getTimestamp() { return timestamp; }
    public String getDate() { return date;}
    public String getSubject() { return subject; }
    public String getTopic() { return topic; }
    public String getDescription() { return description; }
    public int getDuration() { return duration; }

    public boolean hasEvents(String eventType) {
        String sql = "SELECT COUNT(*) FROM session_events WHERE session_id = ? AND event_type = ?";

        try (Connection conn = DriverManager.getConnection(DatabaseHandler.getDatabaseUrl());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, this.id);
            pstmt.setString(2, eventType);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
