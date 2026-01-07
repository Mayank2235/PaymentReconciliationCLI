package com.finance;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ReconciliationService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final AuditLogRepository auditLogRepository;

    public ReconciliationService(InvoiceRepository invoiceRepository,
                                 PaymentRepository paymentRepository,
                                 ReconciliationRepository reconciliationRepository,
                                 AuditLogRepository auditLogRepository) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void runReconciliation(int batchSize) {
        long startTime = System.currentTimeMillis();
        System.out.println("⏳ Starting Reconciliation Engine (Batch Size: " + batchSize + ")...");

        // Fetch unprocessed records (Phase 4: using batch size)
        List<Invoice> invoices = invoiceRepository.findByStatus(ReconciliationStatus.UNMATCHED, PageRequest.of(0, batchSize));
        List<Payment> payments = paymentRepository.findByStatus(ReconciliationStatus.UNMATCHED, PageRequest.of(0, batchSize));

        int matchedCount = 0;
        int partialCount = 0;

        for (Payment payment : payments) {
            // Strategy 1: Exact Match on Reference Note (if it contains Invoice #)
            Invoice match = invoices.stream()
                    .filter(inv -> inv.getStatus() == ReconciliationStatus.UNMATCHED) // double check
                    .filter(inv -> payment.getReferenceNote() != null && payment.getReferenceNote().contains(inv.getInvoiceNumber()))
                    .findFirst()
                    .orElse(null);

            // Strategy 2: Fallback to Exact Amount + Date proximity (simplified for this exercise)
            if (match == null) {
                match = invoices.stream()
                        .filter(inv -> inv.getStatus() == ReconciliationStatus.UNMATCHED)
                        .filter(inv -> inv.getAmount().compareTo(payment.getAmount()) == 0)
                        .findFirst()
                        .orElse(null);
            }

            if (match != null) {
                // Determine Status
                ReconciliationStatus status = ReconciliationStatus.MATCHED;
                
                // Logic for Partial/Overpayment based on tolerance (Simple implementation)
                BigDecimal diff = match.getAmount().subtract(payment.getAmount());
                if (diff.abs().compareTo(new BigDecimal("10.00")) < 0 && diff.compareTo(BigDecimal.ZERO) != 0) {
                    status = ReconciliationStatus.PARTIAL;
                    partialCount++;
                } else {
                    matchedCount++;
                }

                // Update Entities
                match.setStatus(status);
                payment.setStatus(status);
                
                // Save Result
                reconciliationRepository.save(new ReconciliationResult(match, payment, status));
            }
        }

        // Persist updates
        invoiceRepository.saveAll(invoices);
        paymentRepository.saveAll(payments);

        long duration = System.currentTimeMillis() - startTime;
        String logMsg = String.format("Batch Processed in %d ms. Matched: %d, Partial: %d", duration, matchedCount, partialCount);
        auditLogRepository.save(new AuditLog("RECONCILIATION_RUN", logMsg));

        System.out.println("✔ " + logMsg);
    }
}