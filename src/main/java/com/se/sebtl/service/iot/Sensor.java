package com.se.sebtl.service.iot;

import com.se.sebtl.model.SlotStatus;
import com.se.sebtl.service.IoTManagerService;

public class Sensor {
    private int slotId;
    private IoTManagerService iotManager;

    public Sensor(int slotId, IoTManagerService iotManager) {
        this.slotId = slotId;
        this.iotManager = iotManager;
    }

    public void reportAvailable() {
        iotManager.onSensorUpdate(slotId, SlotStatus.AVAILABLE);
    }

    public void reportOccupied() {
        iotManager.onSensorUpdate(slotId, SlotStatus.OCCUPIED);
    }

    public void reportFailure() {
        // simulate sensor malfunction
        iotManager.onSensorUpdate(slotId, SlotStatus.UNKNOWN);
    }
}