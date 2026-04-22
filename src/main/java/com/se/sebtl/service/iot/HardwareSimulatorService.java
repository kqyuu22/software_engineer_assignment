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
    }

    @PostConstruct 
    public void initializeHardware() {
        // Expand to 300 sensors for Sections C, B, and A
        for (int i = 1; i <= 300; i++) {
            sensors.add(new Sensor(i, iotManager));
        }

        // Exactly 3 signs for the 3 intersections
        for (int i = 1; i <= 3; i++) {
            signs.add(new IntersectionSign(iotManager));
        }
        
        iotManager.setSigns(signs); 
        System.out.println("[SYSTEM] Hardware array initialized with 300 sensors and 3 signs.");
    }
    
    public void simulateCarArrival(int assignedSlotId) {
        if (assignedSlotId < 1 || assignedSlotId > sensors.size()) {
            System.out.println("[ERROR] Invalid slot ID.");
            return;
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
                System.out.println("[CHAOS] Driver ignored assigned spot " + assignedSlotId + 
                                   " and parked in spot " + actualParkedSlot + " instead!");
            }
        }

        System.out.println("[SIMULATION] Hardware sensor triggered for slot " + actualParkedSlot);
        sensors.get(actualParkedSlot - 1).reportOccupied();
    }

    public void simulateCarDeparture(int slotId) {
        if (slotId > 0 && slotId <= sensors.size()) {
            sensors.get(slotId - 1).reportAvailable();
        }
    }

    public void simulateSensorFailure(int slotId) {
        if (slotId > 0 && slotId <= sensors.size()) {
            sensors.get(slotId - 1).reportFailure();
        }
    }
}