package com.se.sebtl.controller;

import com.se.sebtl.repository.TicketViewRepository;
import org.apache.catalina.connector.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.se.sebtl.service.iot.HardwareSimulatorService;
import com.se.sebtl.service.IoTManagerService;
import com.se.sebtl.service.ParkingService;
import com.se.sebtl.model.Role;
import com.se.sebtl.model.SsoTicket;
import com.se.sebtl.model.GuestTicket;
import com.se.sebtl.model.Price;
import com.se.sebtl.model.Ticket;
import com.se.sebtl.model.SystemMode;
import com.se.sebtl.model.Alert;
import com.se.sebtl.model.AlertType;
import com.se.sebtl.model.ParkingSlot;
import com.se.sebtl.repository.SsoTicketRepository;
import com.se.sebtl.repository.GuestTicketRepository;
import com.se.sebtl.repository.UnimemberRepository;
import com.se.sebtl.repository.PriceRepository;
import com.se.sebtl.repository.BillingRepository;
import com.se.sebtl.repository.AlertRepository;
import com.se.sebtl.repository.TicketRepository;
import com.se.sebtl.service.iot.Gate;
import com.se.sebtl.service.iot.Camera;
import com.se.sebtl.service.iot.CardReader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/simulation")
public class SimulationApiController {

    private final TicketViewRepository ticketViewRepository;
    private final HardwareSimulatorService hardwareSimulator;
    private final IoTManagerService iotManager;
    private final SsoTicketRepository ssoTicketRepository;
    private final GuestTicketRepository guestTicketRepository;
    private final UnimemberRepository unimemberRepository;
    private final PriceRepository priceRepository;
    private final BillingRepository billingRepository;
    private final AlertRepository alertRepository;
    private final TicketRepository ticketRepository;
    private final ParkingService parkingService;

    private final Gate gate;
    private final Camera camera;
    private final CardReader cardReader;

    public SimulationApiController(
            HardwareSimulatorService hardwareSimulator, 
            IoTManagerService iotManager,
            SsoTicketRepository ssoTicketRepository,
            GuestTicketRepository guestTicketRepository,
            UnimemberRepository unimemberRepository,
            PriceRepository priceRepository,
            BillingRepository billingRepository,
            AlertRepository alertRepository,
            TicketRepository ticketRepository,
            ParkingService parkingService,
            Gate gate,
            Camera camera, CardReader cardReader, TicketViewRepository ticketViewRepository) {
        this.hardwareSimulator = hardwareSimulator;
        this.iotManager = iotManager;
        this.ssoTicketRepository = ssoTicketRepository;
        this.guestTicketRepository = guestTicketRepository;
        this.unimemberRepository = unimemberRepository;
        this.priceRepository = priceRepository;
        this.billingRepository = billingRepository;
        this.alertRepository = alertRepository;
        this.ticketRepository = ticketRepository;
        this.gate = gate;
        this.camera = camera;
        this.cardReader = cardReader;
        this.parkingService = parkingService;
        this.ticketViewRepository = ticketViewRepository;
    }

    @GetMapping("/slots")
    public ResponseEntity<List<ParkingSlot>> getAllSlots() {
        return ResponseEntity.ok(parkingService.getAllSlots()); 
    }

    @GetMapping("/signs")
    public ResponseEntity<?> getSignStatus() {
        return ResponseEntity.ok(iotManager.getSignDirections());
    }

    @GetMapping("/lot-status")
    public ResponseEntity<?> getLotStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("lotStatus", iotManager.getLotStatus());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/gate-status")
    public ResponseEntity<?> getGateStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("isOpen", gate.isOpen());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/system-mode")
    public ResponseEntity<?> getSystemMode() {
        Map<String, Object> response = new HashMap<>();
        response.put("mode", iotManager.getMode());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/system-mode")
    public ResponseEntity<?> setSystemMode(@RequestParam String mode) {
        SystemMode newMode = SystemMode.valueOf(mode.toUpperCase());
        iotManager.setMode(newMode);
        Map<String, Object> response = new HashMap<>();
        response.put("mode", iotManager.getMode());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sign-failure")
    public ResponseEntity<?> setSignFailure(@RequestParam int signIndex, @RequestParam boolean failed) {
        iotManager.setSignFailure(signIndex, failed);
        Map<String, Object> response = new HashMap<>();
        response.put("signIndex", signIndex);
        response.put("failed", failed);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sign-failure")
    public ResponseEntity<?> getSignFailures() {
        Map<String, Object> response = new HashMap<>();
        response.put("failures", iotManager.getSignFailures());
        return ResponseEntity.ok(response);
    }

    // ----- SCANNING ------
    @GetMapping("/scan-card")
    public ResponseEntity<?> scanCard(
        @RequestParam String param, 
        @RequestParam(required = false, defaultValue = "false") boolean read, 
        @RequestParam(required = false, defaultValue = "false") boolean isGuest
    ) {
        System.out.println("[SimulationApiController] Simulating card scan with param: " + param + " and read: " + read + " and isGuest: " + isGuest);

        cardReader.setSimulatedUserId(isGuest ? -1 : Integer.parseInt(param)); // Set to -1 for guest mode, otherwise use provided ID
        Map<String, Object> response = new HashMap<>();
        if (read) {
            cardReader.readCard();
            
            if (isGuest){
                response.put("message", "Card read successfully. Simulated as Guest (no ID).");
            }
            else {
                response.put("userId", cardReader.readCard());
            }
            return ResponseEntity.ok(response);
        }
        else {
            System.out.println("[SimulationApiController] Card reader failed. Create Alert.");
            Alert alert = new Alert();
            alert.setType(AlertType.SYSTEM_FAILURE);
            alert.setMessage("Card reader failed to capture ID at entrance.");
            alertRepository.save(alert);

            response.put("error", "Card reader failed to read card.");
            return ResponseEntity.badRequest().body(response);
        }
    }
    

    @GetMapping("/scan-plate")
    public ResponseEntity<?> scanPlate(@RequestParam(required = false, defaultValue = "random") String mode, 
                                       @RequestParam(required = false) String expectedPlate) {
        System.out.println("[SimulationApiController] Simulating plate scan with mode: " + mode + " and expected plate: " + expectedPlate);
        Map<String, String> response = new HashMap<>();
        String licensePlate = null;
        
        switch (mode.toLowerCase()) {
            case "correct":
                licensePlate = camera.captureCorrect(expectedPlate);
                break;
            case "wrong":
                licensePlate = camera.captureWrongPlate();
                break;
            case "fail":
                licensePlate = camera.captureFail();
                break;
            case "random":
            default:
                licensePlate = camera.capture();
                break;
        }
        
        if (licensePlate == null) {
            
            System.out.println("[SimulationApiController] Camera failed. Create Alert.");
            Alert alert = new Alert();
            alert.setType(AlertType.SYSTEM_FAILURE);
            alert.setMessage("Camera failed to capture license plate at entrance.");
            alertRepository.save(alert);

            response.put("error", "Camera failed to capture license plate.");
            return ResponseEntity.badRequest().body(response);
        }
        response.put("licensePlate", licensePlate);
        return ResponseEntity.ok(response);
    }

    @Transactional
    @PostMapping("/entrance")
    public ResponseEntity<?> entrance() {
        Map<String, Object> response = new HashMap<>();
        
        int lastScannedCardId = cardReader.isGuest() ? -1 : cardReader.readCard(); // If in guest mode, treat as -1 to avoid database lookup
        String lastCapturedPlate = camera.getLastCapturedPlate();

        if ((lastScannedCardId == -1 && !cardReader.isGuest()) || lastCapturedPlate == null || lastCapturedPlate.isEmpty()) {
            response.put("error", "Please scan a valid card and license plate.");
            return ResponseEntity.badRequest().body(response);
        }

        // Id already exists or none exists, either way we still create alert
        if (ticketViewRepository.existsByHolderIdentifierAndFinishedFalse(String.valueOf(lastScannedCardId))) {
            response.put("error", "Exception: Active ticket already exists for this card or plate. Card ID: " + lastScannedCardId + ", Plate: " + lastCapturedPlate);
            
            System.out.println("[SimulationApiController] Duplicate entry detected for card ID: " + lastScannedCardId + " or plate: " + lastCapturedPlate + ". Create Alert.");
            
            Alert alert = new Alert();
            alert.setType(AlertType.SECURITY_BREACH);
            alert.setMessage("Duplicate entry detected at entrance. Card ID: " + lastScannedCardId + ", Plate: " + lastCapturedPlate);
            alertRepository.save(alert);
            
            return ResponseEntity.badRequest().body(response);
        }

        if (!cardReader.isGuest() && !unimemberRepository.existsById(lastScannedCardId)) {
            response.put("error", "Exception: Card ID " + lastScannedCardId + " does not exist in unimember database.");
            
            System.out.println("[SimulationApiController] Invalid card ID detected: " + lastScannedCardId + ". Create Alert.");
            Alert alert = new Alert();
            alert.setType(AlertType.SECURITY_BREACH);
            alert.setMessage("Invalid card ID detected at entrance: " + lastScannedCardId);
            alertRepository.save(alert);
            
            return ResponseEntity.badRequest().body(response);
        }
        

        // Duplicate plate check to simulate error
        if (ticketViewRepository.existsByLicensePlateIgnoreCaseAndFinishedFalse(lastCapturedPlate)) {
            response.put("error", "Exception: Plate already exists in lot: " + lastCapturedPlate);
            
            System.out.println("[SimulationApiController] Duplicate plate detected: " + lastCapturedPlate + ". Create Alert.");
            Alert alert = new Alert();
            alert.setType(AlertType.SECURITY_BREACH);
            alert.setMessage("Duplicate license plate detected at entrance: " + lastCapturedPlate);
            alertRepository.save(alert);
            
            return ResponseEntity.badRequest().body(response);
        }

        // 1. Look up the role for the user
        // If card ID is -1, then database lookup will fail and we will treat as guest (OTHER role)
        Role role = unimemberRepository.findRoleByUserId(lastScannedCardId).orElse(Role.OTHER);
        
        // 2. Assign a spot
        int spotId = iotManager.assignSpot(role);
        if (spotId == -1) {
            response.put("error", "Exception: Parking lot is full or no priority spots available.");
            Alert alert = new Alert();
            alert.setType(AlertType.SYSTEM_FAILURE);
            alert.setMessage("Entrance denied: Parking lot full or no priority spots available for role: " + role);
            alertRepository.save(alert);
            return ResponseEntity.badRequest().body(response);
        }

        // 3. Create and save the ticket
        java.math.BigDecimal currentPrice = priceRepository.findById(role).map(Price::getPrice).orElse(java.math.BigDecimal.ZERO);
        
        System.out.println("[SimulationApiController] Creating ticket for plate: " + lastCapturedPlate + ", role: " + role + ", assigned spot: " + spotId + ", price: " + currentPrice);

        Integer ticketId;
        Ticket ticket = new Ticket();
        ticket.setEntryTime(java.time.OffsetDateTime.now());
        ticket.setLicensePlate(lastCapturedPlate);
        ticket.setParkingSpot(spotId == 0 ? null : spotId); // IF 0, set to null to avoid database overriding the price
        ticket.setFinished(false);
        ticket.setPrice(currentPrice); // Price is automatically computed by database trigger ONLY if spotId is not null
        ticketRepository.save(ticket);

        if (lastScannedCardId != -1 && unimemberRepository.existsById(lastScannedCardId)) {
            SsoTicket ssoTicket = new SsoTicket(lastScannedCardId, ticket);
            ssoTicketRepository.save(ssoTicket);
            ticketId = ticket.getTicketId();
        } else {
            GuestTicket guestTicket = new GuestTicket(ticket);
            guestTicketRepository.save(guestTicket);
            ticketId = ticket.getTicketId();
        }
        
        // 4. Gate opens
        gate.open();

        response.put("success", true);
        response.put("message", "Vehicle entered successfully. Gate is opening. Plate scanned: " + lastCapturedPlate + ", Assigned spot: " + spotId);
        response.put("ticketId", ticketId);
        response.put("licensePlate", lastCapturedPlate);
        response.put("slotId", spotId);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/assign-spot")
    public ResponseEntity<?> assignSpot(@RequestParam String role) {
        try {
            Role userRole = Role.valueOf(role.toUpperCase());
            int assignedSpot = iotManager.assignSpot(userRole);
            Map<String, Object> response = new HashMap<>();
            if (assignedSpot != -1) {
                response.put("success", true);
                response.put("message", "Spot " + assignedSpot + " assigned and RESERVED. Timer started.");
                response.put("slotId", assignedSpot);
            } else {
                response.put("success", false);
                response.put("message", "Lot is full or no priority spots available for this role.");
                Alert alert = new Alert();
                alert.setType(AlertType.SYSTEM_FAILURE);
                alert.setMessage("Parking lot full or no priority spots available for role: " + userRole);
                alertRepository.save(alert);
            }
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid Role. Please type STAFF, LECTURER, STUDENT, or OTHER.");
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/car-arrival")
    public ResponseEntity<?> carArrival(@RequestParam int slotId) {
        hardwareSimulator.simulateCarArrival(slotId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Triggered car arrival for slot " + slotId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/car-departure")
    public ResponseEntity<?> carDeparture(@RequestParam int slotId) {
        hardwareSimulator.simulateCarDeparture(slotId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Triggered car departure for slot " + slotId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sensor-failure")
    public ResponseEntity<?> sensorFailure(@RequestParam int slotId) {
        hardwareSimulator.simulateSensorFailure(slotId);
        
        Alert alert = new Alert();
        alert.setType(AlertType.SYSTEM_FAILURE);
        alert.setMessage("Hardware sensor malfunction detected at slot " + slotId);
        alertRepository.save(alert);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Triggered sensor failure for slot " + slotId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sensor-fix")
    public ResponseEntity<?> sensorFix(@RequestParam int slotId) {
        hardwareSimulator.simulateSensorFix(slotId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Triggered sensor fix for slot " + slotId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sensor-failure-bulk")
    public ResponseEntity<?> sensorFailureBulk(@RequestBody List<Integer> slotIds) {
        for (int slotId : slotIds) {
            hardwareSimulator.simulateSensorFailure(slotId);
        }
        Alert alert = new Alert();
        alert.setType(AlertType.SYSTEM_FAILURE);
        alert.setMessage("Hardware sensor malfunction detected at slots: " + slotIds.toString());
        alertRepository.save(alert);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Triggered sensor failure for " + slotIds.size() + " slots");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sensor-fix-bulk")
    public ResponseEntity<?> sensorFixBulk(@RequestBody List<Integer> slotIds) {
        for (int slotId : slotIds) {
            hardwareSimulator.simulateSensorFix(slotId);
        }
        Map<String, String> response = new HashMap<>();
        response.put("message", "Triggered sensor fix for " + slotIds.size() + " slots");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active-tickets")
    public ResponseEntity<List<Map<String, Object>>> getActiveTickets() {
        List<Map<String, Object>> active = new java.util.ArrayList<>();
        
        ssoTicketRepository.findByFinishedFalse().forEach(t -> {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", t.getUserId());
            map.put("licensePlate", t.getLicensePlate());
            active.add(map);
        });

        guestTicketRepository.findAll().stream()
            .filter(t -> t.getFinished() != null && !t.getFinished())
            .forEach(t -> {
                Map<String, Object> map = new HashMap<>();
                map.put("userId", "Guest");
                map.put("licensePlate", t.getLicensePlate());
                active.add(map);
            });
            
        return ResponseEntity.ok(active);
    }

    

    @Transactional
    @PostMapping("/exit")
    public ResponseEntity<?> exit(@RequestParam int userId, @RequestParam String licensePlate) {
        Map<String, Object> response = new HashMap<>();
        
        Integer ticketId = null;

        if (userId > 0 && unimemberRepository.existsById(userId)) {
            Optional<SsoTicket> ticketOpt = ssoTicketRepository.findByUserIdAndFinishedFalse(userId);
            if (ticketOpt.isEmpty()) {
                response.put("error", "Exception: No active ticket found for this card (User ID: " + userId + ").");
                Alert alert = new Alert();
                alert.setType(AlertType.SECURITY_BREACH);
                alert.setMessage("Exit attempted but no active ticket found for User ID: " + userId);
                alertRepository.save(alert);
                return ResponseEntity.badRequest().body(response);
            }
            SsoTicket ticket = ticketOpt.get();
            if (ticket.getLicensePlate() == null || !ticket.getLicensePlate().equalsIgnoreCase(licensePlate)) {
                response.put("error", "Exception: Registered plate mismatch. Expected " + ticket.getLicensePlate() + " but scanned " + licensePlate);
                Alert alert = new Alert();
                alert.setType(AlertType.SECURITY_BREACH);
                alert.setMessage("Plate mismatch at exit. User ID " + userId + " expected " + ticket.getLicensePlate() + " but scanned " + licensePlate);
                alertRepository.save(alert);
                return ResponseEntity.badRequest().body(response);
            }
            ticketRepository.updateExitTimeAndFinish(ticket.getTicketId(), java.time.OffsetDateTime.now());
            ticketId = ticket.getTicketId();
            if (ticket.getParkingSpot() != null) {
                hardwareSimulator.simulateCarDeparture(ticket.getParkingSpot());
            }
        } else {
            Optional<GuestTicket> ticketOpt = guestTicketRepository.findByLicensePlateAndFinishedFalse(licensePlate);
            if (ticketOpt.isEmpty()) {
                response.put("error", "Exception: No active guest ticket found for plate: " + licensePlate);
                Alert alert = new Alert();
                alert.setType(AlertType.SECURITY_BREACH);
                alert.setMessage("Exit attempted but no active guest ticket found for plate: " + licensePlate);
                alertRepository.save(alert);
                return ResponseEntity.badRequest().body(response);
            }
            GuestTicket ticket = ticketOpt.get();
            ticketRepository.updateExitTimeAndFinish(ticket.getTicketId(), java.time.OffsetDateTime.now());
            ticketId = ticket.getTicketId();
            if (ticket.getParkingSpot() != null) {
                hardwareSimulator.simulateCarDeparture(ticket.getParkingSpot());
            }
        }

        // 4. Gate opens
        gate.open();

        response.put("success", true);
        response.put("message", "Vehicle exited successfully. Gate is opening. Database has handled checkout.");
        response.put("ticketId", ticketId);

        return ResponseEntity.ok(response);
    }
}
