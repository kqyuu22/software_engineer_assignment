package com.se.sebtl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.se.sebtl.model.Billing;
import java.util.List;

public interface BillingRepository extends JpaRepository<Billing, Long> {
    List<Billing> findByUserIdOrderByLastUpdatedDesc(Integer userId);
}