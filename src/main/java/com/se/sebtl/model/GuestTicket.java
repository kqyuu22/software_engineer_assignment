package com.se.sebtl.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;

@Entity
@Table(name = "guest_tickets")
public class GuestTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guest_ticket_id")
    private Integer guestTicketId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    @JsonIgnore
    private Ticket ticket;

    @Column(name = "final_calculated_fee")
    private BigDecimal finalCalculatedFee;

    @Column(name = "paid_directly")
    private Boolean paidDirectly;

    public GuestTicket() {}

    public GuestTicket(Ticket ticket) {
        this.ticket = ticket;
        this.paidDirectly = false;
    }

    public Integer getGuestTicketId() { return guestTicketId; }
    public void setGuestTicketId(Integer guestTicketId) { this.guestTicketId = guestTicketId; }

    public Ticket getTicket() { return ticket; }
    public void setTicket(Ticket ticket) { this.ticket = ticket; }

    public BigDecimal getFinalCalculatedFee() { return finalCalculatedFee; }
    public void setFinalCalculatedFee(BigDecimal finalCalculatedFee) { this.finalCalculatedFee = finalCalculatedFee; }

    public Boolean getPaidDirectly() { return paidDirectly; }
    public void setPaidDirectly(Boolean paidDirectly) { this.paidDirectly = paidDirectly; }

    @Transient
    public Integer getTicketId() { return ticket != null ? ticket.getTicketId() : null; }

    @Transient
    public java.time.OffsetDateTime getEntryTime() { return ticket != null ? ticket.getEntryTime() : null; }

    @Transient
    public java.time.OffsetDateTime getExitTime() { return ticket != null ? ticket.getExitTime() : null; }

    @Transient
    public String getLicensePlate() { return ticket != null ? ticket.getLicensePlate() : null; }

    @Transient
    public Integer getParkingSpot() { return ticket != null ? ticket.getParkingSpot() : null; }

    @Transient
    public Boolean getFinished() { return ticket != null ? ticket.getFinished() : null; }

    @Transient
    public BigDecimal getPrice() { return ticket != null ? ticket.getPrice() : null; }
}
