package com.se.sebtl.service;

import com.se.sebtl.exception.*;
import com.se.sebtl.model.AppRole;
import com.se.sebtl.model.AppUser;
import com.se.sebtl.repository.AppUserRepository;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {
    private final AppUserRepository userDb;

    public SecurityService(AppUserRepository userDb) {
        this.userDb = userDb;
    }
    
    public List<Object> parseToken(String token){
        // Token: "Bearer <userId>; Expr <date>" (ISO-8601)
        if (token == null || !token.startsWith("Bearer ")) {
            throw new InvalidTokenException("Invalid or missing token");
        }
        String[] parts = token.split("; ");
        if (parts.length < 2) {
            throw new InvalidTokenException("Missing expiry in token");
        }
        int userId = Integer.parseInt(parts[0].replace("Bearer ", ""));
        OffsetDateTime expiry = OffsetDateTime.parse(parts[1].replace("Expr ", ""));
        return Arrays.asList(userId, expiry);
    }

    public AppUser verifyRole(String token, AppRole expectedRole) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new InvalidTokenException("Invalid or missing token");
        }
        int userId = getUserIdFromToken(token);
        validateExpiry(token);
        AppUser user = userDb.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("User not found for token: " + token));

        if (!user.getRole().equals(expectedRole)) {
            throw new AccessDeniedException("User " + user.getUserId() + " does not have the required role: " + expectedRole);
        }
        return user;
    }

    public int getUserIdFromToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new InvalidTokenException("Invalid or missing token");
        }
        validateExpiry(token);
        String[] parts = token.split("; ");
        return Integer.parseInt(parts[0].replace("Bearer ", ""));
    }

    private void validateExpiry(String token) {
        String[] parts = token.split("; ");
        if (parts.length < 2) {
            throw new InvalidTokenException("Missing expiry in token");
        }
        OffsetDateTime expiry = OffsetDateTime.parse(parts[1].replace("Expr ", ""));
        if (expiry.isBefore(OffsetDateTime.now())) {
            throw new InvalidTokenException("Token expired");
        }
    }
}