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
            boolean success = configDir.mkdirs();
            if(!success){
                System.err.println("Error creating config folder");
            }
        }

        File dbFile = new File(configDir, DB_NAME);
        return "jdbc:sqlite:" + dbFile.toPath().toAbsolutePath();
    }

    public static void initializeDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS sessions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "subject TEXT NOT NULL, " +
                "topic TEXT, " +
                "description TEXT, " +
                "duration_minutes INTEGER NOT NULL, " +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)";

        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             Statement test = conn.createStatement()) {
            test.execute(sql);
            System.out.println("[DEBUG] Database initialized correctly");
        } catch (SQLException e) {
            System.err.println("DB Init Error: " + e.getMessage());
        }
    }

    public static void saveSession(String subject, String topic, String description, int minutes) {
        if(minutes>=0){
            String sql = "INSERT INTO sessions(subject, topic, description, duration_minutes) VALUES(?, ?, ?, ?)";

            try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
                 PreparedStatement test = conn.prepareStatement(sql)) {

                test.setString(1, subject);
                test.setString(2, topic);
                test.setString(3, description);
                test.setInt(4, minutes);

                test.executeUpdate();
                System.out.println("[DEBUG] Session saved: " + subject + " - " + topic + " - " + minutes + " sec");

            } catch (SQLException e) {
                System.err.println("Error saving session: " + e.getMessage());
            }
        }
    }

    public static ObservableList<Session> getAllSessions() {
        ObservableList<Session> sessions = FXCollections.observableArrayList();
        String sql = "SELECT subject, topic, duration_minutes, timestamp FROM sessions ORDER BY timestamp DESC";

        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                sessions.add(new Session(
                        rs.getString("timestamp"),
                        rs.getString("subject"),
                        rs.getString("topic"),
                        rs.getInt("duration_minutes")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getAllSessions(): " + e.getMessage());
        }
        return sessions;
    }

    public static java.util.Map<java.time.LocalDate, Integer> getMinutesPerDayLastYear() {
        java.util.Map<java.time.LocalDate, Integer> data = new java.util.HashMap<>();
        String sql = "SELECT date(timestamp) as day, SUM(duration_minutes) as total " +
                "FROM sessions " +
                "WHERE timestamp >= date('now', '-1 year') " +
                "GROUP BY day";

        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                data.put(java.time.LocalDate.parse(rs.getString("day")), rs.getInt("total"));
            }
        } catch (SQLException e) {
            System.err.println("Error loading heatmap: " + e.getMessage());
        }
        return data;
    }

    public static void generateRandomPomodoros() {
        String sql = "INSERT INTO sessions(subject, topic, description, duration_minutes, timestamp) VALUES(?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(getDatabaseUrl())) {
            conn.setAutoCommit(false);
            try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {

                java.util.Random random = new java.util.Random();
                java.time.LocalDate today = java.time.LocalDate.now();
                String[] subjects = {"subject1", "subject2", "subject3", "subject4", "subject5"};

                for (int i = 0; i < 365; i++) {
                    java.time.LocalDate date = today.minusDays(i);
                    // 70% to fill that day
                    if (random.nextDouble() < 0.7) {
                        int sessionsToday = random.nextInt(8) + 1;

                        for (int s = 0; s < sessionsToday; s++) {
                            String subject = subjects[random.nextInt(subjects.length)];
                            int minutes = 25;

                            preparedStatement.setString(1, subject);
                            preparedStatement.setString(2, "session " + s);
                            preparedStatement.setString(3, "testing the heatmap");
                            preparedStatement.setInt(4, minutes);
                            preparedStatement.setString(5, date + " 12:00:00");

                            preparedStatement.addBatch();
                        }
                    }
                }
                preparedStatement.executeBatch();
                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Error: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}