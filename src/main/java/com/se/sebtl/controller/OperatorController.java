package com.se.sebtl.controller;

import com.se.sebtl.model.ParkingSlot;
import com.se.sebtl.repository.ParkingSlotRepository;

import org.aspectj.weaver.patterns.ConcreteCflowPointcut.Slot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.se.sebtl.model.SlotStatus;

import java.util.List;

@RestController
@RequestMapping("/slots")
@CrossOrigin(origins = "*")
public class OperatorController {

    @Autowired
    private ParkingSlotRepository parkingSlotRepository; // Direct use


    // GET /slots - Get all parking slots
    @GetMapping
    public List<ParkingSlot> getAll() {
        return parkingSlotRepository.findAll();
    }

    @PostMapping
    public ParkingSlot create(@RequestBody ParkingSlot slot) {
        return parkingSlotRepository.save(slot);
    }

    // Find a slot by status
    @GetMapping("/status/{status}")
    public List<ParkingSlot> getByStatus(@PathVariable String status) {
        return parkingSlotRepository.findByStatus(SlotStatus.valueOf(status.toUpperCase()));
    }
}