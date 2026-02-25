package com.frandm.pomodoro;

import java.sql.*;

public class DatabaseViewer {
    public static void main(String[] args) {
        String url = "jdbc:sqlite:pomodoro.db";
        String sql = "SELECT id, subject, topic, description, duration_minutes, timestamp FROM sessions";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.printf("%-3s | %-12s | %-12s | %-15s | %-5s | %-19s%n",
                    "ID", "Subject", "Topic", "Description", "Min", "Date");
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