package com.hcmut.smartparking.database;

import java.sql.*;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.hcmut.smartparking.enums.Roles;
import com.hcmut.smartparking.exception.*;

@Repository
public class SchoolDatabase {

    @Autowired
    private DataSource dataSource;

    public Roles getUserRole(int userId) throws UserNotFoundException, DbUnreachableException  {
        String sql = "SELECT role FROM uni_members WHERE user_id = ?";
        try (Connection conn = dataSource.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Roles.valueOf(rs.getString("role"));
            }
            throw new UserNotFoundException(userId); // not found → security event

        } catch (UserNotFoundException e) {
            throw e; // let it bubble up
        } catch (Exception e) {
            throw new DbUnreachableException("DATACORE unreachable: " + e.getMessage());
        }
    }
}
