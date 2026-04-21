package com.se.sebtl.model;

import jakarta.persistence.*;


@Entity
@Table(name = "parking_slots") // Matches your Supabase table name
public class ParkingSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slot_id")
    private Integer slotId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SlotStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private Role priority;

    // Standard Getters and Setters
    public Integer getSlotId() { return slotId; }
    public void setSlotId(Integer slotId) { this.slotId = slotId; }

    public SlotStatus getStatus() { return status; }
    public void setStatus(SlotStatus status) { this.status = status; }

    public Role getPriority() { return priority; }
    public void setPriority(Role priority) { this.priority = priority; }
}