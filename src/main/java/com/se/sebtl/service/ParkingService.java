package com.se.sebtl.service;

import com.se.sebtl.model.*;
import com.se.sebtl.repository.*;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ParkingService {
    private final TicketViewRepository ticketViewDb;
    private final ParkingSlotRepository slotDb;
    private final PriceRepository priceDb;

    public ParkingService(TicketViewRepository ticketViewDb, ParkingSlotRepository slotDb, PriceRepository priceDb) {
        this.ticketViewDb = ticketViewDb;
        this.slotDb = slotDb;
        this.priceDb = priceDb;
    }

    public List<TicketView> getAllTickets() {
        return ticketViewDb.findAllByOrderByEntryTimeDesc();
    }

    public List<ParkingSlot> getAllSlots() {
        return slotDb.findAllByOrderBySlotIdAsc();
    }
    
    public java.math.BigDecimal getCurrentPrice() {
        return priceDb.findById(1).map(Price::getPrice).orElse(java.math.BigDecimal.valueOf(5000.00));
    }

    public List<TicketView> searchTickets(String query) {
        String term = query.trim();
        
        // If the search term contains ONLY numbers, it could be an ID or a numeric license plate
        if (term.matches("\\d+")) {
            return ticketViewDb.findByTicketIdOrLicensePlateContainingIgnoreCaseOrHolderIdentifierContainingIgnoreCaseOrderByEntryTimeDesc(
                    Integer.parseInt(term), term, term);
        } else {
            // If it contains letters, it can be a license plate or a User ID (like "GUEST-02").
            return ticketViewDb.findByLicensePlateContainingIgnoreCaseOrHolderIdentifierContainingIgnoreCaseOrderByEntryTimeDesc(term, term);
        }
    }
}
