package com.se.sebtl.service.iot;

import com.se.sebtl.model.Direction;
import com.se.sebtl.model.ParkingLotStatus;
import com.se.sebtl.service.IoTManagerService;

public class IntersectionSign {
    private IoTManagerService iotManager;

    public IntersectionSign(IoTManagerService iotManager) {
        this.iotManager = iotManager;
    }

    public void updateDirection(Direction direction) {
        System.out.println("Sign direction updated to: " + direction);
    }

    public void updateStatus(ParkingLotStatus status) {
        System.out.println("Sign status updated to: " + status);
    }

    public void sendFail() {
        System.out.println("Sign failure simulated");
        // iotManager.reportSignFailure() — wired up later
    }
}