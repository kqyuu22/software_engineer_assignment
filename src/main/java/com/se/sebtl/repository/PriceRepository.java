package com.se.sebtl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.se.sebtl.model.Price;
import com.se.sebtl.model.Role;

public interface PriceRepository extends JpaRepository<Price, Role> {
    
}
