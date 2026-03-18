package com.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.util.List;

public interface ReconciliationRepository extends JpaRepository<ReconciliationResult, Long> {

    List<ReconciliationResult> findTop50ByOrderByReconciledAtDesc();

    long countByMatchType(ReconciliationStatus matchType);

    @Query("SELECT SUM(r.paymentAmount) FROM ReconciliationResult r WHERE r.matchType = 'MATCHED'")
    BigDecimal sumMatchedPaymentAmount();

    @Query("SELECT SUM(r.paymentAmount) FROM ReconciliationResult r WHERE r.matchType = 'PARTIAL'")
    BigDecimal sumPartialPaymentAmount();
}