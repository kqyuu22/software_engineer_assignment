package com.hcmut.smartparking.controller;

import java.util.*;
import com.hcmut.smartparking.database.TicketDatabase;
import com.hcmut.smartparking.database.ParkingSlotDatabase;
import com.hcmut.smartparking.session.SessionManager;
import com.hcmut.smartparking.model.Ticket;
import com.hcmut.smartparking.model.ParkingSlot;

public abstract class BaseController {

    protected final SessionManager sessionManager;
    protected final TicketDatabase ticketDb;
    protected final ParkingSlotDatabase slotDb;

    protected BaseController(SessionManager sessionManager, TicketDatabase ticketDb,
                             ParkingSlotDatabase slotDb) {
        this.sessionManager = sessionManager;
        this.ticketDb = ticketDb;
        this.slotDb = slotDb;
    }

    protected boolean checkSession(int userId) {
        if (!sessionManager.isActive(userId)) return false;
        sessionManager.refreshSession(userId);
        return true;
    }

    protected List<Ticket> fetchHistory() {
        List<Ticket> tickets = ticketDb.findAll();
        tickets.sort(Comparator
            .comparing(Ticket::isFinished)
            .thenComparing(Comparator.comparing(Ticket::getEntryTime).reversed()));
        return tickets;
    }

    protected List<Ticket> fetchHistoryByUser(int searchTerm) {
        if (searchTerm != 0) return ticketDb.findAllByUserId(searchTerm);
        return fetchHistory();
    }

    protected List<ParkingSlot> fetchSlots() {
        return slotDb.findAll();
    }
}