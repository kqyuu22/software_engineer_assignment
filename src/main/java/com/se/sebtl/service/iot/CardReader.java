package com.se.sebtl.service.iot;

import org.springframework.stereotype.Service;

@Service
public class CardReader {
    private int simulatedUserId;
    private boolean isGuest;

    public CardReader() {
        this.simulatedUserId = -1; // Default to -1 to indicate no card read
        this.isGuest = true; // Default to guest mode (no card) for simulation purposes
    }

    public int readCard() {
        return isGuest ? -1 : simulatedUserId;
    }

    public void setSimulatedUserId(int simulatedUserId) {
        this.simulatedUserId = simulatedUserId;
        this.isGuest = (simulatedUserId == -1); // If userId is -1, treat as guest
    }

    public boolean isGuest() {
        return isGuest;
    }
}