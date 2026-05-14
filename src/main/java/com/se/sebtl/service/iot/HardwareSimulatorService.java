package com.se.sebtl.service.iot;

import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.se.sebtl.service.IoTManagerService;
import com.se.sebtl.repository.ParkingSlotRepository;
import com.se.sebtl.model.ParkingSlot;
import com.se.sebtl.model.SlotStatus;

@Service
public class HardwareSimulatorService {
    
    private final IoTManagerService iotManager;
    private final ParkingSlotRepository slotRepository; // Needed to find empty spots for rogue drivers
    private final List<Sensor> sensors = new ArrayList<>();
    private final List<IntersectionSign> signs = new ArrayList<>();
    private final Random random = new Random();

    public HardwareSimulatorService(IoTManagerService iotManager, ParkingSlotRepository slotRepository) {
        this.iotManager = iotManager;
        this.slotRepository = slotRepository;
        // initializeHardware(); // This is already called by Spring after construction in SimulationApiController, so we don't need to call it here.
        // Initialize the sensor status based on the current state of the parking lot in the database
        slotRepository.findAll().forEach(slot -> {
            int slotId = slot.getSlotId();
            SlotStatus status = slot.getStatus();

            if (slotId % 48 == 1) {
                System.out.println("[HardwareSimulator] Initializing sensor for slot " + slotId + " with status " + status);
            }

            if (slotId > 0 && slotId <= sensors.size()) {
                sensors.get(slotId - 1).setInternalState(status);
            }
        });
    }

    @PostConstruct 
    public void initializeHardware() {
        if (!sensors.isEmpty()){
            // Empty the lists if they were already initialized to avoid duplicates
            System.out.println("[HardwareSimulator] Clearing existing hardware arrays before re-initialization.");
            sensors.clear();
        }
        if (!signs.isEmpty()) {
            System.out.println("[HardwareSimulator] Clearing existing signs array before re-initialization.");
            signs.clear();
        }

        for (int i = 1; i <= 240; i++) {
            sensors.add(new Sensor(i, iotManager));
        }

        for (int i = 1; i <= 3; i++) {
            signs.add(new IntersectionSign(iotManager));
        }
        
        iotManager.setSigns(signs); 
        System.out.println("[SYSTEM] Hardware array initialized with " + sensors.size() + " sensors and " + signs.size() + " signs.");
    }
    
    public boolean simulateCarArrival(int assignedSlotId) {
        System.out.println("[HardwareSimulator] Simulating car arrival at slot " + assignedSlotId);
        if (assignedSlotId < 1 || assignedSlotId > sensors.size()) {
            System.out.println("[HardwareSimulator] ERROR: Invalid slot ID.");
            return false;
        }
        // If the current slot is not available or reserved
        SlotStatus currentSlotStatus = sensors.get(assignedSlotId - 1).getInternalState();
        if (currentSlotStatus != SlotStatus.AVAILABLE && currentSlotStatus != SlotStatus.RESERVED) {
            System.out.println("[HardwareSimulator] ERROR: Assigned slot " + assignedSlotId + " is not available. Cannot simulate arrival.");
            return false;
        }

        int actualParkedSlot = assignedSlotId;

        // 10% chance to park in the wrong spot within the same section
        if (random.nextDouble() < 0.10) {
            int minBound = ((assignedSlotId - 1) / 100) * 100 + 1;
            int maxBound = minBound + 99;

            // Query DB for available spots in this specific bracket
            List<ParkingSlot> availableInSection = slotRepository.findAll().stream()
                .filter(s -> s.getSlotId() >= minBound && s.getSlotId() <= maxBound)
                .filter(s -> s.getStatus() == SlotStatus.AVAILABLE)
                .collect(Collectors.toList());

            if (!availableInSection.isEmpty()) {
                // Pick a random available spot from the list
                actualParkedSlot = availableInSection.get(random.nextInt(availableInSection.size())).getSlotId();
                System.out.println("[HardwareSimulator] CHAOS: Driver ignored assigned spot " + assignedSlotId + 
                                   " and parked in spot " + actualParkedSlot + " instead!");
            }
        }

        System.out.println("[HardwareSimulator] Hardware sensor triggered for slot " + actualParkedSlot);
        sensors.get(actualParkedSlot - 1).reportOccupied();
        return true;
    }

    public boolean simulateCarDeparture(int slotId) {
        if (slotId < 1 || slotId > sensors.size()) {
            System.out.println("[HardwareSimulator] ERROR: Invalid slot ID.");
            return false;
        }
        if (sensors.get(slotId - 1).getInternalState() != SlotStatus.OCCUPIED) {
            System.out.println("[HardwareSimulator] ERROR: Slot " + slotId + " is not currently occupied. Cannot simulate departure.");
            return false;
        }

        sensors.get(slotId - 1).setInternalState(SlotStatus.AVAILABLE);
        iotManager.reserveSlotForExit(slotId);
        return true;
    }

    public void simulateSensorFailure(int slotId) {
        if (slotId > 0 && slotId <= sensors.size()) {
            sensors.get(slotId - 1).reportFailure();
        }
    }

    public void simulateSensorFix(int slotId) {
        if (slotId > 0 && slotId <= sensors.size()) {
            System.out.println("[HardwareSimulator] Hardware sensor fixed at slot " + slotId + ". Rebooting to previous state.");
            sensors.get(slotId - 1).restoreState();
        } else {
            System.out.println("[HardwareSimulator] ERROR: Invalid slot ID.");
        }
    }

    public void simulateSensorFailureBulk(java.util.Collection<Integer> slotIds) {
        java.util.Map<Integer, SlotStatus> updates = new java.util.HashMap<>();
        for (int slotId : slotIds) {
            if (slotId > 0 && slotId <= sensors.size()) {
                updates.put(slotId, SlotStatus.UNKNOWN);
            }
        }
        iotManager.onSensorBulkUpdate(updates);
    }

    public void simulateSensorFixBulk(java.util.Collection<Integer> slotIds) {
        java.util.Map<Integer, SlotStatus> updates = new java.util.HashMap<>();
        for (int slotId : slotIds) {
            if (slotId > 0 && slotId <= sensors.size()) {
                Sensor sensor = sensors.get(slotId - 1);
                SlotStatus state = sensor.getInternalState();
                sensor.setInternalState(state);
                updates.put(slotId, state);
            }
        }
        iotManager.onSensorBulkUpdate(updates);
    }
}