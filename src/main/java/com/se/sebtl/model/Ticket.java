package com.se.sebtl.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "tickets", schema = "public")
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Integer ticketId;

    @Column(name = "entry_time", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime entryTime;

    @Column(name = "exit_time", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime exitTime;

    @Column(name = "license_plate", nullable = false)
    private String licensePlate;

    @Column(name = "parking_spot")
    private Integer parkingSpot;

    @Column(name = "finished", nullable = false)
    private Boolean finished = false;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    public Integer getTicketId() { return ticketId; }
    public void setTicketId(Integer ticketId) { this.ticketId = ticketId; }

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
}