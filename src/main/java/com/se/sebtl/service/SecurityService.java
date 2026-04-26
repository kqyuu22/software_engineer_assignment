package com.se.sebtl.service;

import com.se.sebtl.exception.*;

@Service
public class SecurityService {
    private final AppUserRepository userDb;

    public SecurityService(AppUserRepository userDb) {
        this.userDb = userDb;
    }

    public AppUser verifyRole(String token, String expectedRole) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new InvalidTokenException("Invalid or missing token");
        }
        int userId = Integer.parseInt(token.replace("Bearer ", ""));
        AppUser user = userDb.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("User not found for token: " + token));

        if (!user.getRole().toString().equals(expectedRole)) {
            throw new AccessDeniedException("User " + user.getId() + " does not have the required role: " + expectedRole);
        }
        return user;
    }
}