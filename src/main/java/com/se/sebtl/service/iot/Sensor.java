package com.se.sebtl.service.iot;

import com.se.sebtl.model.SlotStatus;
import com.se.sebtl.service.IoTManagerService;

public class Sensor {
    private int slotId;
    private IoTManagerService iotManager;
    private SlotStatus internalState = SlotStatus.AVAILABLE;

    public Sensor(int slotId, IoTManagerService iotManager) {
        this.slotId = slotId;
        this.iotManager = iotManager;
    }

    public void reportAvailable() {
        this.internalState = SlotStatus.AVAILABLE;
        iotManager.onSensorUpdate(slotId, SlotStatus.AVAILABLE);
    }

    public void reportOccupied() {
        this.internalState = SlotStatus.OCCUPIED;
        iotManager.onSensorUpdate(slotId, SlotStatus.OCCUPIED);
    }

    public void reportFailure() {
        // simulate sensor malfunction
        iotManager.onSensorUpdate(slotId, SlotStatus.UNKNOWN);
    }

    public void restoreState() {
        iotManager.onSensorUpdate(slotId, internalState);
    }
}