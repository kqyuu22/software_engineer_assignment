package com.se.sebtl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import com.se.sebtl.model.SsoTicket;

import java.util.List;

public interface SsoTicketRepository extends JpaRepository<SsoTicket, Integer> {
    @Query("SELECT s FROM SsoTicket s JOIN s.ticket t WHERE t.finished = false")
    List<SsoTicket> findByFinishedFalse();

    @Query("SELECT s FROM SsoTicket s JOIN s.ticket t WHERE s.userId = :userId AND t.finished = false")
    Optional<SsoTicket> findByUserIdAndFinishedFalse(@Param("userId") Integer userId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM SsoTicket s JOIN s.ticket t WHERE LOWER(t.licensePlate) = LOWER(:plate) AND t.finished = false")
    boolean existsByLicensePlateAndFinishedFalse(@Param("plate") String plate);

    // Method for numeric searches (Searches Plate OR Ticket ID OR User ID)
    @Query("SELECT s FROM SsoTicket s JOIN s.ticket t WHERE LOWER(t.licensePlate) LIKE LOWER(CONCAT('%', :licensePlate, '%')) OR t.ticketId = :ticketId OR s.userId = :userId ORDER BY t.entryTime DESC")
    List<SsoTicket> findByLicensePlateContainingIgnoreCaseOrTicketIdOrUserIdOrderByEntryTimeDesc(
            @Param("licensePlate") String licensePlate, @Param("ticketId") Integer ticketId, @Param("userId") Integer userId);
    

    // For the main history view: Operators need to see the most recent entries first.
    @Query("SELECT s FROM SsoTicket s JOIN s.ticket t ORDER BY t.entryTime DESC")
    List<SsoTicket> findAllByOrderByEntryTimeDesc();

    // For the search bar: Allows searching by partial license plate (case-insensitive).
    @Query("SELECT s FROM SsoTicket s JOIN s.ticket t WHERE LOWER(t.licensePlate) LIKE LOWER(CONCAT('%', :licensePlate, '%')) ORDER BY t.entryTime DESC")
    List<SsoTicket> findByLicensePlateContainingIgnoreCaseOrderByEntryTimeDesc(@Param("licensePlate") String licensePlate);
    
    // (Optional) If you also want to search by exact User ID
    @Query("SELECT s FROM SsoTicket s JOIN s.ticket t WHERE s.userId = :userId ORDER BY t.entryTime DESC")
    List<SsoTicket> findByUserIdOrderByEntryTimeDesc(@Param("userId") int userId);
}