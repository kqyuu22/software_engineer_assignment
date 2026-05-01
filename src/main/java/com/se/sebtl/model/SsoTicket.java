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

@Entity
@Table(name = "sso_tickets")
public class SsoTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sso_ticket_id")
    private Integer ssoTicketId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    @JsonIgnore
    private Ticket ticket;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "bill_id")
    private Long billId;

    public SsoTicket() {}

    public SsoTicket(Integer userId, Ticket ticket) {
        this.userId = userId;
        this.ticket = ticket;
    }

    public Integer getSsoTicketId() { return ssoTicketId; }
    public void setSsoTicketId(Integer ssoTicketId) { this.ssoTicketId = ssoTicketId; }

    public Ticket getTicket() { return ticket; }
    public void setTicket(Ticket ticket) { this.ticket = ticket; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }

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
    public java.math.BigDecimal getPrice() { return ticket != null ? ticket.getPrice() : null; }
}