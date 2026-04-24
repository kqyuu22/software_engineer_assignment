package com.hcmut.smartparking.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import com.hcmut.smartparking.database.*;
import com.hcmut.smartparking.session.SessionManager;
import com.hcmut.smartparking.model.*;

@RestController
@RequestMapping("/operator")
public class OperatorController extends BaseController {

    private final AlertDatabase alertDb;

    @Autowired
    public OperatorController(SessionManager sessionManager, TicketDatabase ticketDb,
                              ParkingSlotDatabase slotDb, AlertDatabase alertDb) {
        super(sessionManager, ticketDb, slotDb);
        this.alertDb = alertDb;
    }

    @GetMapping("/slots")
    public List<ParkingSlot> getSlots(@RequestParam int userId) {
        if (!checkSession(userId)) return Collections.emptyList();
        return fetchSlots();
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

    @GetMapping("/alerts")
    public List<Alert> getAlerts(@RequestParam int userId) {
        if (!checkSession(userId)) return Collections.emptyList();
        List<Alert> alerts = alertDb.findUnacknowledged();
        alerts.sort(Comparator.comparing(Alert::getType));
        return alerts;
    }

    @PostMapping("/alerts/resolve")
    public void resolveAlert(@RequestParam int userId, @RequestParam int alertId) {
        if (!checkSession(userId)) return;
        alertDb.acknowledge(alertId);
    }
}