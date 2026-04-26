package com.se.sebtl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import com.se.sebtl.model.Ticket;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Integer> {
    Optional<Ticket> findByUserIdAndFinishedFalse(Integer userId);
    boolean existsByLicensePlateAndFinishedFalse(String plate);
    // Method for numeric searches (Searches Plate OR Ticket ID OR User ID)
    List<Ticket> findByLicensePlateContainingIgnoreCaseOrTicketIdOrUserIdOrderByEntryTimeDesc(
            String licensePlate, Integer ticketId, Integer userId);
    

    // For the main history view: Operators need to see the most recent entries first.
    List<Ticket> findAllByOrderByEntryTimeDesc();

    // For the search bar: Allows searching by partial license plate (case-insensitive).
    List<Ticket> findByLicensePlateContainingIgnoreCaseOrderByEntryTimeDesc(String licensePlate);
    
    // (Optional) If you also want to search by exact User ID
    List<Ticket> findByUserIdOrderByEntryTimeDesc(int userId);

    // Find the payment by ticket ID (for redirecting to BKPay)
}