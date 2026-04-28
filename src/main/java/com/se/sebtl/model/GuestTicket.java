package com.se.sebtl.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Table(name = "guest_tickets")
public class GuestTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Integer ticketId;

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

    @Column(name = "final_calculated_fee")
    private BigDecimal finalCalculatedFee;

    @Column(name = "paid_directly")
    private Boolean paidDirectly;

    public GuestTicket() {}

    public GuestTicket(String licensePlate, Integer parkingSpot, BigDecimal price) {
        this.entryTime = OffsetDateTime.now();
        this.licensePlate = licensePlate;
        this.parkingSpot = parkingSpot;
        this.price = price;
        this.finished = false;
        this.paidDirectly = false;
    }

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

    public BigDecimal getFinalCalculatedFee() { return finalCalculatedFee; }
    public void setFinalCalculatedFee(BigDecimal finalCalculatedFee) { this.finalCalculatedFee = finalCalculatedFee; }

    public Boolean getPaidDirectly() { return paidDirectly; }
    public void setPaidDirectly(Boolean paidDirectly) { this.paidDirectly = paidDirectly; }
}
