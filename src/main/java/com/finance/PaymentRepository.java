package com.finance;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByStatus(ReconciliationStatus status, Pageable pageable);

    long countByStatus(ReconciliationStatus status);

    boolean existsByTransactionId(String transactionId);

    List<Payment> findTop50ByOrderByCreatedAtDesc();

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'MATCHED'")
    BigDecimal sumMatchedAmount();

    @Query("SELECT SUM(p.amount) FROM Payment p")
    BigDecimal sumTotalAmount();
}