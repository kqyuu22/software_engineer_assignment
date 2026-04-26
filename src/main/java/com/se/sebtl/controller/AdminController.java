package com.se.sebtl.controller;

import com.se.sebtl.model.*;
import com.se.sebtl.repository.*;
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

    public AdminController(TicketRepository ticketDb, ParkingSlotRepository slotDb, PriceRepository priceDb) {
        this.ticketDb = ticketDb;
        this.slotDb = slotDb;
        this.priceDb = priceDb;
    }

    @GetMapping("/history")
    public ResponseEntity<List<Ticket>> getHistory(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(ticketDb.findAllByOrderByEntryTimeDesc());
    }

    @GetMapping("/history/search")
    public ResponseEntity<List<Ticket>> searchHistory(@RequestHeader("Authorization") String token, 
                                                      @RequestParam String plate) {
        return ResponseEntity.ok(ticketDb.findByLicensePlateContainingIgnoreCaseOrderByEntryTimeDesc(plate));
    }

    // --- SLOT MANAGEMENT ---

    @GetMapping("/slots")
    public ResponseEntity<List<ParkingSlot>> getAllSlots(@RequestHeader("Authorization") String token) {
        // verifyAdminRole(token); // (Uncomment if you added the security helper)
        
        // It's best to return these sorted by ID so the table stays neat
        return ResponseEntity.ok(slotDb.findAllByOrderBySlotIdAsc()); 
    }

    @PostMapping("/slots")
    public ResponseEntity<?> addSlot(@RequestHeader("Authorization") String token, 
                                     @RequestParam Role priority) {
        ParkingSlot newSlot = new ParkingSlot();
        newSlot.setPriority(priority);
        newSlot.setStatus(SlotStatus.AVAILABLE);
        slotDb.save(newSlot);
        return ResponseEntity.ok("{\"message\": \"Slot successfully added\"}");
    }

    @DeleteMapping("/slots/{slotId}")
    public ResponseEntity<?> removeSlot(@RequestHeader("Authorization") String token, 
                                        @PathVariable int slotId) {
        slotDb.deleteById(slotId);
        return ResponseEntity.ok("{\"message\": \"Slot successfully removed\"}");
    }

    @PatchMapping("/tickets/{ticketId}/force-close")
    public ResponseEntity<?> forceCloseTicket(@RequestHeader("Authorization") String token, 
                                              @PathVariable int ticketId) {
        return ticketDb.findById(ticketId).map(ticket -> {
            if (ticket.getFinished()) {
                return ResponseEntity.badRequest().body("{\"message\": \"Ticket is already closed.\"}");
            }
            ticket.finish(); // Stamps exitTime and sets finished to true
            ticketDb.save(ticket);
            return ResponseEntity.ok("{\"message\": \"Ticket " + ticketId + " forced closed.\"}");
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/tickets/{ticketId}/waive")
    public ResponseEntity<?> waiveTicketFee(@RequestHeader("Authorization") String token, 
                                            @PathVariable int ticketId) {
        return ticketDb.findById(ticketId).map(ticket -> {
            ticket.setFee(0.0);
            ticketDb.save(ticket);
            return ResponseEntity.ok("{\"message\": \"Fee waived for ticket " + ticketId + ".\"}");
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/tickets/{ticketId}")
    public ResponseEntity<?> deleteTicket(@RequestHeader("Authorization") String token, 
                                          @PathVariable int ticketId) {
        if (!ticketDb.existsById(ticketId)) {
            return ResponseEntity.notFound().build();
        }
        ticketDb.deleteById(ticketId);
        return ResponseEntity.ok("{\"message\": \"Ticket " + ticketId + " permanently deleted.\"}");
    }

    
    @PatchMapping("/slots/bulk")
    public ResponseEntity<?> updateBulkSlots(@RequestHeader("Authorization") String token, 
                                             @RequestBody BulkSlotRequest request) {
        
        List<ParkingSlot> slots = slotDb.findAllById(request.getSlotIds());
        
        if (slots.isEmpty()) {
            return ResponseEntity.badRequest().body("{\"message\": \"No valid slots found.\"}");
        }

        slots.forEach(slot -> slot.setPriority(request.getPriority()));
        slotDb.saveAll(slots);
        
        return ResponseEntity.ok("{\"message\": \"Successfully updated " + slots.size() + " slots.\"}");
    }

    @GetMapping("/price")
    public ResponseEntity<Double> getPrice(@RequestHeader("Authorization") String token) {
        Price price = priceDb.findById(1).orElse(null);
        return ResponseEntity.ok(price != null ? price.getPrice() : 5000.0); // Default price if not set
    }

    @PutMapping("/price")
    public ResponseEntity<?> setPrice(@RequestHeader("Authorization") String token, 
                                      @RequestParam double newPrice) {
        Price price = priceDb.findById(1).orElse(null);
        if (price != null) {
            price.setPrice(newPrice);
            priceDb.save(price);
        } else {
            Price newPriceEntry = new Price();
            newPriceEntry.setPrice(newPrice);
            priceDb.save(newPriceEntry);
        }
        return ResponseEntity.ok("{\"message\": \"Global price updated to " + newPrice + "VND\"}");
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