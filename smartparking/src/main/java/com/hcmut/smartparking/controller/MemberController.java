package com.hcmut.smartparking.controller;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import com.hcmut.smartparking.database.*;
import com.hcmut.smartparking.session.SessionManager;
import com.hcmut.smartparking.model.Ticket;

@RestController
@RequestMapping("/member")
public class MemberController {

    private final SessionManager sessionManager;
    private final TicketDatabase ticketDb;

    @Autowired
    public MemberController(SessionManager sessionManager, TicketDatabase ticketDb) {
        this.sessionManager = sessionManager;
        this.ticketDb = ticketDb;
    }

    // ── UC-M1: Ticket history ─────────────────────────────
    @GetMapping("/history")
    public List<Ticket> getHistory(@RequestParam int userId) {
        if (!sessionManager.isActive(userId)) return Collections.emptyList();
        sessionManager.refreshSession(userId);

        List<Ticket> tickets = ticketDb.findAllByUserId(userId);
        tickets.sort(Comparator
            .comparing(Ticket::isFinished)
            .thenComparing(Comparator.comparing(Ticket::getEntryTime).reversed()));
        return tickets;
    }

    // ── UC-M2: BKPay redirect (session keepalive only) ────
    @PostMapping("/bkpay/keepalive")
    public void bkpayKeepalive(@RequestParam int userId) {
        if (!sessionManager.isActive(userId)) return;
        sessionManager.refreshSession(userId);
        // redirect logic lives entirely in member.html
    }
}