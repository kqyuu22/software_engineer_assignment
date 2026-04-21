package com.se.sebtl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import com.se.sebtl.model.ParkingSlot;
import com.se.sebtl.model.SlotStatus;

public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, Integer> {
    List<ParkingSlot> findByStatus(SlotStatus status);
}