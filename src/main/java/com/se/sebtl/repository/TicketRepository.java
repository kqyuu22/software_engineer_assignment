package com.se.sebtl.repository;

import com.se.sebtl.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Integer> {
	@org.springframework.data.jpa.repository.Modifying
	@org.springframework.data.jpa.repository.Query("UPDATE Ticket t SET t.exitTime = :exitTime, t.finished = true WHERE t.ticketId = :ticketId")
	@org.springframework.transaction.annotation.Transactional
	void updateExitTimeAndFinish(@org.springframework.data.repository.query.Param("ticketId") Integer ticketId,
								 @org.springframework.data.repository.query.Param("exitTime") java.time.OffsetDateTime exitTime);

	@org.springframework.data.jpa.repository.Query("SELECT t FROM Ticket t WHERE t.parkingSpot = :parkingSpot AND t.finished = false")
	java.util.List<Ticket> findByParkingSpotAndFinishedFalse(@org.springframework.data.repository.query.Param("parkingSpot") Integer parkingSpot);

	@org.springframework.data.jpa.repository.Modifying
	@org.springframework.data.jpa.repository.Query("UPDATE Ticket t SET t.finished = true, t.exitTime = CURRENT_TIMESTAMP WHERE t.finished = false")
	void finishAllActiveTickets();
}
