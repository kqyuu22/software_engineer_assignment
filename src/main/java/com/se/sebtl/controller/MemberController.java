package com.se.sebtl.controller;

import com.se.sebtl.model.Payment;
import com.se.sebtl.model.Ticket;
import com.se.sebtl.model.MessageResponse;
import com.se.sebtl.repository.TicketRepository;
import com.se.sebtl.repository.PaymentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/member")
@CrossOrigin(origins = "*")
public class MemberController {

    private final TicketRepository ticketDb;
    private final PaymentRepository paymentDb;

    public MemberController(TicketRepository ticketDb, PaymentRepository paymentDb) {
        this.ticketDb = ticketDb;
        this.paymentDb = paymentDb;
    }

    @GetMapping("/history")
    public ResponseEntity<List<Ticket>> getHistory(@RequestHeader("Authorization") String token) {
        int userId = extractUserIdFromToken(token);
        
        return ResponseEntity.ok(ticketDb.findByUserIdOrderByEntryTimeDesc(userId));
    }

    // --- PAYMENTS ---
    @GetMapping("/payment")
    public ResponseEntity<List<Payment>> getPayments(@RequestHeader("Authorization") String token) {
        int userId = extractUserIdFromToken(token);
        return ResponseEntity.ok(paymentDb.findByUserIdOrderByTimestampDesc(userId));
    }

    @PostMapping("/payment/{paymentId}")
    public ResponseEntity<MessageResponse> payFee(@RequestHeader("Authorization") String token, @PathVariable Integer paymentId) {
        int userId = extractUserIdFromToken(token);
        
        Optional<Payment> paymentOpt = paymentDb.findById(paymentId);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            // Make sure this payment actually belongs to the user trying to pay it
            if (!payment.getUserId().equals(userId)) {
                return ResponseEntity.status(403).body(new MessageResponse("Forbidden"));
            }
            
            payment.setStatus("PAID"); // Change status
            payment.setTimestamp(java.time.LocalDateTime.now()); // Update timestamp to now
            paymentDb.save(payment);
            return ResponseEntity.ok(new MessageResponse("Payment successful."));
        }
        return ResponseEntity.notFound().build();
    }

    // Helper method to simulate decoding a JWT token
    private int extractUserIdFromToken(String token) {
        return Integer.parseInt(token.replace("Bearer ", ""));
    }
}