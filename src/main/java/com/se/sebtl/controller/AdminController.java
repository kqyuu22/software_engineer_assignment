package com.se.sebtl.controller;

import com.se.sebtl.model.*;
import com.se.sebtl.repository.*;
import com.se.sebtl.service.SecurityService;
import com.se.sebtl.service.ParkingService;
import com.se.sebtl.model.MessageResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final ParkingSlotRepository slotDb;
    private final PriceRepository priceDb;
    private final SecurityService securityService;
    private final ParkingService parkingService;

    public AdminController(ParkingSlotRepository slotDb, PriceRepository priceDb, SecurityService securityService, ParkingService parkingService) {
        this.slotDb = slotDb;
        this.priceDb = priceDb;
        this.securityService = securityService;
        this.parkingService = parkingService;
    }

    // --- TICKET MANAGEMENT ---

    @GetMapping("/history")
    public ResponseEntity<List<Ticket>> getHistory(@RequestHeader("Authorization") String token) {
        securityService.verifyRole(token, AppRole.ADMIN);
        return ResponseEntity.ok(parkingService.getAllTickets());
    }

    @GetMapping("/history/search")
    public ResponseEntity<List<Ticket>> searchHistory(@RequestHeader("Authorization") String token, 
                                                      @RequestParam String query) {
        securityService.verifyRole(token, AppRole.ADMIN);
        return ResponseEntity.ok(parkingService.searchTickets(query));
    }
    // --- SLOT MANAGEMENT ---

    @GetMapping("/slots")
    public ResponseEntity<List<ParkingSlot>> getAllSlots(@RequestHeader("Authorization") String token) {
        securityService.verifyRole(token, AppRole.ADMIN);
        
        return ResponseEntity.ok(parkingService.getAllSlots()); 
    }

    @PostMapping("/slots")
    public ResponseEntity<MessageResponse> addSlot(@RequestHeader("Authorization") String token, 
                                     @RequestParam Role priority) {
        securityService.verifyRole(token, AppRole.ADMIN);
        ParkingSlot newSlot = new ParkingSlot();
        newSlot.setPriority(priority);
        newSlot.setStatus(SlotStatus.AVAILABLE);
        slotDb.save(newSlot);
        return ResponseEntity.ok(new MessageResponse("Slot successfully added"));
    }

    @DeleteMapping("/slots/{slotId}")
    public ResponseEntity<MessageResponse> removeSlot(@RequestHeader("Authorization") String token, 
                                        @PathVariable int slotId) {
        securityService.verifyRole(token, AppRole.ADMIN);
        if (!slotDb.existsById(slotId)) {
            return ResponseEntity.notFound().build();
        }
        slotDb.deleteById(slotId);
        return ResponseEntity.ok(new MessageResponse("Slot successfully removed"));
    }
    
    @PatchMapping("/slots/bulk")
    public ResponseEntity<MessageResponse> updateBulkSlots(@RequestHeader("Authorization") String token, 
                                             @RequestBody BulkSlotRequest request) {
        securityService.verifyRole(token, AppRole.ADMIN);
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
        securityService.verifyRole(token, AppRole.ADMIN);
        Price price = priceDb.findById(1).orElse(null);
        return ResponseEntity.ok(price != null ? price.getPrice() : 5000.0); // Default price if not set
    }

    @PutMapping("/price")
    public ResponseEntity<MessageResponse> setPrice(@RequestHeader("Authorization") String token, 
                                      @RequestParam double newPrice) {
        securityService.verifyRole(token, AppRole.ADMIN);
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