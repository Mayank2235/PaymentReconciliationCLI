package com.finance;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional(readOnly = true)
public class ReportingService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final AuditLogRepository auditLogRepository;

    public ReportingService(InvoiceRepository invoiceRepository,
                            PaymentRepository paymentRepository,
                            AuditLogRepository auditLogRepository) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public void showUnmatched() {
        long unmatchedInvoices = invoiceRepository.countByStatus(ReconciliationStatus.UNMATCHED);
        long unmatchedPayments = paymentRepository.countByStatus(ReconciliationStatus.UNMATCHED);
        
        System.out.println("\n🔍 Unmatched Records Summary");
        System.out.println("---------------------------");
        System.out.printf("Unmatched Invoices : %d%n", unmatchedInvoices);
        System.out.printf("Unmatched Payments : %d%n", unmatchedPayments);
        System.out.println("---------------------------");
    }

    public void generateDailyReport() {
        long totalMatched = invoiceRepository.countByStatus(ReconciliationStatus.MATCHED);
        long totalPartial = invoiceRepository.countByStatus(ReconciliationStatus.PARTIAL);
        long totalUnmatched = invoiceRepository.countByStatus(ReconciliationStatus.UNMATCHED);
        BigDecimal matchedValue = invoiceRepository.sumMatchedAmount();

        System.out.println("\n📊 Daily Reconciliation Report");
        System.out.println("==================================");
        System.out.printf("Total Fully Matched : %d%n", totalMatched);
        System.out.printf("Total Partial Match : %d%n", totalPartial);
        System.out.printf("Pending / Unmatched : %d%n", totalUnmatched);
        System.out.printf("Total Value Matched : %s%n", matchedValue != null ? matchedValue.toString() : "0.00");
        System.out.println("==================================");
    }

    public void showAuditLog() {
        System.out.println("\n📜 Recent Audit Logs");
        System.out.println("----------------------------------");
        auditLogRepository.findTop50ByOrderByTimestampDesc()
                .forEach(System.out::println);
        System.out.println("----------------------------------");
    }
}