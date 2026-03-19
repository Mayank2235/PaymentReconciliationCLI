package com.finance;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ReconciliationService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final AuditLogRepository auditLogRepository;

    // 2% tolerance for fuzzy amount matching
    private static final BigDecimal TOLERANCE_PERCENT = new BigDecimal("0.02");
    // Full match threshold: within 10 units is considered MATCHED (not PARTIAL)
    private static final BigDecimal FULL_MATCH_THRESHOLD = new BigDecimal("10.00");

    public ReconciliationService(InvoiceRepository invoiceRepository,
                                 PaymentRepository paymentRepository,
                                 ReconciliationRepository reconciliationRepository,
                                 AuditLogRepository auditLogRepository) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void runReconciliation() {
        runReconciliation(200);
    }

    @Transactional
    public void runReconciliation(int batchSize) {
        long startTime = System.currentTimeMillis();
        System.out.println("⏳ Reconciliation Engine running (batch=" + batchSize + ")...");

        List<Payment> unmatchedPayments = paymentRepository.findByStatus(
                ReconciliationStatus.UNMATCHED, PageRequest.of(0, batchSize));

        int matchedCount = 0, partialCount = 0, overpaidCount = 0, duplicateCount = 0;

        for (Payment payment : unmatchedPayments) {

            // ── Guard: Duplicate payment detection ───────────────────────────
            long existingResults = reconciliationRepository.countByMatchType(ReconciliationStatus.DUPLICATE);
            // More precise duplicate check: same txId already has a reconciliation record
            boolean alreadyProcessed = reconciliationRepository.findTop50ByOrderByReconciledAtDesc()
                    .stream()
                    .anyMatch(r -> payment.getTransactionId().equals(r.getTransactionId())
                                  && r.getMatchType() != ReconciliationStatus.DUPLICATE);
            if (alreadyProcessed) {
                payment.setStatus(ReconciliationStatus.DUPLICATE);
                auditLogRepository.save(new AuditLog("DUPLICATE_PAYMENT",
                        "Duplicate detected: " + payment.getTransactionId()));
                duplicateCount++;
                continue;
            }

            // ── Strategy 1: Reference-based match ───────────────────────────
            Invoice match = null;
            if (payment.getReferenceNote() != null && !payment.getReferenceNote().isBlank()) {
                match = invoiceRepository
                        .findAll()
                        .stream()
                        .filter(inv -> inv.getStatus() == ReconciliationStatus.UNMATCHED
                                || inv.getStatus() == ReconciliationStatus.PARTIAL)
                        .filter(inv -> payment.getReferenceNote().contains(inv.getInvoiceNumber()))
                        .findFirst()
                        .orElse(null);
            }

            // ── Strategy 2: Fuzzy amount match (within 2%) ──────────────────
            if (match == null) {
                final BigDecimal payAmt = payment.getAmount();
                match = invoiceRepository
                        .findAll()
                        .stream()
                        .filter(inv -> inv.getStatus() == ReconciliationStatus.UNMATCHED)
                        .filter(inv -> {
                            BigDecimal tolerance = inv.getAmount()
                                    .multiply(TOLERANCE_PERCENT)
                                    .setScale(2, RoundingMode.HALF_UP);
                            BigDecimal diff = inv.getAmount().subtract(payAmt).abs();
                            return diff.compareTo(tolerance) <= 0;
                        })
                        .findFirst()
                        .orElse(null);
            }

            if (match != null) {
                BigDecimal remaining = match.getRemainingBalance();
                BigDecimal payAmt = payment.getAmount();
                BigDecimal diff = remaining.subtract(payAmt);

                ReconciliationStatus status;

                if (diff.abs().compareTo(FULL_MATCH_THRESHOLD) <= 0) {
                    // Full match (within threshold)
                    status = ReconciliationStatus.MATCHED;
                    match.setRemainingBalance(BigDecimal.ZERO);
                    match.setStatus(ReconciliationStatus.MATCHED);
                    matchedCount++;
                } else if (payAmt.compareTo(remaining) > 0) {
                    // Overpayment
                    status = ReconciliationStatus.OVERPAID;
                    match.setRemainingBalance(BigDecimal.ZERO);
                    match.setStatus(ReconciliationStatus.OVERPAID);
                    overpaidCount++;
                } else {
                    // Partial payment
                    status = ReconciliationStatus.PARTIAL;
                    match.setRemainingBalance(remaining.subtract(payAmt));
                    match.setStatus(ReconciliationStatus.PARTIAL);
                    partialCount++;
                }

                payment.setStatus(status);
                reconciliationRepository.save(new ReconciliationResult(match, payment, status));
            }
            // If no match found, payment stays UNMATCHED
        }

        // Persist all entity updates
        paymentRepository.saveAll(unmatchedPayments);

        long duration = System.currentTimeMillis() - startTime;
        String logMsg = String.format(
                "Batch %dms | Matched:%d | Partial:%d | Overpaid:%d | Duplicate:%d",
                duration, matchedCount, partialCount, overpaidCount, duplicateCount);
        auditLogRepository.save(new AuditLog("RECONCILIATION_RUN", logMsg));
        System.out.println("✔ " + logMsg);
    }
}