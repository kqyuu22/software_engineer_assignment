package com.se.sebtl.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.se.sebtl.model.Alert;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Integer> {
    // Spring generates this SQL automatically based on the method name
    List<Alert> findByAcknowledgedFalseOrderByTypeAscTimestampDesc();
    List<Alert> findByAcknowledgedTrueOrderByTimestampDesc();
}
