package com.finance;

import jakarta.persistence.*;
import java.time.LocalDateTime;

enum ReconciliationStatus {
    UNMATCHED,
    MATCHED,
    PARTIAL,
    OVERPAID
}

@Entity
@Table(name = "reconciliations")
public class ReconciliationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @OneToOne
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    private ReconciliationStatus matchType;

    private LocalDateTime reconciledAt;

    public ReconciliationResult() {}

    public ReconciliationResult(Invoice invoice, Payment payment, ReconciliationStatus matchType) {
        this.invoice = invoice;
        this.payment = payment;
        this.matchType = matchType;
        this.reconciledAt = LocalDateTime.now();
    }

    // Getters
    public ReconciliationStatus getMatchType() { return matchType; }
}