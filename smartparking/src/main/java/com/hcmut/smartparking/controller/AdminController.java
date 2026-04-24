package com.hcmut.smartparking.controller;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import com.hcmut.smartparking.database.*;
import com.hcmut.smartparking.session.SessionManager;
import com.hcmut.smartparking.enums.Roles;
import com.hcmut.smartparking.model.ParkingSlot;
import com.hcmut.smartparking.model.Ticket;

@RestController
@RequestMapping("/admin")
public class AdminController extends BaseController {

    private final PriceDatabase priceDb;

    @Autowired
    public AdminController(SessionManager sessionManager, TicketDatabase ticketDb,
                           ParkingSlotDatabase slotDb, PriceDatabase priceDb) {
        super(sessionManager, ticketDb, slotDb);
        this.priceDb = priceDb;
    }

    @GetMapping("/history")
    public List<Ticket> getHistory(@RequestParam int userId) {
        if (!checkSession(userId)) return Collections.emptyList();
        return fetchHistory();
    }

    @GetMapping("/history/search")
    public List<Ticket> searchHistory(@RequestParam int userId,
                                      @RequestParam int searchTerm) {
        if (!checkSession(userId)) return Collections.emptyList();
        return fetchHistoryByUser(searchTerm);
    }

    @GetMapping("/price")
    public double getPrice(@RequestParam int userId) {
        if (!checkSession(userId)) return -1;
        return priceDb.getPrice();
    }

    @PostMapping("/price")
    public void setPrice(@RequestParam int userId, @RequestParam double newPrice) {
        if (!checkSession(userId)) return;
        if (newPrice <= 0) return;
        priceDb.setPrice(newPrice);
    }

    @GetMapping("/slots")
    public List<ParkingSlot> getSlots(@RequestParam int userId) {
        if (!checkSession(userId)) return Collections.emptyList();
        return fetchSlots();
    }

    @PostMapping("/slots/priority/bulk")
    public void setBulkPriority(@RequestParam int userId,
                                @RequestBody List<Integer> slotIds,
                                @RequestParam Roles priority) {
        if (!checkSession(userId)) return;
        for (int slotId : slotIds) {
            slotDb.updatePriority(slotId, priority);
        }
    }
}