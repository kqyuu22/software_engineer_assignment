package com.se.sebtl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.se.sebtl.model.GuestTicket;
import java.util.Optional;

public interface GuestTicketRepository extends JpaRepository<GuestTicket, Integer> {
    @Query("SELECT g FROM GuestTicket g JOIN g.ticket t WHERE LOWER(t.licensePlate) = LOWER(:licensePlate) AND t.finished = false")
    Optional<GuestTicket> findByLicensePlateAndFinishedFalse(@Param("licensePlate") String licensePlate);
}
