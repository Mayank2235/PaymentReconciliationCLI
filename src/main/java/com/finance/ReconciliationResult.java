package com.finance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

enum ReconciliationStatus {
    UNMATCHED,
    MATCHED,
    PARTIAL,
    OVERPAID,
    DUPLICATE
}

@Entity
@Table(name = "reconciliations")
public class ReconciliationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    private ReconciliationStatus matchType;

    // Denormalized fields for fast dashboard queries
    private String invoiceNumber;
    private String transactionId;
    private BigDecimal invoiceAmount;
    private BigDecimal paymentAmount;
    private BigDecimal difference;

    private LocalDateTime reconciledAt;

    public ReconciliationResult() {}

    public ReconciliationResult(Invoice invoice, Payment payment, ReconciliationStatus matchType) {
        this.invoice = invoice;
        this.payment = payment;
        this.matchType = matchType;
        this.invoiceNumber = invoice.getInvoiceNumber();
        this.transactionId = payment.getTransactionId();
        this.invoiceAmount = invoice.getAmount();
        this.paymentAmount = payment.getAmount();
        this.difference = invoice.getAmount().subtract(payment.getAmount());
        this.reconciledAt = LocalDateTime.now();
    }

    // Getters
    public Long getId() { return id; }
    public Invoice getInvoice() { return invoice; }
    public Payment getPayment() { return payment; }
    public ReconciliationStatus getMatchType() { return matchType; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public String getTransactionId() { return transactionId; }
    public BigDecimal getInvoiceAmount() { return invoiceAmount; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public BigDecimal getDifference() { return difference; }
    public LocalDateTime getReconciledAt() { return reconciledAt; }
}