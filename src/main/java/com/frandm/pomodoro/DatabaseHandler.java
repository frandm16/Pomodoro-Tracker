package com.frandm.pomodoro;

import java.sql.*;

public class DatabaseHandler {
    private static final String URL = "jdbc:sqlite:pomodoro.db";

    public static void initializeDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS sessions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "subject TEXT NOT NULL, " +
                "duration_minutes INTEGER NOT NULL, " +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)"; // date of the entry
        try (Connection conn = DriverManager.getConnection(URL);
             Statement test = conn.createStatement()) {
            test.execute(sql);
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public static void saveSession(String subject, int minutes) {
        String sql = "INSERT INTO sessions(subject, duration_minutes) VALUES(?, ?)";

        try (Connection conn = DriverManager.getConnection(URL);
            PreparedStatement test = conn.prepareStatement(sql)) {
            test.setString(1, subject);
            test.setInt(2, minutes);
            test.executeUpdate();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }
}