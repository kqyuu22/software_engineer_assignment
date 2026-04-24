package com.hcmut.smartparking.database;

import java.sql.*;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.hcmut.smartparking.model.AppUser;
import com.hcmut.smartparking.exception.*;
import com.hcmut.smartparking.enums.AppRole;

@Repository
public class SSODatabase {

    @Autowired
    private DataSource dataSource;

    public AppUser login(String username, String password) 
            throws InvalidCredentialsException, DbUnreachableException {

        String sql = "SELECT * FROM sso_users WHERE username = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                throw new InvalidCredentialsException("Username not found: " + username);
            }

            String storedPassword = rs.getString("password");

            if (!storedPassword.equals(password)) {
                throw new InvalidCredentialsException("Incorrect password for: " + username);
            }

            AppUser user = new AppUser();
            user.setUserId(rs.getInt("user_id"));
            user.setName(rs.getString("name"));
            user.setUsername(rs.getString("username"));
            user.setPassword(storedPassword);
            user.setRole(AppRole.valueOf(rs.getString("role")));

            return user;

        } catch (InvalidCredentialsException e) {
            throw e;
        } catch (SQLException e) {
            throw new DbUnreachableException("SSO DB unreachable: " + e.getMessage());
        }
    }
}