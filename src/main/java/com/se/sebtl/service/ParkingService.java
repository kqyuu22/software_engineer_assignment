package com.se.sebtl.service;

import com.se.sebtl.model.*;
import com.se.sebtl.repository.*;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ParkingService {
    private final TicketRepository ticketDb;
    private final ParkingSlotRepository slotDb;
    private final PriceRepository priceDb;

    public ParkingService(TicketRepository ticketDb, ParkingSlotRepository slotDb, PriceRepository priceDb) {
        this.ticketDb = ticketDb;
        this.slotDb = slotDb;
        this.priceDb = priceDb;
    }

    public List<Ticket> getAllTickets() {
        return ticketDb.findAllByOrderByEntryTimeDesc();
    }

    public List<ParkingSlot> getAllSlots() {
        return slotDb.findAllByOrderBySlotIdAsc();
    }
    
    public double getCurrentPrice() {
        return priceDb.findById(1).map(Price::getPrice).orElse(5000.0);
    }

    public List<Ticket> searchTickets(String query) {
        String term = query.trim();
        
        // If the search term contains ONLY numbers, it could be an ID or a numeric license plate
        if (term.matches("\\d+")) {
            int numericId = Integer.parseInt(term);
            return ticketDb.findByLicensePlateContainingIgnoreCaseOrTicketIdOrUserIdOrderByEntryTimeDesc(
                    term, numericId, numericId);
        } else {
            // If it contains letters, it can only be a license plate. 
            // (Passing text into an Integer ID query would crash the database)
            return ticketDb.findByLicensePlateContainingIgnoreCaseOrderByEntryTimeDesc(term);
        }
    }
}
