package com.se.sebtl.controller;

import com.se.sebtl.model.*;
import com.se.sebtl.repository.*;
import com.se.sebtl.service.SecurityService;
import com.se.sebtl.service.ParkingService;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/operator")
@CrossOrigin(origins = "*")
public class OperatorController {

    private final AlertRepository alertDb;
    private final SecurityService securityService;
    private final ParkingService parkingService;

    // SessionManager is removed here assuming you take the JWT advice, 
    // but the @RequestHeader ensures the request is tracked to an operator ID.
    public OperatorController(AlertRepository alertDb,
                              SecurityService securityService,
                              ParkingService parkingService) {
        this.alertDb = alertDb;
        this.securityService = securityService;
        this.parkingService = parkingService;   
    }

    // --- PARKING SLOTS ---

    @GetMapping("/slots")
    public ResponseEntity<List<ParkingSlot>> getSlots(@RequestHeader("Authorization") String token) {
        securityService.verifyRole(token, AppRole.OPERATOR); // Verify the user is an operator
        // Returns the current map of all parking slots
        return ResponseEntity.ok(parkingService.getAllSlots());
    }

    // --- TICKET HISTORY ---

    @GetMapping("/history")
    public ResponseEntity<List<TicketView>> getHistory(@RequestHeader("Authorization") String token) {
        securityService.verifyRole(token, AppRole.OPERATOR); // Verify the user is an operator
        // Returns the ticket history chronologically (newest first)
        return ResponseEntity.ok(parkingService.getAllTickets());
    }

    @GetMapping("/history/search")
    public ResponseEntity<List<TicketView>> searchHistory(@RequestHeader("Authorization") String token, 
                                                      @RequestParam String query) {
        securityService.verifyRole(token, AppRole.OPERATOR); // Verify the user is an operator

        return ResponseEntity.ok(parkingService.searchTickets(query));
    }

    // --- ALERTS ---

    @GetMapping("/alerts/active")
    public ResponseEntity<List<Alert>> getActiveAlerts(@RequestHeader("Authorization") String token) {
        securityService.verifyRole(token, AppRole.OPERATOR); // Verify the user is an operator
        // Returns only active (unacknowledged) alerts, sorted by type and timestamp
        return ResponseEntity.ok(alertDb.findByAcknowledgedFalseOrderByTypeAscTimestampDesc());
    }

    @GetMapping("/alerts/history")
    public ResponseEntity<List<Alert>> getAlertHistory(@RequestHeader("Authorization") String token) {
        securityService.verifyRole(token, AppRole.OPERATOR); // Verify the user is an operator
        // Allows operators to view past resolved alerts
        return ResponseEntity.ok(alertDb.findByAcknowledgedTrueOrderByTimestampDesc()); 
    }

    @PatchMapping("/alerts/{alertId}/resolve")
    public ResponseEntity<MessageResponse> resolveAlert(@RequestHeader("Authorization") String token, 
                                          @PathVariable int alertId) {
        AppUser user = securityService.verifyRole(token, AppRole.OPERATOR); // Verify the user is an operator

        // Safely checks if the alert exists before modifying it.
        Optional<Alert> alertOpt = alertDb.findById(alertId);
        
        if (alertOpt.isPresent()) {
            Alert alert = alertOpt.get();
            alert.setAcknowledged(true);
            alertDb.save(alert);
            return ResponseEntity.ok(new MessageResponse("Alert " + alertId + " successfully resolved by Operator " + user.getUserId() + "."));
        } else {
            return ResponseEntity.notFound().build(); // Properly returns a 404 error
        }
    }
}