package com.se.sebtl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import com.se.sebtl.model.SsoTicket;

import java.util.List;

public interface SsoTicketRepository extends JpaRepository<SsoTicket, Integer> {
    List<SsoTicket> findByFinishedFalse();
    Optional<SsoTicket> findByUserIdAndFinishedFalse(Integer userId);
    boolean existsByLicensePlateAndFinishedFalse(String plate);
    // Method for numeric searches (Searches Plate OR Ticket ID OR User ID)
    List<SsoTicket> findByLicensePlateContainingIgnoreCaseOrTicketIdOrUserIdOrderByEntryTimeDesc(
            String licensePlate, Integer ticketId, Integer userId);
    

    // For the main history view: Operators need to see the most recent entries first.
    List<SsoTicket> findAllByOrderByEntryTimeDesc();

    // For the search bar: Allows searching by partial license plate (case-insensitive).
    List<SsoTicket> findByLicensePlateContainingIgnoreCaseOrderByEntryTimeDesc(String licensePlate);
    
    // (Optional) If you also want to search by exact User ID
    List<SsoTicket> findByUserIdOrderByEntryTimeDesc(int userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE SsoTicket t SET t.exitTime = :exitTime WHERE t.ticketId = :ticketId")
    @org.springframework.transaction.annotation.Transactional
    void updateExitTime(@org.springframework.data.repository.query.Param("ticketId") Integer ticketId, @org.springframework.data.repository.query.Param("exitTime") java.time.OffsetDateTime exitTime);
}