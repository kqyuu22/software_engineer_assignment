package com.se.sebtl.service;

import com.se.sebtl.repository.ParkingSlotRepository;
import com.se.sebtl.model.*;
import com.se.sebtl.service.iot.IntersectionSign;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.se.sebtl.repository.TicketRepository;
import com.se.sebtl.repository.SsoTicketRepository;
import com.se.sebtl.repository.GuestTicketRepository;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
public class IoTManagerService {

    private final ParkingSlotRepository slotRepository; 
    private final TaskScheduler taskScheduler; 
    private final TransactionTemplate transactionTemplate;
    private final TicketRepository ticketRepository;
    private final SsoTicketRepository ssoTicketRepository;
    private final GuestTicketRepository guestTicketRepository;

    private List<IntersectionSign> signs;
    private java.util.Map<Integer, Direction> currentSignDirections = new java.util.concurrent.ConcurrentHashMap<>();
    private java.util.Map<Integer, Boolean> signFailures = new java.util.concurrent.ConcurrentHashMap<>();
    private ParkingLotStatus currentLotStatus = ParkingLotStatus.AVAILABLE;
    private SystemMode mode = SystemMode.NORMAL;
    private boolean sensorFailure = false;
    private boolean signFailure = false;

    public IoTManagerService(ParkingSlotRepository slotRepository, 
                             TaskScheduler taskScheduler,
                             TransactionTemplate transactionTemplate,
                             TicketRepository ticketRepository,
                             SsoTicketRepository ssoTicketRepository,
                             GuestTicketRepository guestTicketRepository) {
        this.slotRepository = slotRepository;
        this.taskScheduler = taskScheduler;
        this.transactionTemplate = transactionTemplate;
        this.ticketRepository = ticketRepository;
        this.ssoTicketRepository = ssoTicketRepository;
        this.guestTicketRepository = guestTicketRepository;
    }

    public void setSigns(List<IntersectionSign> signs) {
        this.signs = signs;
        for (int i = 0; i < signs.size(); i++) {
            currentSignDirections.put(i, Direction.NONE);
            signFailures.put(i, false);
        }
    }

    public java.util.Map<String, Object> getSignDirections() {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        for (int i = 0; i < signs.size(); i++) {
            result.put("sign_" + i, currentSignDirections.getOrDefault(i, Direction.NONE));
        }
        if (mode != SystemMode.MONITOR) {
            result.put("lotStatus", currentLotStatus);
        }
        return result;
    }

    @Transactional
    public int assignSpot(Role role) {
        if (mode == SystemMode.MONITOR) {
            return 0;
        }
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
            Direction direction = getDirectionForSlot(slotId, i);
            sign.updateDirection(direction);
            currentSignDirections.put(i, direction);
            System.out.println("[SIGN UPDATE] Sign " + i + " direction: " + direction);
            try {
                Thread.sleep(3000); // Wait 3 seconds before updating next sign
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // --- The Optimized Routing Algorithm ---
    private Direction getDirectionForSlot(int slotId, int signIndex) {
        if (mode == SystemMode.MONITOR) {
            return Direction.NONE;
        }
        // Section A (Bottom/First section)
        if (slotId <= 48) {
            if (signIndex == 0) return Direction.RIGHT; // Turn into A
            if (signIndex > 0) return Direction.NONE;   // Driver is already parked, ignore upper signs
        } 
        // Section B (Middle/Second section)
        else if (slotId <= 96) {
            if (signIndex == 0) return Direction.STRAIGHT; // Skip A
            if (signIndex == 1) return Direction.RIGHT;    // Turn into B
            if (signIndex > 1) return Direction.NONE;      // Driver is already parked, ignore upper signs
        } 
        // Section C (Top/Third section)
        else if (slotId <= 144) {
            if (signIndex == 0) return Direction.STRAIGHT; // Skip B
            if (signIndex == 1) return Direction.STRAIGHT; // Skip A
            if (signIndex == 2) return Direction.RIGHT;    // Turn into C
        }
        else if (slotId <= 192) {
            if (signIndex == 0) return Direction.STRAIGHT; // Skip A
            if (signIndex == 1) return Direction.STRAIGHT; // Skip B
            if (signIndex == 2) return Direction.LEFT;     // Turn into D
        }
        else if (slotId <= 240) {
            if (signIndex == 0) return Direction.STRAIGHT; // Skip A
            if (signIndex == 1) return Direction.LEFT; // Turn into E
            if (signIndex == 2) return Direction.NONE;
        }
        
        return Direction.NONE; // Fallback safety
    }

    private void startReservationTimer(int slotId) {
        Instant executeTime = Instant.now().plus(Duration.ofSeconds(30)); // Reduced to 30 seconds
        
        taskScheduler.schedule(() -> {
            transactionTemplate.execute(status -> {
                slotRepository.findById(slotId).ifPresent(slot -> {
                    if (slot.getStatus() == SlotStatus.RESERVED) {
                        slot.setStatus(SlotStatus.AVAILABLE);
                        slotRepository.save(slot);
                        System.out.println("[SYSTEM] Timer expired. Slot " + slotId + " released back to AVAILABLE.");
                        
                        // Delete any unfinished ticket holding this reservation
                        List<Ticket> unattendedTickets = ticketRepository.findByParkingSpotAndFinishedFalse(slotId);
                        for (Ticket t : unattendedTickets) {
                            ssoTicketRepository.deleteByTicket(t);
                            guestTicketRepository.deleteByTicket(t);
                            ticketRepository.delete(t);
                            System.out.println("[SYSTEM] Cancelled un-arrived ticket " + t.getTicketId() + " (Plate: " + t.getLicensePlate() + ")");
                        }
                    }
                });
                return null;
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
        checkSensorHealth();
    }

    private void updateParkingLotStatus() {
        if (mode == SystemMode.MONITOR) {
            currentLotStatus = null;
            return;
        }

        List<ParkingSlot> all = slotRepository.findAll();

        long occupied = all.stream()
                .filter(s -> s.getStatus() == SlotStatus.OCCUPIED)
                .count();

        long known = all.stream()
                .filter(s -> s.getStatus() != SlotStatus.UNKNOWN)
                .count();

        double occupancy = (known == 0) ? 0.0 : (double) occupied / known;

        ParkingLotStatus newStatus = ParkingLotStatus.AVAILABLE;
        if (occupancy >= 1.0) {
            newStatus = ParkingLotStatus.FULL;   
        } else if (occupancy >= 0.9) {
            newStatus = ParkingLotStatus.NEARLY_FULL;
        } else {
            newStatus = ParkingLotStatus.AVAILABLE;
        }

        if (newStatus != currentLotStatus) {
            currentLotStatus = newStatus;
            // signService.updateParkingStatus(currentLotStatus);
        }
    }

    private void checkSensorHealth() {
        List<ParkingSlot> all = slotRepository.findAll();

        long unknown = all.stream()
                .filter(s -> s.getStatus() == SlotStatus.UNKNOWN)
                .count();

        double failureRate = (double) unknown / all.size();

        sensorFailure = failureRate >= 0.25;

        if (sensorFailure) {
            updateModeIfNeeded(
                SystemMode.MONITOR,
                "Sensor failure exceeded 25%, degraded to MONITOR mode"
            );
        } else if (!signFailure) {
            updateModeIfNeeded(
                SystemMode.NORMAL,
                "System recovered to NORMAL mode (sensor OK)"
            );
        }
    }

    @Scheduled(fixedRate = 5000)
    public void monitorSensorHealth() {
        checkSensorHealth();
    }

    @Scheduled(fixedRate = 5000)
    public void monitorSignHealth() {
        if (signFailure) {
            updateModeIfNeeded(
                SystemMode.MONITOR,
                "Sign system failure detected, switching to MONITOR mode"
            );
        } else if (!sensorFailure) {
            updateModeIfNeeded(
                SystemMode.NORMAL,
                "System recovered to NORMAL mode (sign OK)"
            );
        }
    }

    private void updateModeIfNeeded(SystemMode newMode, String message) {
        if (mode != newMode) {
            mode = newMode;

            if (newMode == SystemMode.MONITOR) {
                currentLotStatus = null;
                resetSignDirections();
            }
            System.out.println("[SYSTEM MODE] " + message);
        }
    }

    private void resetSignDirections() {
        if (signs == null || signs.isEmpty()) return;
        for (int i = 0; i < signs.size(); i++) {
            IntersectionSign sign = signs.get(i);
            sign.updateDirection(Direction.NONE);
            currentSignDirections.put(i, Direction.NONE);
        }
    }

    private void updateAllSigns(ParkingLotStatus status) {
        if (signs == null) return;
        currentLotStatus = status;
        for (IntersectionSign sign : signs) {
            sign.updateStatus(status);
        }
        System.out.println("[LOT STATUS] Lot status updated to: " + status);
    }

    public ParkingLotStatus getLotStatus() {
        return currentLotStatus;
    }

    public SystemMode getMode() {
        return mode;
    }

    public void setMode(SystemMode newMode) {
        updateModeIfNeeded(newMode, "System mode updated to " + newMode);
    }

    public void setSignFailure(boolean hasFailure) {
        signFailure = hasFailure;
    }

    public void setSignFailure(int signIndex, boolean failed) {
        if (signIndex < 0 || signs == null || signIndex >= signs.size()) {
            return;
        }
        signFailures.put(signIndex, failed);
        signFailure = signFailures.values().stream().anyMatch(Boolean::booleanValue);
    }

    public java.util.Map<Integer, Boolean> getSignFailures() {
        return new java.util.HashMap<>(signFailures);
    }

}