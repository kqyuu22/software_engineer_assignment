package com.hcmut.smartparking.database;

import java.sql.*;
import java.util.*;
import java.util.Date;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.hcmut.smartparking.model.Ticket;
import com.hcmut.smartparking.enums.Roles;
import com.hcmut.smartparking.exception.DbUnreachableException;

@Repository
public class TicketDatabase {

    @Autowired
    private DataSource dataSource;

    // ── helpers ─────────────────────────────────────────────

    private Ticket mapRow(ResultSet rs) throws SQLException {
        Ticket t = new Ticket();
        t.setTicketId(rs.getInt("ticket_id"));
        t.setUserId(rs.getInt("user_id"));
        t.setRole(Roles.valueOf(rs.getString("role")));
        t.setEntryTime(rs.getTimestamp("entry_time"));
        t.setExitTime(rs.getTimestamp("exit_time"));
        t.setLicensePlate(rs.getString("license_plate"));
        t.setParkingSpot(rs.getInt("parking_spot"));
        t.setFinished(rs.getBoolean("finished"));
        t.setFee(rs.getDouble("fee"));
        t.setPrice(rs.getDouble("price"));
        return t;
    }

    // ── public methods ──────────────────────────────────────

    public void save(Ticket ticket) throws DbUnreachableException {
        String sql = """
            INSERT INTO tickets 
            (user_id, role, entry_time, license_plate, parking_spot, finished, fee, price)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, ticket.getUserId());
            stmt.setString(2, ticket.getRole().name());
            stmt.setTimestamp(3, new Timestamp(ticket.getEntryTime().getTime()));
            stmt.setString(4, ticket.getLicensePlate());
            stmt.setInt(5, ticket.getParkingSpot());
            stmt.setBoolean(6, false);
            stmt.setNull(7, Types.REAL);
            stmt.setDouble(8, ticket.getPrice());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DbUnreachableException("DB error: " + e.getMessage());
        }
    }

    public void update(Ticket ticket) throws DbUnreachableException {
        String sql = """
            UPDATE tickets
            SET exit_time = ?, finished = ?, fee = ?
            WHERE ticket_id = ?
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, new Timestamp(ticket.getExitTime().getTime()));
            stmt.setBoolean(2, true);
            stmt.setDouble(3, ticket.getFee());
            stmt.setInt(4, ticket.getTicketId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DbUnreachableException("DB error: " + e.getMessage());
        }
    }

    public boolean hasActiveTicket(int userId) {
        String sql = "SELECT 1 FROM tickets WHERE user_id = ? AND finished = false LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            return false;
        }
    }

    public boolean hasActiveTicketByPlate(String plate) {
        String sql = "SELECT 1 FROM tickets WHERE license_plate = ? AND finished = false LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, plate);
            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            return false;
        }
    }

    public Ticket findActiveByUserIdExit(int userId) throws DbUnreachableException {
        String sql = "SELECT * FROM tickets WHERE user_id = ? AND finished = false LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) return mapRow(rs);

        } catch (SQLException e) {
            throw new DbUnreachableException("DB error: " + e.getMessage());
        }

        return null;
    }

    public List<Ticket> findAllByUserId(int userId) {
        String sql = """
            SELECT * FROM tickets 
            WHERE user_id = ?
            ORDER BY finished ASC, entry_time DESC
        """;

        List<Ticket> tickets = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) tickets.add(mapRow(rs));

        } catch (SQLException e) {
            // log if needed
        }

        return tickets;
    }

    public List<Ticket> findAll() {
        String sql = "SELECT * FROM tickets ORDER BY entry_time DESC";

        List<Ticket> tickets = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) tickets.add(mapRow(rs));

        } catch (SQLException e) {}

        return tickets;
    }

    public List<Ticket> findByDateRange(Date from, Date to) {
        String sql = """
            SELECT * FROM tickets 
            WHERE entry_time >= ? AND entry_time <= ?
            ORDER BY entry_time DESC
        """;

        List<Ticket> tickets = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, new Timestamp(from.getTime()));
            stmt.setTimestamp(2, new Timestamp(to.getTime()));

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) tickets.add(mapRow(rs));

        } catch (SQLException e) {}

        return tickets;
    }
}