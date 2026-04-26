package com.se.sebtl.controller;

import com.se.sebtl.exception.*;

import com.se.sebtl.model.*;
import com.se.sebtl.repository.*;
import com.se.sebtl.service.SecurityService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/operator")
@CrossOrigin(origins = "*")
public class OperatorController {

    private final TicketRepository ticketDb;
    private final ParkingSlotRepository slotDb;
    private final AlertRepository alertDb;
    private final AppUserRepository userDb; // Added for role verification
    private final SecurityService securityService;

    // SessionManager is removed here assuming you take the JWT advice, 
    // but the @RequestHeader ensures the request is tracked to an operator ID.
    public OperatorController(TicketRepository ticketDb, 
                              ParkingSlotRepository slotDb, 
                              AlertRepository alertDb,
                              AppUserRepository userDb,
                              SecurityService securityService) {
        this.ticketDb = ticketDb;
        this.slotDb = slotDb;
        this.alertDb = alertDb;
        this.userDb = userDb;
        this.securityService = securityService;
    }

    // --- PARKING SLOTS ---

    @GetMapping("/slots")
    public ResponseEntity<List<ParkingSlot>> getSlots(@RequestHeader("Authorization") String token) {
        securityService.verifyRole(token, Role.OPERATOR); // Verify the user is an operator
        // Returns the current map of all parking slots
        return ResponseEntity.ok(slotDb.findAllByOrderBySlotIdAsc());
    }

    // --- TICKET HISTORY ---

    @GetMapping("/history")
    public ResponseEntity<List<Ticket>> getHistory(@RequestHeader("Authorization") String token) {
        securityService.verifyRole(token, Role.OPERATOR); // Verify the user is an operator
        // Returns the ticket history chronologically (newest first)
        return ResponseEntity.ok(ticketDb.findAllByOrderByEntryTimeDesc());
    }

    @GetMapping("/history/search")
    public ResponseEntity<List<Ticket>> searchHistory(@RequestHeader("Authorization") String token, 
                                                      @RequestParam String query) {
        securityService.verifyRole(token, Role.OPERATOR); // Verify the user is an operator

        // Trim any accidental whitespace from the operator's input
        String term = query.trim();
        
        // If the search term contains ONLY numbers, it could be an ID or a numeric license plate
        if (term.matches("\\d+")) {
            int numericId = Integer.parseInt(term);
            return ResponseEntity.ok(
                ticketDb.findByLicensePlateContainingIgnoreCaseOrTicketIdOrUserIdOrderByEntryTimeDesc(
                    term, numericId, numericId)
            );
        } else {
            // If it contains letters, it can only be a license plate. 
            // (Passing text into an Integer ID query would crash the database)
            return ResponseEntity.ok(
                ticketDb.findByLicensePlateContainingIgnoreCaseOrderByEntryTimeDesc(term)
            );
        }
    }

    // --- ALERTS ---

    @GetMapping("/alerts/active")
    public ResponseEntity<List<Alert>> getActiveAlerts(@RequestHeader("Authorization") String token) {
        securityService.verifyRole(token, Role.OPERATOR); // Verify the user is an operator
        // Returns only active (unacknowledged) alerts, sorted by type and timestamp
        return ResponseEntity.ok(alertDb.findByAcknowledgedFalseOrderByTypeAscTimestampDesc());
    }

    @GetMapping("/alerts/history")
    public ResponseEntity<List<Alert>> getAlertHistory(@RequestHeader("Authorization") String token) {
        securityService.verifyRole(token, Role.OPERATOR); // Verify the user is an operator
        // Allows operators to view past resolved alerts
        return ResponseEntity.ok(alertDb.findByAcknowledgedTrueOrderByTimestampDesc()); 
    }

    @PatchMapping("/alerts/{alertId}/resolve")
    public ResponseEntity<MessageResponse> resolveAlert(@RequestHeader("Authorization") String token, 
                                          @PathVariable int alertId) {
        AppUser user = securityService.verifyRole(token, Role.OPERATOR); // Verify the user is an operator

        // Safely checks if the alert exists before modifying it.
        Optional<Alert> alertOpt = alertDb.findById(alertId);
        
        if (alertOpt.isPresent()) {
            Alert alert = alertOpt.get();
            alert.setAcknowledged(true);
            alertDb.save(alert);
            return ResponseEntity.ok(new MessageResponse("Alert " + alertId + " successfully resolved by Operator " + user.getId() + "."));
        } else {
            return ResponseEntity.notFound().build(); // Properly returns a 404 error
        }
    }

    // Helper method to simulate decoding a JWT token
    private int extractUserIdFromToken(String token) {
        return Integer.parseInt(token.replace("Bearer ", ""));
    }
}