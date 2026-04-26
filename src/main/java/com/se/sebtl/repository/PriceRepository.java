package com.se.sebtl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.se.sebtl.model.Price;

public interface PriceRepository extends JpaRepository<Price, Integer> {
    
}
