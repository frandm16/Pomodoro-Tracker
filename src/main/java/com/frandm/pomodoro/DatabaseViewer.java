package com.frandm.pomodoro;

import java.io.File;
import java.sql.*;

public class DatabaseViewer {
    private static final String FOLDER_NAME = ".pomodoro_app";
    private static final String DB_NAME = "pomodoro.db";

    private static String getDatabaseUrl() {
        String userHome = System.getProperty("user.home");
        File dbFile = new File(userHome + File.separator + FOLDER_NAME, DB_NAME);
        return "jdbc:sqlite:" + dbFile.getAbsolutePath();
    }

    static void main(String[] args) {
        String url = getDatabaseUrl();
        String sql = "SELECT id, subject, topic, description, duration_seconds, timestamp FROM sessions";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.printf("%-3s | %-12s | %-12s | %-15s | %-5s | %-19s%n",
                    "ID", "Subject", "Topic", "Description", "Sec", "Date");
            System.out.println("---------------------------------------------------------------------------------------");

            while (rs.next()) {
                System.out.printf("%-3d | %-12s | %-12s | %-15s | %-5d | %-19s%n",
                        rs.getInt("id"),
                        rs.getString("subject"),
                        rs.getString("topic"),
                        rs.getString("description"),
                        rs.getInt("duration_minutes"),
                        rs.getString("timestamp")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error reading database: " + e.getMessage());
        }
    }
}