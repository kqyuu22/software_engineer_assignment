package com.se.sebtl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.se.sebtl.model.GuestTicket;
import java.util.Optional;

public interface GuestTicketRepository extends JpaRepository<GuestTicket, Integer> {
    Optional<GuestTicket> findByLicensePlateAndFinishedFalse(String licensePlate);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE GuestTicket t SET t.exitTime = :exitTime WHERE t.ticketId = :ticketId")
    @org.springframework.transaction.annotation.Transactional
    void updateExitTime(@org.springframework.data.repository.query.Param("ticketId") Integer ticketId, @org.springframework.data.repository.query.Param("exitTime") java.time.OffsetDateTime exitTime);
}
