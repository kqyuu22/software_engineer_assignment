package com.hcmut.smartparking.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.hcmut.smartparking.database.SSODatabase;
import com.hcmut.smartparking.session.SessionManager;
import com.hcmut.smartparking.model.AppUser;
import com.hcmut.smartparking.exception.*;

@RestController
@RequestMapping("/auth")
public class LoginController {

    private final SSODatabase ssoDb;
    private final SessionManager sessionManager;

    @Autowired
    public LoginController(SSODatabase ssoDb, SessionManager sessionManager) {
        this.ssoDb = ssoDb;
        this.sessionManager = sessionManager;
    }

    // DTO for request
    public static class LoginRequest {
        public String username;
        public String password;
    }

    // DTO for response
    public static class LoginResponse {
        public int userId;
        public String role;

        public LoginResponse(int userId, String role) {
            this.userId = userId;
            this.role = role;
        }
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {

        try {
            AppUser user = ssoDb.login(request.username, request.password);
            sessionManager.startSession(user);

            System.out.println("Login successful: "
                + user.getName() + " [" + user.getRole() + "]");

            return new LoginResponse(user.getUserId(), user.getRole().name().toLowerCase());

        } catch (InvalidCredentialsException e) {
            throw new RuntimeException("Invalid username or password");

        } catch (DbUnreachableException e) {
            throw new RuntimeException("SSO unavailable");
        }
    }

    @PostMapping("/logout")
    public void logout(@RequestParam int userId) {
        sessionManager.endSession(userId);
    }
}