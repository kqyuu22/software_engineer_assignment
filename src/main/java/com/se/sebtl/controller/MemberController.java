package com.se.sebtl.controller;

import com.se.sebtl.model.Billing;
import com.se.sebtl.model.SsoTicket;
import com.se.sebtl.model.MessageResponse;
import com.se.sebtl.service.SecurityService;
import com.se.sebtl.repository.SsoTicketRepository;
import com.se.sebtl.repository.BillingRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/member")
@CrossOrigin(origins = "*")
public class MemberController {

    private final SsoTicketRepository ticketDb;
    private final BillingRepository billingDb;
    private final SecurityService securityService;

    public MemberController(SsoTicketRepository ticketDb, BillingRepository billingDb, SecurityService securityService) {
        this.ticketDb = ticketDb;
        this.billingDb = billingDb;
        this.securityService = securityService;
    }

    @GetMapping("/history")
    public ResponseEntity<List<SsoTicket>> getHistory(@RequestHeader("Authorization") String token) {
        int userId = securityService.getUserIdFromToken(token);
        
        return ResponseEntity.ok(ticketDb.findByUserIdOrderByEntryTimeDesc(userId));
    }

    // --- PAYMENTS ---
    @GetMapping("/payment")
    public ResponseEntity<List<Billing>> getPayments(@RequestHeader("Authorization") String token) {
        int userId = securityService.getUserIdFromToken(token);
        return ResponseEntity.ok(billingDb.findByUserIdOrderByLastUpdatedDesc(userId));
    }

    @PostMapping("/payment/{billId}")
    public ResponseEntity<MessageResponse> payFee(@RequestHeader("Authorization") String token, @PathVariable Long billId) {
        int userId = securityService.getUserIdFromToken(token);
        
        Optional<Billing> billOpt = billingDb.findById(billId);
        if (billOpt.isPresent()) {
            Billing bill = billOpt.get();
            // Make sure this payment actually belongs to the user trying to pay it
            if (!bill.getUserId().equals(userId)) {
                return ResponseEntity.status(403).body(new MessageResponse("Forbidden"));
            }
            
            bill.setStatus(com.se.sebtl.model.BillStatus.PAID); // Change status
            bill.setLastUpdated(java.time.OffsetDateTime.now()); // Update timestamp to now
            billingDb.save(bill);
            return ResponseEntity.ok(new MessageResponse("Payment successful."));
        }
        return ResponseEntity.notFound().build();
    }
}