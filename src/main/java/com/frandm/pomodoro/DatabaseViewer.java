package com.frandm.pomodoro;

import java.sql.*;
// simple debug view of the db
public class DatabaseViewer {
    public static void main(String[] args) {
        String url = "jdbc:sqlite:pomodoro.db";
        String sql = "SELECT * FROM sessions";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("ID | Subject | Minutes | Date");
            System.out.println("-----------------------------------");
            while (rs.next()) {
                System.out.println(
                        rs.getInt("id") + " | " +
                                rs.getString("subject") + " | " +
                                rs.getInt("duration_minutes") + " | " +
                                rs.getString("timestamp")
                );
            }
        } catch (SQLException e) {
            System.out.println("Error reading database: " + e.getMessage());
        }
    }
}