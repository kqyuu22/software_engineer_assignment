// package com.se.sebtl.model;

// import java.util.*;

// public class Ticket {
//     private int userId;
//     private int ticketId;
//     private Role role;
//     private Date entryTime;
//     private Date exitTime;
//     private String licensePlate;
//     private int parkingSpot;
//     private boolean finished;
//     private double price;
//     private double fee;

//     public Ticket() {}

//     public Ticket(int userId, int ticketId, Role role, String licensePlate, int parkingSpot, double price, double fee) {
//         this.userId = userId;
//         this.ticketId = ticketId;
//         this.role = role;
//         this.entryTime = new Date();
//         this.licensePlate = licensePlate;
//         this.parkingSpot = parkingSpot;
//         this.finished = false;
//         this.price = price;
//         this.fee = fee;
//     }

//     public int getUserId() { return userId; }
//     public void setUserId(int userId) { this.userId = userId; }

//     public int getTicketId() { return ticketId; }
//     public void setTicketId(int ticketId) { this.ticketId = ticketId; }

//     public Role getRole() { return role; }
//     public void setRole(Role role) { this.role = role; }

//     public Date getEntryTime() { return entryTime; }
//     public void setEntryTime(Date entryTime) { this.entryTime = entryTime; }

//     public Date getExitTime() { return exitTime; }
//     public void setExitTime(Date exitTime) { this.exitTime = exitTime; }

//     public String getLicensePlate() { return licensePlate; }
//     public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

//     public int getParkingSpot() { return parkingSpot; }
//     public void setParkingSpot(int parkingSpot) { this.parkingSpot = parkingSpot; }

//     public double getPrice() { return price; }
//     public void setPrice(double price) { this.price = price; }

//     public double getFee() { return fee; }
//     public void setFee(double fee) { this.fee = fee; }

//     public boolean isFinished() { return finished; }
//     public void setFinished(boolean finished) { this.finished = finished; }
//     public void finish() {
//         this.exitTime = new Date();
//         this.finished = true;
//     }
// }
