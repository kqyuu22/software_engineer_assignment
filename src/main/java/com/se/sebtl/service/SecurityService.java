package com.se.sebtl.service;

import com.se.sebtl.exception.*;
import com.se.sebtl.model.AppUser;
import com.se.sebtl.model.AppRole;
import com.se.sebtl.repository.AppUserRepository;

import org.springframework.stereotype.Service;

@Service
public class SecurityService {
    private final AppUserRepository userDb;

    public SecurityService(AppUserRepository userDb) {
        this.userDb = userDb;
    }

    public AppUser verifyRole(String token, AppRole expectedRole) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new InvalidTokenException("Invalid or missing token");
        }
        int userId = Integer.parseInt(token.replace("Bearer ", ""));
        AppUser user = userDb.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("User not found for token: " + token));

        if (!user.getRole().equals(expectedRole)) {
            throw new AccessDeniedException("User " + user.getUserId() + " does not have the required role: " + expectedRole);
        }
        return user;
    }

    public int getUserIdFromToken(String token) {
        return Integer.parseInt(token.replace("Bearer ", ""));
    }
}