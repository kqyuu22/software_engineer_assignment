package com.hcmut.smartparking.database;

import java.sql.*;
import java.util.*;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.hcmut.smartparking.model.Alert;
import com.hcmut.smartparking.enums.AlertType;

@Repository
public class AlertDatabase {

    @Autowired
    private DataSource dataSource;

    // ── helper ─────────────────────────────────────────────

    private Alert mapRow(ResultSet rs) throws SQLException {
        Alert a = new Alert(
            AlertType.valueOf(rs.getString("type")),
            rs.getString("message")
        );
        a.setAlertId(rs.getInt("alert_id"));
        a.setTimestamp(rs.getTimestamp("timestamp"));
        a.setAcknowledged(rs.getBoolean("acknowledged"));
        return a;
    }

    // ── public methods ──────────────────────────────────────

    public void save(Alert alert) {
        String sql = """
            INSERT INTO alerts (type, message, timestamp, acknowledged)
            VALUES (?, ?, ?, ?)
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, alert.getType().name());
            stmt.setString(2, alert.getMessage());
            stmt.setTimestamp(3, new Timestamp(alert.getTimestamp().getTime()));
            stmt.setBoolean(4, false);

            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Alert save failed: " + e.getMessage());
        }
    }

    public void acknowledge(int alertId) {
        String sql = "UPDATE alerts SET acknowledged = true WHERE alert_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, alertId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Alert acknowledge failed: " + e.getMessage());
        }
    }

    public List<Alert> findUnacknowledged() {
        String sql = """
            SELECT * FROM alerts 
            WHERE acknowledged = false
            ORDER BY 
                CASE type 
                    WHEN 'SECURITY_BREACH' THEN 1 
                    WHEN 'SYSTEM_FAILURE' THEN 2 
                END ASC,
                timestamp DESC
        """;

        List<Alert> alerts = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) alerts.add(mapRow(rs));

        } catch (SQLException e) {
            System.err.println("findUnacknowledged failed: " + e.getMessage());
        }

        return alerts;
    }

    public List<Alert> findAcknowledged() {
        String sql = """
            SELECT * FROM alerts 
            WHERE acknowledged = true
            ORDER BY timestamp DESC
        """;

        List<Alert> alerts = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) alerts.add(mapRow(rs));

        } catch (SQLException e) {
            System.err.println("findAcknowledged failed: " + e.getMessage());
        }

        return alerts;
    }
}