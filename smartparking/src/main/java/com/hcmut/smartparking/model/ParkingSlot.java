package com.hcmut.smartparking.model;

import com.hcmut.smartparking.enums.Roles;
import com.hcmut.smartparking.enums.SlotStatus;

public class ParkingSlot {
    private int slotId;
    private SlotStatus status;
    private Roles priority;

    public ParkingSlot(){}
    public ParkingSlot(int slotId, Roles priority) {
        this.slotId = slotId;
        this.priority = priority;
        this.status = SlotStatus.AVAILABLE; // default status
    }

    public int getSlotId() { return slotId; }
    public void setSlotId(int slotId) { this.slotId = slotId; }

    public SlotStatus getStatus() { return status; }
    public void setStatus(SlotStatus status) { this.status = status; }

    public Roles getPriority() { return priority; }
    public void setPriority(Roles priority) { this.priority = priority; }
}