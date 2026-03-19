package com.finance;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class ReportingService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final ReportSummaryRepository reportSummaryRepository;
    private final AuditLogRepository auditLogRepository;

    public ReportingService(InvoiceRepository invoiceRepository,
                            PaymentRepository paymentRepository,
                            ReconciliationRepository reconciliationRepository,
                            ReportSummaryRepository reportSummaryRepository,
                            AuditLogRepository auditLogRepository) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.reportSummaryRepository = reportSummaryRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void generateAndSaveReport() {
        ReportSummary report = buildReport();
        reportSummaryRepository.save(report);
        System.out.printf("📊 Report saved | Invoices=%d | Payments=%d | Matched=%.2f | Outstanding=%.2f%n",
                report.getTotalInvoices(), report.getTotalPayments(),
                safe(report.getTotalMatchedAmount()), safe(report.getTotalOutstandingBalance()));
        auditLogRepository.save(new AuditLog("REPORT_GENERATED",
                "Total=" + report.getTotalInvoices() + " invoices, " + report.getTotalPayments() + " payments"));
    }

    public ReportSummary buildReport() {
        ReportSummary r = new ReportSummary();

        r.setTotalInvoices(invoiceRepository.count());
        r.setTotalPayments(paymentRepository.count());

        r.setFullyMatched(invoiceRepository.countByStatus(ReconciliationStatus.MATCHED));
        r.setPartiallyMatched(invoiceRepository.countByStatus(ReconciliationStatus.PARTIAL));
        r.setUnmatched(invoiceRepository.countByStatus(ReconciliationStatus.UNMATCHED));
        r.setOverpaid(invoiceRepository.countByStatus(ReconciliationStatus.OVERPAID));
        r.setDuplicates(paymentRepository.countByStatus(ReconciliationStatus.DUPLICATE));

        r.setTotalInvoiceAmount(orZero(invoiceRepository.sumTotalAmount()));
        r.setTotalPaymentReceived(orZero(paymentRepository.sumTotalAmount()));
        r.setTotalMatchedAmount(orZero(invoiceRepository.sumMatchedAmount()));
        r.setTotalOutstandingBalance(orZero(invoiceRepository.sumOutstandingBalance()));
        r.setTotalUnmatchedPayments(orZero(paymentRepository.sumTotalAmount())
                .subtract(orZero(paymentRepository.sumMatchedAmount())));

        return r;
    }

    public Optional<ReportSummary> getLatestReport() {
        return reportSummaryRepository.findTopByOrderByGeneratedAtDesc();
    }

    // ── Legacy CLI methods (backward compat) ─────────────────────────────────

    public void generateDailyReport() {
        ReportSummary r = buildReport();
        System.out.println("\n📊 Daily Reconciliation Report");
        System.out.println("==================================");
        System.out.printf("Total Invoices       : %d%n", r.getTotalInvoices());
        System.out.printf("Total Payments       : %d%n", r.getTotalPayments());
        System.out.printf("Fully Matched        : %d%n", r.getFullyMatched());
        System.out.printf("Partially Matched    : %d%n", r.getPartiallyMatched());
        System.out.printf("Unmatched            : %d%n", r.getUnmatched());
        System.out.printf("Total Matched ($)    : %.2f%n", safe(r.getTotalMatchedAmount()));
        System.out.printf("Outstanding ($)      : %.2f%n", safe(r.getTotalOutstandingBalance()));
        System.out.println("==================================");
    }

    public void showUnmatched() {
        long unmatchedInvoices = invoiceRepository.countByStatus(ReconciliationStatus.UNMATCHED);
        long unmatchedPayments = paymentRepository.countByStatus(ReconciliationStatus.UNMATCHED);
        System.out.println("\n🔍 Unmatched Records");
        System.out.printf("Invoices : %d%n", unmatchedInvoices);
        System.out.printf("Payments : %d%n", unmatchedPayments);
    }

    public void showAuditLog() {
        System.out.println("\n📜 Recent Audit Logs");
        auditLogRepository.findTop50ByOrderByTimestampDesc().forEach(System.out::println);
    }

    private BigDecimal orZero(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    private double safe(BigDecimal val) {
        return val != null ? val.doubleValue() : 0.0;
    }
}