package com.se.sebtl.service.iot;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class Gate {

    private boolean isOpen;
    
    public void open() {
        System.out.println("Gate: Opening gate");
        isOpen = true;
        closeDelayed(); // Trigger the background thread
    }

    public void close() {
        System.out.println("Gate: Closing gate instantly");
        isOpen = false;
    }

    @Async // Crucial: Moves the sleep to a background thread
    protected void closeDelayed() {
        try {
            Thread.sleep(5000);
            System.out.println("Gate: Closing gate after 5-second delay");
            isOpen = false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean isOpen() {
        return isOpen;
    }
}