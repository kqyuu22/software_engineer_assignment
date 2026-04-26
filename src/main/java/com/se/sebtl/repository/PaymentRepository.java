package com.se.sebtl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.se.sebtl.model.Payment;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    List<Payment> findByUserIdOrderByTimestampDesc(Integer userId);
}