package com.finance;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    
    // Find open invoices for processing
    List<Invoice> findByStatus(ReconciliationStatus status, Pageable pageable);

    long countByStatus(ReconciliationStatus status);
    
    @Query("SELECT SUM(i.amount) FROM Invoice i WHERE i.status = 'MATCHED'")
    java.math.BigDecimal sumMatchedAmount();
}