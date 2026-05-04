package com.se.sebtl.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;

@Entity
@Table(name = "all_tickets_view")
@IdClass(TicketView.TicketViewId.class)
public class TicketView {

    @Id
    @Column(name = "ticket_type")
    private String ticketType;

    @Id
    @Column(name = "ticket_id")
    private Integer ticketId;

    @Column(name = "holder_identifier")
    private String holderIdentifier;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    @Column(name = "entry_time")
    private OffsetDateTime entryTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    @Column(name = "exit_time")
    private OffsetDateTime exitTime;

    @Column(name = "license_plate")
    private String licensePlate;

    @Column(name = "parking_spot")
    private Integer parkingSpot;

    @Column(name = "finished")
    private Boolean finished;

    public String getTicketType() { return ticketType; }
    public Integer getTicketId() { return ticketId; }
    public String getHolderIdentifier() { return holderIdentifier; }
    public String getUserId() { return holderIdentifier; } // For frontend compatibility
    public OffsetDateTime getEntryTime() { return entryTime; }
    public OffsetDateTime getExitTime() { return exitTime; }
    public String getLicensePlate() { return licensePlate; }
    public Integer getParkingSpot() { return parkingSpot; }
    public Boolean getFinished() { return finished; }

    public static class TicketViewId implements Serializable {
        private String ticketType;
        private Integer ticketId;

        public TicketViewId() {}
        public TicketViewId(String ticketType, Integer ticketId) {
            this.ticketType = ticketType;
            this.ticketId = ticketId;
        }
        
        // equals and hashCode
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TicketViewId that = (TicketViewId) o;
            return ticketType.equals(that.ticketType) && ticketId.equals(that.ticketId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(ticketType, ticketId);
        }
    }
}
