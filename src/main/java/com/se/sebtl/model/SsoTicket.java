package com.se.sebtl.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Table(name = "sso_tickets")
public class SsoTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Integer ticketId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    @Column(name = "entry_time", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime entryTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    @Column(name = "exit_time", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime exitTime;

    @Column(name = "license_plate", nullable = false)
    private String licensePlate;

    @Column(name = "parking_spot")
    private Integer parkingSpot;

    @Column(name = "finished")
    private Boolean finished;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "bill_id", unique = true)
    private Long billId;

    public SsoTicket() {}

    public SsoTicket(Integer userId, String licensePlate, Integer parkingSpot, BigDecimal price) {
        this.userId = userId;
        this.entryTime = OffsetDateTime.now();
        this.licensePlate = licensePlate;
        this.parkingSpot = parkingSpot;
        this.finished = false;
        this.price = price;
    }

    public Integer getTicketId() { return ticketId; }
    public void setTicketId(Integer ticketId) { this.ticketId = ticketId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public OffsetDateTime getEntryTime() { return entryTime; }
    public void setEntryTime(OffsetDateTime entryTime) { this.entryTime = entryTime; }

    public OffsetDateTime getExitTime() { return exitTime; }
    public void setExitTime(OffsetDateTime exitTime) { this.exitTime = exitTime; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public Integer getParkingSpot() { return parkingSpot; }
    public void setParkingSpot(Integer parkingSpot) { this.parkingSpot = parkingSpot; }

    public Boolean getFinished() { return finished; }
    public void setFinished(Boolean finished) { this.finished = finished; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }

    public void finish() {
        this.exitTime = OffsetDateTime.now();
        this.finished = true;
    }
}