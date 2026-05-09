package com.se.sebtl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import com.se.sebtl.model.ParkingSlot;
import com.se.sebtl.model.SlotStatus;

public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, Integer> {
    
    List<ParkingSlot> findByStatus(SlotStatus status);
    List<ParkingSlot> findAllByOrderBySlotIdAsc();

    // This lock physically prevents two concurrent requests from claiming the same spot
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ParkingSlot p WHERE p.status = :status")
    List<ParkingSlot> findByStatusWithLock(@Param("status") SlotStatus status);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ParkingSlot p SET p.status = :status WHERE p.slotId IN :slotIds")
    void updateStatusByIds(@Param("status") SlotStatus status, @Param("slotIds") List<Integer> slotIds);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ParkingSlot p SET p.status = :newStatus WHERE p.status = :oldStatus")
    void updateStatusByCurrentStatus(@Param("oldStatus") SlotStatus oldStatus, @Param("newStatus") SlotStatus newStatus);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ParkingSlot p SET p.status = :newStatus WHERE p.status = :oldStatus AND p.slotId IN :slotIds")
    void updateStatusForSlotIds(@Param("oldStatus") SlotStatus oldStatus, @Param("newStatus") SlotStatus newStatus, @Param("slotIds") List<Integer> slotIds);

    long countByStatus(SlotStatus status);
    long count();
}