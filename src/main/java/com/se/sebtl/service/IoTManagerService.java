package com.se.sebtl.service;

import com.se.sebtl.repository.ParkingSlotRepository;
import com.se.sebtl.model.*;
import com.se.sebtl.service.iot.IntersectionSign;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
public class IoTManagerService {

    private final ParkingSlotRepository slotRepository; 
    private final TaskScheduler taskScheduler; 
    private List<IntersectionSign> signs;

    public IoTManagerService(ParkingSlotRepository slotRepository, TaskScheduler taskScheduler) {
        this.slotRepository = slotRepository;
        this.taskScheduler = taskScheduler;
    }

    public void setSigns(List<IntersectionSign> signs) {
        this.signs = signs;
    }

    @Transactional
    public int assignSpot(Role role) {
        // CHANGED: Using the locked query so nobody else can steal the spot while we process it
        List<ParkingSlot> available = slotRepository.findByStatusWithLock(SlotStatus.AVAILABLE);
        
        List<ParkingSlot> priority = available.stream()
            .filter(s -> s.getPriority() == role)
            .collect(Collectors.toList());

        List<ParkingSlot> candidates = priority.isEmpty() ? available : priority;

        if (candidates.isEmpty()) return -1;

        ParkingSlot assigned = candidates.stream()
            .min(Comparator.comparingInt(ParkingSlot::getSlotId))
            .orElseThrow();

        // 1. Reserves the spot instantly before doing anything else
        assigned.setStatus(SlotStatus.RESERVED);
        slotRepository.save(assigned);

        // 2. Triggers the hardware logic
        startReservationTimer(assigned.getSlotId());
        updateSigns(assigned.getSlotId());
        
        return assigned.getSlotId();
    }

    @Async 
    public void updateSigns(int slotId) {
        if (signs == null || signs.isEmpty()) return;
        
        for (int i = 0; i < signs.size(); i++) {
            IntersectionSign sign = signs.get(i);
            sign.updateDirection(getDirectionForSlot(slotId, i));
            try {
                Thread.sleep(3000); // Wait 3 seconds before updating next sign
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // --- The Optimized Routing Algorithm ---
    private Direction getDirectionForSlot(int slotId, int signIndex) {
        // Section C (Bottom/First section)
        if (slotId <= 100) {
            if (signIndex == 0) return Direction.RIGHT; // Turn into C
            if (signIndex > 0) return Direction.NONE;   // Driver is already parked, ignore upper signs
        } 
        // Section B (Middle/Second section)
        else if (slotId <= 200) {
            if (signIndex == 0) return Direction.STRAIGHT; // Skip C
            if (signIndex == 1) return Direction.RIGHT;    // Turn into B
            if (signIndex > 1) return Direction.NONE;      // Driver is already parked, ignore upper signs
        } 
        // Section A (Top/Third section)
        else {
            if (signIndex == 0) return Direction.STRAIGHT; // Skip C
            if (signIndex == 1) return Direction.STRAIGHT; // Skip B
            if (signIndex == 2) return Direction.RIGHT;    // Turn into A
        }
        
        return Direction.NONE; // Fallback safety
    }

    private void startReservationTimer(int slotId) {
        Instant executeTime = Instant.now().plus(Duration.ofMinutes(2)); // Reduced to 2 minutes
        
        taskScheduler.schedule(() -> {
            slotRepository.findById(slotId).ifPresent(slot -> {
                if (slot.getStatus() == SlotStatus.RESERVED) {
                    slot.setStatus(SlotStatus.AVAILABLE);
                    slotRepository.save(slot);
                    System.out.println("[SYSTEM] Timer expired. Slot " + slotId + " released back to AVAILABLE.");
                }
            });
        }, executeTime);
    }

    @Transactional
    public void onSensorUpdate(int slotId, SlotStatus status) {
        slotRepository.findById(slotId).ifPresent(slot -> {
            slot.setStatus(status);
            slotRepository.save(slot);
        });
        updateParkingLotStatus();
    }

    private void updateParkingLotStatus() {
        List<ParkingSlot> all = slotRepository.findAll();
        long occupied = all.stream().filter(s -> s.getStatus() == SlotStatus.OCCUPIED).count();
        long known = all.stream().filter(s -> s.getStatus() != SlotStatus.UNKNOWN).count();
        
        if (known == 0) return;
        
        double occupancy = (double) occupied / known;

        if (occupancy >= 1.0) updateAllSigns(ParkingLotStatus.FULL);
        else if (occupancy >= 0.9) updateAllSigns(ParkingLotStatus.NEARLY_FULL);
        else updateAllSigns(ParkingLotStatus.AVAILABLE);
    }

    private void updateAllSigns(ParkingLotStatus status) {
        if (signs == null) return;
        for (IntersectionSign sign : signs) {
            sign.updateStatus(status);
        }
    }
}