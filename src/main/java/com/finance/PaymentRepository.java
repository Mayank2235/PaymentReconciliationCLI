package com.finance;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    List<Payment> findByStatus(ReconciliationStatus status, Pageable pageable);
    
    long countByStatus(ReconciliationStatus status);
}