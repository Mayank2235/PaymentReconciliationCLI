package com.finance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "invoices", indexes = {
    @Index(name = "idx_invoice_status", columnList = "status"),
    @Index(name = "idx_invoice_number", columnList = "invoiceNumber", unique = true)
})
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private BigDecimal remainingBalance;

    @Column(nullable = false)
    private LocalDate date;

    private String vendor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReconciliationStatus status = ReconciliationStatus.UNMATCHED;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Invoice() {}

    public Invoice(String invoiceNumber, BigDecimal amount, LocalDate date) {
        this.invoiceNumber = invoiceNumber;
        this.amount = amount;
        this.remainingBalance = amount;
        this.date = date;
    }

    public Invoice(String invoiceNumber, BigDecimal amount, LocalDate date, String vendor) {
        this.invoiceNumber = invoiceNumber;
        this.amount = amount;
        this.remainingBalance = amount;
        this.date = date;
        this.vendor = vendor;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getRemainingBalance() { return remainingBalance; }
    public LocalDate getDate() { return date; }
    public String getVendor() { return vendor; }
    public ReconciliationStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setStatus(ReconciliationStatus status) { this.status = status; }
    public void setRemainingBalance(BigDecimal remainingBalance) { this.remainingBalance = remainingBalance; }
}