package com.se.sebtl.model;

import jakarta.persistence.*;
import java.time.LocalDateTime; // Swapped to modern Java Time API

import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Integer ticketId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "entry_time", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime entryTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "exit_time", columnDefinition = "TIMESTAMP")
    private LocalDateTime exitTime;

    @Column(name = "license_plate", nullable = false)
    private String licensePlate;

    @Column(name = "parking_spot")
    private Integer parkingSpot;

    @Column(name = "finished")
    private Boolean finished;

    @Column(name = "price")
    private Double price;

    @Column(name = "fee")
    private Double fee;

    public Ticket() {}

    public Ticket(Integer userId, Role role, String licensePlate, Integer parkingSpot, Double price, Double fee) {
        this.userId = userId;
        this.role = role;
        this.entryTime = LocalDateTime.now(); // Updated to use LocalDateTime
        this.licensePlate = licensePlate;
        this.parkingSpot = parkingSpot;
        this.finished = false;
        this.price = price;
        this.fee = fee;
    }

    public Integer getTicketId() { return ticketId; }
    public void setTicketId(Integer ticketId) { this.ticketId = ticketId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public LocalDateTime getEntryTime() { return entryTime; }
    public void setEntryTime(LocalDateTime entryTime) { this.entryTime = entryTime; }

    public LocalDateTime getExitTime() { return exitTime; }
    public void setExitTime(LocalDateTime exitTime) { this.exitTime = exitTime; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public Integer getParkingSpot() { return parkingSpot; }
    public void setParkingSpot(Integer parkingSpot) { this.parkingSpot = parkingSpot; }

    public Boolean getFinished() { return finished; }
    public void setFinished(Boolean finished) { this.finished = finished; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Double getFee() { return fee; }
    public void setFee(Double fee) { this.fee = fee; }

    public void finish() {
        this.exitTime = LocalDateTime.now(); // Updated to use LocalDateTime
        this.finished = true;
    }
}