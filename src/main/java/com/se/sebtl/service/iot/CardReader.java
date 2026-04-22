package com.se.sebtl.service.iot;

import org.springframework.stereotype.Service;

@Service
public class CardReader {
    private int simulatedUserId;

    public CardReader() {
        this.simulatedUserId = 1001;
    }

    public int readCard() {
        return simulatedUserId;
    }

    public void setSimulatedUserId(int simulatedUserId) {
        this.simulatedUserId = simulatedUserId;
    }
}