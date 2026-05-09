package com.se.sebtl.controller;

import com.se.sebtl.repository.TicketViewRepository;
import org.apache.catalina.connector.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.se.sebtl.service.iot.HardwareSimulatorService;
import com.se.sebtl.service.IoTManagerService;
import com.se.sebtl.service.ParkingService;
import com.se.sebtl.model.*;
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
import com.se.sebtl.util.SlotIdParser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/simulation")
@CrossOrigin(origins = "*")
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

    @PostMapping("/slots/fill-all")
    @Transactional
    public ResponseEntity<?> fillAllSlots() {
        parkingService.fillAllAvailableSlots();
        iotManager.recalculateLotStatusAndHealth();
        Map<String, String> response = new HashMap<>();
        response.put("message", "All available slots set to OCCUPIED");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/slots/fill-percentage")
    @Transactional
    public ResponseEntity<?> fillSlotsToPercentage(@RequestParam double percentage) {
        parkingService.fillSlotsToPercentage(percentage);
        iotManager.recalculateLotStatusAndHealth();
        Map<String, String> response = new HashMap<>();
        response.put("message", "Slots filled to " + (percentage * 100) + "% occupancy");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/slots/fill-priority")
    @Transactional
    public ResponseEntity<?> fillPrioritySlots(@RequestParam String priority) {
        parkingService.fillPrioritySlots(Role.valueOf(priority.toUpperCase()));
        iotManager.recalculateLotStatusAndHealth();
        Map<String, String> response = new HashMap<>();
        response.put("message", "All " + priority + " slots set to OCCUPIED");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/slots/clear-all")
    @Transactional
    public ResponseEntity<?> clearAllSlots() {
        parkingService.clearAllOccupiedSlots();
        iotManager.recalculateLotStatusAndHealth();
        Map<String, String> response = new HashMap<>();
        response.put("message", "All occupied slots set to AVAILABLE");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-all")
    @Transactional
    public ResponseEntity<?> resetAll() {
        gate.close();
        hardwareSimulator.simulateSensorFixBulk(java.util.stream.IntStream.rangeClosed(1, 240).boxed().collect(java.util.stream.Collectors.toList()));
        ticketRepository.finishAllActiveTickets();
        parkingService.clearAllOccupiedSlots();
        iotManager.recalculateLotStatusAndHealth();
        iotManager.setMode(SystemMode.NORMAL);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Simulation reset to clean state");
        return ResponseEntity.ok(response);
    }

    // ----- IOT STATUS ENDPOINTS -----
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

    // @PostMapping("/sensor-failure")
    // public ResponseEntity<?> sensorFailure(@RequestParam int slotId) {
    //     hardwareSimulator.simulateSensorFailure(slotId);
        
    //     Alert alert = new Alert();
    //     alert.setType(AlertType.SYSTEM_FAILURE);
    //     alert.setMessage("Hardware sensor malfunction detected at slot " + slotId);
    //     alertRepository.save(alert);

    //     Map<String, String> response = new HashMap<>();
    //     response.put("message", "Triggered sensor failure for slot " + slotId);
    //     return ResponseEntity.ok(response);
    // }

    // @PostMapping("/sensor-fix")
    // public ResponseEntity<?> sensorFix(@RequestParam int slotId) {
    //     hardwareSimulator.simulateSensorFix(slotId);
    //     Map<String, String> response = new HashMap<>();
    //     response.put("message", "Triggered sensor fix for slot " + slotId);
    //     return ResponseEntity.ok(response);
    // }

    @PostMapping("/sensor-failure-bulk")
    public ResponseEntity<?> sensorFailureBulk(@RequestBody Map<String, String> body) {
        String slotsSpec = body.get("slots");
        if (slotsSpec == null || slotsSpec.isBlank()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Missing 'slots' parameter. Expected format: \"1-5, 8, 9-15, 20-23\"");
            return ResponseEntity.badRequest().body(error);
        }

        Set<Integer> slotIds;
        try {
            slotIds = SlotIdParser.parse(slotsSpec);
        } catch (NumberFormatException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid slot ID format: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }

        hardwareSimulator.simulateSensorFailureBulk(slotIds);

        Alert alert = new Alert();
        alert.setType(AlertType.SYSTEM_FAILURE);
        alert.setMessage("Hardware sensor malfunction detected at slots: " + slotIds.toString());
        alertRepository.save(alert);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Triggered sensor failure for " + slotIds.size() + " slots");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sensor-fix-bulk")
    public ResponseEntity<?> sensorFixBulk(@RequestBody Map<String, String> body) {
        String slotsSpec = body.get("slots");
        if (slotsSpec == null || slotsSpec.isBlank()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Missing 'slots' parameter. Expected format: \"1-5, 8, 9-15, 20-23\"");
            return ResponseEntity.badRequest().body(error);
        }

        Set<Integer> slotIds;
        try {
            slotIds = SlotIdParser.parse(slotsSpec);
        } catch (NumberFormatException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid slot ID format: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }

        hardwareSimulator.simulateSensorFixBulk(slotIds);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Triggered sensor fix for " + slotIds.size() + " slots");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active-tickets")
    public List<TicketView> getActiveTickets() {
        // List<Map<String, Object>> active = new java.util.ArrayList<>();
        
        // ssoTicketRepository.findByFinishedFalse().forEach(t -> {
        //     Map<String, Object> map = new HashMap<>();
        //     map.put("userId", t.getUserId());
        //     map.put("licensePlate", t.getLicensePlate());
        //     active.add(map);
        // });

        // guestTicketRepository.findAll().stream()
        //     .filter(t -> t.getFinished() != null && !t.getFinished())
        //     .forEach(t -> {
        //         Map<String, Object> map = new HashMap<>();
        //         map.put("userId", "Guest");
        //         map.put("licensePlate", t.getLicensePlate());
        //         active.add(map);
        //     });
            
        // return ResponseEntity.ok(active);

        return ticketViewRepository.findByFinishedFalseOrderByEntryTimeDesc();
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
        try {
            TicketView ticket = ticketViewRepository.findByParkingSpotAndFinishedFalse(slotId);
            System.out.println("[SimulationApiController] Simulating car departure for slot " + slotId + ". Found ticket: " + (ticket != null ? "Yes, Ticket ID: " + ticket.getTicketId() : "No"));
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Triggered car departure for slot " + slotId);
            if (ticket != null) {
                Role role;
                if (ticket.getTicketType().equals("GUEST")){
                    role = Role.OTHER;
                }
                else {
                    role = unimemberRepository.findRoleByUserId(Integer.parseInt(ticket.getHolderIdentifier())).orElse(Role.OTHER);
                }
                response.put("licensePlate", ticket.getLicensePlate());
                response.put("ticketId", ticket.getTicketId());
                response.put("ticketType", ticket.getTicketType());
                response.put("entryTime", ticket.getEntryTime());
                response.put("userId", ticket.getUserId());
                response.put("price", priceRepository.findById(role).map(Price::getPrice).orElse(java.math.BigDecimal.ZERO));
            } else {
                response.put("licensePlate", "Unknown");
                response.put("ticketId", "Unknown");
                response.put("ticketType", "Unknown");
                response.put("entryTime", "Unknown");
                response.put("userId", "Unknown");
                response.put("price", "Unknown");
            }
            hardwareSimulator.simulateCarDeparture(slotId);
            return ResponseEntity.ok(response);
        }
        catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error during car departure simulation: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    
    @PostMapping("/pay")
    public ResponseEntity<?> pay(@RequestParam String ticketType, @RequestParam String userId, @RequestParam java.math.BigDecimal price, @RequestParam boolean paidDirectly){
        System.out.println("[SimulationApiController] Processing payment for ticketType: " + ticketType + ", userId: " + userId + ", price: " + price + ", paidDirectly: " + paidDirectly);
        if (ticketType.equals("GUEST")){
            // UserID will in the format "GUEST-<ticketId>"
            String[] parts = userId.split("-");
            if (parts.length != 2 || !parts[0].equals("GUEST")) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid userId format for guest. Expected format: GUEST-<ticketId>");
                return ResponseEntity.badRequest().body(error);
            }
            try {
                int ticketId = Integer.parseInt(parts[1]);
                Optional<GuestTicket> guestTicketOpt = guestTicketRepository.findById(ticketId);
                if (guestTicketOpt.isEmpty()) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Guest ticket not found for ID: " + ticketId);
                    return ResponseEntity.badRequest().body(error);
                }
                
                GuestTicket guestTicket = guestTicketOpt.get();
                // guestTicket.setFinalCalculatedFee(priceRepository.findById(Role.OTHER).map(Price::getPrice).orElse(java.math.BigDecimal.ZERO)); 
                guestTicket.setFinalCalculatedFee(price);
                guestTicket.setPaidDirectly(paidDirectly);
                // Assuming guest tickets are charged at OTHER rate
                guestTicketRepository.save(guestTicket);

                BigDecimal finalFee = guestTicket.getFinalCalculatedFee();
                String licensePlate = guestTicket.getTicket().getLicensePlate();

                System.out.println("[SimulationApiController] Updated guest ticket ID " + ticketId + " with final calculated fee: " + finalFee);

                finalFee = finalFee != null ? finalFee : new java.math.BigDecimal(-1); // Handle null case just in case
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Payment successful for guest ticket ID: " + ticketId + ". Amount paid: " + finalFee);
                response.put("userId", userId);
                response.put("licensePlate", licensePlate);
                return ResponseEntity.ok(response);
            } catch (NumberFormatException e) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid ticket ID format. Expected an integer after GUEST-");
                return ResponseEntity.badRequest().body(error);
            }
        }
        if (ticketType.equals("SSO")){
            try {
                int userIdInt = Integer.parseInt(userId);
                Optional<SsoTicket> ssoTicketOpt = ssoTicketRepository.findByUserIdAndFinishedFalse(userIdInt);
                if (ssoTicketOpt.isEmpty()) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Active SSO ticket not found for user ID: " + userIdInt);
                    return ResponseEntity.badRequest().body(error);
                }
                
                SsoTicket ssoTicket = ssoTicketOpt.get();
                Billing bill = billingRepository.findByUserId(ssoTicket.getUserId());
                System.out.println("[SimulationApiController] Current bill for user ID " + ssoTicket.getUserId() + ", Bill ID: " + (bill != null ? bill.getBillId() : "No existing bill") + ". Adding price: " + price);
                
                if (bill == null) {
                    bill = new Billing();
                    bill.setUserId(ssoTicket.getUserId());
                    bill.setAmount(price);
                    bill.setBillingMonth(LocalDate.now().withDayOfMonth(1));
                    System.out.println("[SimulationApiController] Creating new bill for user ID " + ssoTicket.getUserId() + " with amount: " + price);
                }
                else {
                    bill.setAmount(bill.getAmount().add(price));
                    bill.setLastUpdated(java.time.OffsetDateTime.now());
                    System.out.println("[SimulationApiController] Updated bill amount for user ID " + ssoTicket.getUserId() + ", Bill ID: " + bill.getBillId() + ", New Amount: " + bill.getAmount());
                }
                System.out.println("[SimulationApiController] Saving bill for user ID " + ssoTicket.getUserId() + ", Bill ID: " + (bill.getBillId() != null ? bill.getBillId() : "New bill") + ", Amount: " + bill.getAmount());
                ssoTicket.setBillId(bill.getBillId());
                
                billingRepository.save(bill);
                ssoTicketRepository.save(ssoTicket);

                System.out.println("[SimulationApiController] Payment processed for SSO ticket ID " + ssoTicket.getTicket().getTicketId() + ", User ID: " + ssoTicket.getUserId() + ". Amount paid: " + price);
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Payment successful for SSO ticket ID: " + ssoTicket.getTicket().getTicketId() + ". Amount paid: " + price);

                return ResponseEntity.ok(response);
            } catch (NumberFormatException e) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid user ID format. Expected an integer.");
                return ResponseEntity.badRequest().body(error);
            }
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid ticket type. Expected 'GUEST' or 'SSO'."));
    }
    

    @Transactional
    @PostMapping("/exit")
    public ResponseEntity<?> exit(@RequestParam String userId, @RequestParam String licensePlate) {
        Map<String, Object> response = new HashMap<>();
        
        Integer ticketId = null;
        System.out.println("[SimulationApiController] Processing exit for userId: " + userId + ", licensePlate: " + licensePlate);
       
        // Find to see if there is a matching active ticket for this userId or license plate
        Optional<TicketView> ticketOpt = ticketViewRepository.findByHolderIdentifierAndFinishedFalse(String.valueOf(userId)).stream().findFirst();

        System.out.println("[SimulationApiController] Active ticket lookup for userId: " + userId + " found: " + (ticketOpt.isPresent() ? "Yes, Ticket ID: " + ticketOpt.get().getTicketId() : "No"));
        if (ticketOpt.isEmpty()){
            response.put("error", "Exception: No active ticket found for this card (User ID: " + userId + ").");
            Alert alert = new Alert();
            alert.setType(AlertType.SECURITY_BREACH);
            alert.setMessage("Exit attempted but no active ticket found for User ID: " + userId);
            alertRepository.save(alert);
            return ResponseEntity.badRequest().body(response);
        }

        // Check if license plate matches the ticket's license plate for added security
        if (!ticketOpt.get().getLicensePlate().equalsIgnoreCase(licensePlate)) {
            response.put("error", "Exception: License plate does not match the active ticket for this user. Provided: " + licensePlate + ", Expected: " + ticketOpt.get().getLicensePlate());
            Alert alert = new Alert();
            alert.setType(AlertType.SECURITY_BREACH);
            alert.setMessage("Exit attempted with mismatched license plate. User ID: " + userId + ", Provided Plate: " + licensePlate + ", Expected Plate: " + ticketOpt.get().getLicensePlate());
            alertRepository.save(alert);
            return ResponseEntity.badRequest().body(response);
        }


        Ticket ticket = ticketRepository.findById(ticketOpt.get().getTicketId()).orElseThrow(() -> new RuntimeException("Ticket not found for ID: " + ticketOpt.get().getTicketId()));
        System.out.println("[SimulationApiController] Found ticket for exit processing. Ticket ID: " + ticket.getTicketId() + ", Current finished status: " + ticket.getFinished());
        ticketRepository.updateExitTimeAndFinish(ticket.getTicketId(), java.time.OffsetDateTime.now());

        // Gate opens
        gate.open();

        response.put("success", true);
        response.put("message", "Vehicle exited successfully. Gate is opening. Database has handled checkout.");
        response.put("ticketId", ticketId);

        return ResponseEntity.ok(response);
    }
}
