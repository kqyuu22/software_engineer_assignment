package com.se.sebtl.service.iot;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class Gate {
    
    public void open() {
        System.out.println("Gate: Opening gate");
        closeDelayed(); // Trigger the background thread
    }

    public void close() {
        System.out.println("Gate: Closing gate instantly");
    }

    @Async // Crucial: Moves the sleep to a background thread
    protected void closeDelayed() {
        try {
            Thread.sleep(3000);
            System.out.println("Gate: Closing gate after 3-second delay");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}