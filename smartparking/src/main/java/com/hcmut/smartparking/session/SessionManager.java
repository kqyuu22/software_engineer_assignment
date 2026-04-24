package com.hcmut.smartparking.session;

import java.util.*;
import com.hcmut.smartparking.model.AppUser;
import org.springframework.stereotype.Component;

@Component
public class SessionManager {
    private Map<Integer, AppUser> activeSessions = new HashMap<>();
    private Map<Integer, Long> lastActivity = new HashMap<>();     // userId → timestamp
    private static final long TIMEOUT_MS = 30 * 60 * 1000; // 30 minutes

    public void startSession(AppUser user) {
        activeSessions.put(user.getUserId(), user);
        lastActivity.put(user.getUserId(), System.currentTimeMillis());
        startTimeoutWatcher(user.getUserId());
    }

    public void refreshSession(int userId) {
        // called every time user performs an action
        lastActivity.put(userId, System.currentTimeMillis());
    }

    public void endSession(int userId) {
        activeSessions.remove(userId);
        lastActivity.remove(userId);
    }

    private void startTimeoutWatcher(int userId) {
        new Thread(() -> {
            while (activeSessions.containsKey(userId)) {
                try {
                    Thread.sleep(60000); // check every minute
                    long inactive = System.currentTimeMillis() - lastActivity.get(userId);
                    if (inactive >= TIMEOUT_MS) {
                        endSession(userId);
                        // notify UI to redirect to login with timeout message
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    public boolean isActive(int userId) {
        return activeSessions.containsKey(userId);
    }
}