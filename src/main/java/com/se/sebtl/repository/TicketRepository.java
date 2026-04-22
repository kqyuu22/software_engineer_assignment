package com.se.sebtl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import com.se.sebtl.model.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Integer> {
    Optional<Ticket> findByUserIdAndFinishedFalse(Integer userId);
    boolean existsByLicensePlateAndFinishedFalse(String plate);
}