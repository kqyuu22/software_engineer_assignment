package com.se.sebtl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.se.sebtl.model.TicketView;
import java.util.List;

public interface TicketViewRepository extends JpaRepository<TicketView, TicketView.TicketViewId> {
    List<TicketView> findAllByOrderByEntryTimeDesc();
    List<TicketView> findByTicketIdOrLicensePlateContainingIgnoreCaseOrHolderIdentifierContainingIgnoreCaseOrderByEntryTimeDesc(Integer ticketId, String licensePlate, String holderIdentifier);
    List<TicketView> findByLicensePlateContainingIgnoreCaseOrHolderIdentifierContainingIgnoreCaseOrderByEntryTimeDesc(String licensePlate, String holderIdentifier);
    List<TicketView> findByLicensePlateContainingIgnoreCaseOrderByEntryTimeDesc(String licensePlate);
}
