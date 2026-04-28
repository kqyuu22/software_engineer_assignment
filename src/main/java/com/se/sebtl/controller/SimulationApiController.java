package com.se.sebtl.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.se.sebtl.service.iot.HardwareSimulatorService;
import com.se.sebtl.service.IoTManagerService;
import com.se.sebtl.model.Role;
import com.se.sebtl.model.SsoTicket;
import com.se.sebtl.model.GuestTicket;
import com.se.sebtl.model.Price;
import com.se.sebtl.model.Alert;
import com.se.sebtl.model.AlertType;
import com.se.sebtl.repository.SsoTicketRepository;
import com.se.sebtl.repository.GuestTicketRepository;
import com.se.sebtl.repository.UnimemberRepository;
import com.se.sebtl.repository.PriceRepository;
import com.se.sebtl.repository.BillingRepository;
import com.se.sebtl.repository.AlertRepository;
import com.se.sebtl.service.iot.Gate;
import com.se.sebtl.service.iot.Camera;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/simulation")
public class SimulationApiController {

    private final HardwareSimulatorService hardwareSimulator;
    private final IoTManagerService iotManager;
    private final SsoTicketRepository ssoTicketRepository;
    private final GuestTicketRepository guestTicketRepository;
    private final UnimemberRepository unimemberRepository;
    private final PriceRepository priceRepository;
    private final BillingRepository billingRepository;
    private final AlertRepository alertRepository;
    private final Gate gate;
    private final Camera camera;

    public SimulationApiController(
            HardwareSimulatorService hardwareSimulator, 
            IoTManagerService iotManager,
            SsoTicketRepository ssoTicketRepository,
            GuestTicketRepository guestTicketRepository,
            UnimemberRepository unimemberRepository,
            PriceRepository priceRepository,
            BillingRepository billingRepository,
            AlertRepository alertRepository,
            Gate gate,
            Camera camera) {
        this.hardwareSimulator = hardwareSimulator;
        this.iotManager = iotManager;
        this.ssoTicketRepository = ssoTicketRepository;
        this.guestTicketRepository = guestTicketRepository;
        this.unimemberRepository = unimemberRepository;
        this.priceRepository = priceRepository;
        this.billingRepository = billingRepository;
        this.alertRepository = alertRepository;
        this.gate = gate;
        this.camera = camera;
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
                map.put("userId", -1);
                map.put("licensePlate", t.getLicensePlate());
                active.add(map);
            });
            
        return ResponseEntity.ok(active);
    }

    @PostMapping("/entrance")
    public ResponseEntity<?> entrance(@RequestParam int userId, @RequestParam(required = false) String existingPlate) {
        Map<String, Object> response = new HashMap<>();
        
        // Mock IoT Camera capture
        String licensePlate;
        if (existingPlate != null && !existingPlate.isBlank()) {
            licensePlate = existingPlate;
        } else {
            licensePlate = camera.capture();
        }
        
        if (licensePlate == null) {
            response.put("error", "Exception: Camera failed to capture license plate.");
            Alert alert = new Alert();
            alert.setType(AlertType.SYSTEM_FAILURE);
            alert.setMessage("Camera failed to capture license plate at entrance.");
            alertRepository.save(alert);
            return ResponseEntity.badRequest().body(response);
        }

        // Duplicate plate check to simulate error
        if (ssoTicketRepository.existsByLicensePlateAndFinishedFalse(licensePlate) || 
            guestTicketRepository.findByLicensePlateAndFinishedFalse(licensePlate).isPresent()) {
            response.put("error", "Exception: Plate already exists in lot: " + licensePlate);
            Alert alert = new Alert();
            alert.setType(AlertType.SECURITY_BREACH);
            alert.setMessage("Duplicate license plate detected at entrance: " + licensePlate);
            alertRepository.save(alert);
            return ResponseEntity.badRequest().body(response);
        }

        // 1. Look up the role for the user
        Role role = unimemberRepository.findRoleByUserId(userId).orElse(Role.OTHER);
        
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
        java.math.BigDecimal currentPrice = priceRepository.findById(1).map(Price::getPrice).orElse(java.math.BigDecimal.valueOf(5000.00));
        
        Integer ticketId;
        if (userId > 0 && unimemberRepository.existsById(userId)) {
            SsoTicket ticket = new SsoTicket(userId, licensePlate, spotId, currentPrice);
            ssoTicketRepository.save(ticket);
            ticketId = ticket.getTicketId();
        } else {
            GuestTicket ticket = new GuestTicket(licensePlate, spotId, currentPrice);
            guestTicketRepository.save(ticket);
            ticketId = ticket.getTicketId();
        }
        
        // 4. Gate opens
        gate.open();

        response.put("success", true);
        response.put("message", "Vehicle entered successfully. Gate is opening. Plate scanned: " + licensePlate + ", Assigned spot: " + spotId);
        response.put("ticketId", ticketId);
        response.put("licensePlate", licensePlate);
        
        return ResponseEntity.ok(response);
    }

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
            if (!ticket.getLicensePlate().equalsIgnoreCase(licensePlate)) {
                response.put("error", "Exception: Registered plate mismatch. Expected " + ticket.getLicensePlate() + " but scanned " + licensePlate);
                Alert alert = new Alert();
                alert.setType(AlertType.SECURITY_BREACH);
                alert.setMessage("Plate mismatch at exit. User ID " + userId + " expected " + ticket.getLicensePlate() + " but scanned " + licensePlate);
                alertRepository.save(alert);
                return ResponseEntity.badRequest().body(response);
            }
            ssoTicketRepository.updateExitTime(ticket.getTicketId(), java.time.OffsetDateTime.now());
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
            guestTicketRepository.updateExitTime(ticket.getTicketId(), java.time.OffsetDateTime.now());
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
