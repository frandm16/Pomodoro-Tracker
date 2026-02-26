package com.frandm.pomodoro;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.File;
import java.sql.*;

public class DatabaseHandler {
    private static final String FOLDER_NAME = ".pomodoro_app";
    private static final String DB_NAME = "pomodoro.db";

    private static String getDatabaseUrl() {
        String userHome = System.getProperty("user.home");
        File configDir = new File(userHome, FOLDER_NAME);

        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File dbFile = new File(configDir, DB_NAME);
        return "jdbc:sqlite:" + dbFile.getAbsolutePath();
    }

    public static void initializeDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS sessions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "subject TEXT NOT NULL, " +
                "topic TEXT, " +
                "description TEXT, " +
                "duration_seconds INTEGER NOT NULL, " +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)";

        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             Statement test = conn.createStatement()) {
            test.execute(sql);
            System.out.println("[DEBUG] database initialized correctly");
        } catch (SQLException e) {
            System.err.println("DB Init Error: " + e.getMessage());
        }
    }

    public static void saveSession(String subject, String topic, String description, int seconds) {
        if(seconds>=15){
            String sql = "INSERT INTO sessions(subject, topic, description, duration_seconds) VALUES(?, ?, ?, ?)";

            try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
                 PreparedStatement test = conn.prepareStatement(sql)) {

                test.setString(1, subject);
                test.setString(2, topic);
                test.setString(3, description);
                test.setInt(4, seconds);

                test.executeUpdate();
                System.out.println("[DEBUG] session saved: " + subject + " - " + topic + " - " + seconds + " sec");

            } catch (SQLException e) {
                System.err.println("save error: " + e.getMessage());
            }
        }
    }

    public static ObservableList<Session> getAllSessions() {
        ObservableList<Session> sessions = FXCollections.observableArrayList();
        String sql = "SELECT subject, topic, duration_seconds, timestamp FROM sessions ORDER BY timestamp DESC";

        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                sessions.add(new Session(
                        rs.getString("timestamp"),
                        rs.getString("subject"),
                        rs.getString("topic"),
                        rs.getInt("duration_seconds")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener sesiones: " + e.getMessage());
        }
        return sessions;
    }
}