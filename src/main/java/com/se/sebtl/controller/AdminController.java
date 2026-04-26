package com.se.sebtl.controller;

import com.se.sebtl.model.*;
import com.se.sebtl.repository.*;
import com.se.sebtl.service.SecurityService;
import com.se.sebtl.model.MessageResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final TicketRepository ticketDb;
    private final ParkingSlotRepository slotDb;
    private final PriceRepository priceDb;
    private final SecurityService securityService;

    public AdminController(TicketRepository ticketDb, ParkingSlotRepository slotDb, PriceRepository priceDb, SecurityService securityService) {
        this.ticketDb = ticketDb;
        this.slotDb = slotDb;
        this.priceDb = priceDb;
        this.securityService = securityService;
    }

    // --- TICKET MANAGEMENT ---

    @GetMapping("/history")
    public ResponseEntity<List<Ticket>> getHistory(@RequestHeader("Authorization") String token) {
        securityService.verifyRole(token, Role.ADMIN);
        return ResponseEntity.ok(ticketDb.findAllByOrderByEntryTimeDesc());
    }

    @GetMapping("/history/search")
    public ResponseEntity<List<Ticket>> searchHistory(@RequestHeader("Authorization") String token, 
                                                      @RequestParam String plate) {
        securityService.verifyRole(token, Role.ADMIN);
        return ResponseEntity.ok(ticketDb.findByLicensePlateContainingIgnoreCaseOrderByEntryTimeDesc(plate));
    }

    @PatchMapping("/tickets/{ticketId}/waive")
    public ResponseEntity<MessageResponse> waiveTicketFee(@RequestHeader("Authorization") String token, 
                                            @PathVariable int ticketId) {
        securityService.verifyRole(token, Role.ADMIN);
        return ticketDb.findById(ticketId).map(ticket -> {
            ticket.setFee(0.0);
            ticketDb.save(ticket);
            return ResponseEntity.ok(new MessageResponse("Fee waived for ticket " + ticketId + "."));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/tickets/{ticketId}")
    public ResponseEntity<MessageResponse> deleteTicket(@RequestHeader("Authorization") String token, 
                                          @PathVariable int ticketId) {
        securityService.verifyRole(token, Role.ADMIN);
        if (!ticketDb.existsById(ticketId)) {
            return ResponseEntity.notFound().build();
        }
        ticketDb.deleteById(ticketId);
        return ResponseEntity.ok(new MessageResponse("Ticket " + ticketId + " permanently deleted."));
    }

    @PatchMapping("/tickets/{ticketId}/force-close")
    public ResponseEntity<MessageResponse> forceCloseTicket(@RequestHeader("Authorization") String token, 
                                              @PathVariable int ticketId) {
        securityService.verifyRole(token, Role.ADMIN);
        return ticketDb.findById(ticketId).map(ticket -> {
            if (ticket.getFinished()) {
                return ResponseEntity.badRequest().body(new MessageResponse("Ticket is already closed."));
            }
            ticket.finish(); // Stamps exitTime and sets finished to true
            ticketDb.save(ticket);
            return ResponseEntity.ok(new MessageResponse("Ticket " + ticketId + " forced closed."));
        }).orElse(ResponseEntity.notFound().build());
    }

    // --- SLOT MANAGEMENT ---

    @GetMapping("/slots")
    public ResponseEntity<List<ParkingSlot>> getAllSlots(@RequestHeader("Authorization") String token) {
        securityService.verifyRole(token, Role.ADMIN);
        
        return ResponseEntity.ok(slotDb.findAllByOrderBySlotIdAsc()); 
    }

    @PostMapping("/slots")
    public ResponseEntity<MessageResponse> addSlot(@RequestHeader("Authorization") String token, 
                                     @RequestParam Role priority) {
        securityService.verifyRole(token, Role.ADMIN);
        ParkingSlot newSlot = new ParkingSlot();
        newSlot.setPriority(priority);
        newSlot.setStatus(SlotStatus.AVAILABLE);
        slotDb.save(newSlot);
        return ResponseEntity.ok(new MessageResponse("Slot successfully added"));
    }

    @DeleteMapping("/slots/{slotId}")
    public ResponseEntity<MessageResponse> removeSlot(@RequestHeader("Authorization") String token, 
                                        @PathVariable int slotId) {
        securityService.verifyRole(token, Role.ADMIN);
        if (!slotDb.existsById(slotId)) {
            return ResponseEntity.notFound().build();
        }
        slotDb.deleteById(slotId);
        return ResponseEntity.ok(new MessageResponse("Slot successfully removed"));
    }
    
    @PatchMapping("/slots/bulk")
    public ResponseEntity<MessageResponse> updateBulkSlots(@RequestHeader("Authorization") String token, 
                                             @RequestBody BulkSlotRequest request) {
        securityService.verifyRole(token, Role.ADMIN);
        List<ParkingSlot> slots = slotDb.findAllById(request.getSlotIds());
        
        if (slots.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("No valid slots found."));
        }

        slots.forEach(slot -> slot.setPriority(request.getPriority()));
        slotDb.saveAll(slots);
        
        return ResponseEntity.ok(new MessageResponse("Successfully updated " + slots.size() + " slots."));
    }

    // --- PRICE MANAGEMENT ---

    @GetMapping("/price")
    public ResponseEntity<Double> getPrice(@RequestHeader("Authorization") String token) {
        securityService.verifyRole(token, Role.ADMIN);
        Price price = priceDb.findById(1).orElse(null);
        return ResponseEntity.ok(price != null ? price.getPrice() : 5000.0); // Default price if not set
    }

    @PutMapping("/price")
    public ResponseEntity<MessageResponse> setPrice(@RequestHeader("Authorization") String token, 
                                      @RequestParam double newPrice) {
        securityService.verifyRole(token, Role.ADMIN);
        Price price = priceDb.findById(1).orElse(null);
        if (price != null) {
            price.setPrice(newPrice);
            priceDb.save(price);
        } else {
            Price newPriceEntry = new Price();
            newPriceEntry.setPrice(newPrice);
            priceDb.save(newPriceEntry);
        }
        return ResponseEntity.ok(new MessageResponse("Global price updated to " + newPrice + "VND"));
    }

    


    // DTO to capture the JSON body for bulk slot updates
    public static class BulkSlotRequest {
        private List<Integer> slotIds;
        private Role priority;
        
        public List<Integer> getSlotIds() { return slotIds; }
        public void setSlotIds(List<Integer> slotIds) { this.slotIds = slotIds; }
        public Role getPriority() { return priority; }
        public void setPriority(Role priority) { this.priority = priority; }
    }
}