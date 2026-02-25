package com.frandm.pomodoro;

import java.sql.*;

public class DatabaseHandler {
    private static final String URL = "jdbc:sqlite:pomodoro.db";

    public static void initializeDatabase() {

        String sql = "CREATE TABLE IF NOT EXISTS sessions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "subject TEXT NOT NULL, " +
                "topic TEXT, " +
                "description TEXT, " +
                "duration_minutes INTEGER NOT NULL, " +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)";

        try (Connection conn = DriverManager.getConnection(URL);
             Statement test = conn.createStatement()) {
            test.execute(sql);
            System.out.println("[DEBUG] database initialized correctly");
        } catch (SQLException e) {
            System.err.println("DB Init Error: " + e.getMessage());
        }
    }

    public static void saveSession(String subject, String topic, String description, int minutes) {
        String sql = "INSERT INTO sessions(subject, topic, description, duration_minutes) VALUES(?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement test = conn.prepareStatement(sql)) {

            test.setString(1, subject);
            test.setString(2, topic);
            test.setString(3, description);
            test.setInt(4, minutes);

            test.executeUpdate();
            System.out.println("[DEBUG] session saved: " + subject + " - " + topic);

        } catch (SQLException e) {
            System.err.println("save error: " + e.getMessage());
        }
    }
}