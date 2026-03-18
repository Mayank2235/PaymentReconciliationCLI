package com.finance;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByStatus(ReconciliationStatus status, Pageable pageable);

    long countByStatus(ReconciliationStatus status);

    boolean existsByInvoiceNumber(String invoiceNumber);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    List<Invoice> findTop50ByOrderByCreatedAtDesc();

    @Query("SELECT SUM(i.amount) FROM Invoice i WHERE i.status = 'MATCHED'")
    BigDecimal sumMatchedAmount();

    @Query("SELECT SUM(i.remainingBalance) FROM Invoice i WHERE i.status != 'MATCHED'")
    BigDecimal sumOutstandingBalance();

    @Query("SELECT SUM(i.amount) FROM Invoice i")
    BigDecimal sumTotalAmount();
}