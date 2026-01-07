package com.finance;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_status", columnList = "status")
})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String transactionId; // Bank Reference

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate paymentDate;

    private String referenceNote; // Might contain invoice number

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReconciliationStatus status = ReconciliationStatus.UNMATCHED;

    public Payment() {}

    public Payment(String transactionId, BigDecimal amount, LocalDate paymentDate, String referenceNote) {
        this.transactionId = transactionId;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.referenceNote = referenceNote;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getTransactionId() { return transactionId; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public String getReferenceNote() { return referenceNote; }
    public ReconciliationStatus getStatus() { return status; }
    public void setStatus(ReconciliationStatus status) { this.status = status; }
}