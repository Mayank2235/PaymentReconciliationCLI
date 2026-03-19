package com.finance;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_summaries")
public class ReportSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long totalInvoices;
    private long totalPayments;

    private long fullyMatched;
    private long partiallyMatched;
    private long unmatched;
    private long overpaid;
    private long duplicates;

    private BigDecimal totalInvoiceAmount;
    private BigDecimal totalPaymentReceived;
    private BigDecimal totalMatchedAmount;
    private BigDecimal totalOutstandingBalance;
    private BigDecimal totalUnmatchedPayments;

    @Column(nullable = false)
    private LocalDateTime generatedAt = LocalDateTime.now();

    public ReportSummary() {}

    // Getters
    public Long getId() { return id; }
    public long getTotalInvoices() { return totalInvoices; }
    public long getTotalPayments() { return totalPayments; }
    public long getFullyMatched() { return fullyMatched; }
    public long getPartiallyMatched() { return partiallyMatched; }
    public long getUnmatched() { return unmatched; }
    public long getOverpaid() { return overpaid; }
    public long getDuplicates() { return duplicates; }
    public BigDecimal getTotalInvoiceAmount() { return totalInvoiceAmount; }
    public BigDecimal getTotalPaymentReceived() { return totalPaymentReceived; }
    public BigDecimal getTotalMatchedAmount() { return totalMatchedAmount; }
    public BigDecimal getTotalOutstandingBalance() { return totalOutstandingBalance; }
    public BigDecimal getTotalUnmatchedPayments() { return totalUnmatchedPayments; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }

    // Setters
    public void setTotalInvoices(long v) { this.totalInvoices = v; }
    public void setTotalPayments(long v) { this.totalPayments = v; }
    public void setFullyMatched(long v) { this.fullyMatched = v; }
    public void setPartiallyMatched(long v) { this.partiallyMatched = v; }
    public void setUnmatched(long v) { this.unmatched = v; }
    public void setOverpaid(long v) { this.overpaid = v; }
    public void setDuplicates(long v) { this.duplicates = v; }
    public void setTotalInvoiceAmount(BigDecimal v) { this.totalInvoiceAmount = v; }
    public void setTotalPaymentReceived(BigDecimal v) { this.totalPaymentReceived = v; }
    public void setTotalMatchedAmount(BigDecimal v) { this.totalMatchedAmount = v; }
    public void setTotalOutstandingBalance(BigDecimal v) { this.totalOutstandingBalance = v; }
    public void setTotalUnmatchedPayments(BigDecimal v) { this.totalUnmatchedPayments = v; }
}
