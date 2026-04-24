package com.hcmut.smartparking.database;

import java.sql.*;
import java.util.*;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.hcmut.smartparking.model.ParkingSlot;
import com.hcmut.smartparking.enums.Roles;
import com.hcmut.smartparking.enums.SlotStatus;

@Repository
public class ParkingSlotDatabase {

    @Autowired
    private DataSource dataSource;

    // ── helper ─────────────────────────────────────────────

    private ParkingSlot mapRow(ResultSet rs) throws SQLException {
        ParkingSlot slot = new ParkingSlot();
        slot.setSlotId(rs.getInt("slot_id"));
        slot.setStatus(SlotStatus.valueOf(rs.getString("status")));
        slot.setPriority(Roles.valueOf(rs.getString("priority")));
        return slot;
    }

    // ── public methods ──────────────────────────────────────

    public void save(ParkingSlot slot) {
        String sql = """
            INSERT INTO parking_slots (status, priority)
            VALUES (?, ?)
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, SlotStatus.AVAILABLE.name());
            stmt.setString(2, slot.getPriority().name());

            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Slot save failed: " + e.getMessage());
        }
    }

    public void remove(int slotId) {
        String sql = "DELETE FROM parking_slots WHERE slot_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, slotId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Slot remove failed: " + e.getMessage());
        }
    }

    public void updateStatus(int slotId, SlotStatus status) {
        String sql = "UPDATE parking_slots SET status = ? WHERE slot_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setInt(2, slotId);

            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Slot status update failed: " + e.getMessage());
        }
    }

    public void updatePriority(int slotId, Roles priority) { //slow
        String sql = "UPDATE parking_slots SET priority = ? WHERE slot_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, priority.name());
            stmt.setInt(2, slotId);

            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Slot priority update failed: " + e.getMessage());
        }
    }

    public ParkingSlot findById(int slotId) {
        String sql = "SELECT * FROM parking_slots WHERE slot_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, slotId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) return mapRow(rs);

        } catch (SQLException e) {
            System.err.println("findById failed: " + e.getMessage());
        }

        return null;
    }

    public List<ParkingSlot> findAll() {
        String sql = "SELECT * FROM parking_slots ORDER BY slot_id ASC";

        List<ParkingSlot> slots = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) slots.add(mapRow(rs));

        } catch (SQLException e) {
            System.err.println("findAll failed: " + e.getMessage());
        }

        return slots;
    }

    public List<ParkingSlot> findAvailable() {
        String sql = """
            SELECT * FROM parking_slots 
            WHERE status = 'AVAILABLE' 
            ORDER BY slot_id ASC
        """;

        List<ParkingSlot> slots = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) slots.add(mapRow(rs));

        } catch (SQLException e) {
            System.err.println("findAvailable failed: " + e.getMessage());
        }

        return slots;
    }
}