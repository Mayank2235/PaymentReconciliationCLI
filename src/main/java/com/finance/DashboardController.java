package com.finance;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final ReportingService reportingService;

    public DashboardController(InvoiceRepository invoiceRepository,
                               PaymentRepository paymentRepository,
                               ReconciliationRepository reconciliationRepository,
                               ReportingService reportingService) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.reportingService = reportingService;
    }

    /**
     * Main dashboard summary endpoint — polled every 5s by the frontend.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        long totalInvoices = invoiceRepository.count();
        long totalPayments = paymentRepository.count();
        long fullyMatched = invoiceRepository.countByStatus(ReconciliationStatus.MATCHED);
        long partiallyMatched = invoiceRepository.countByStatus(ReconciliationStatus.PARTIAL);
        long unmatched = invoiceRepository.countByStatus(ReconciliationStatus.UNMATCHED);
        long overpaid = invoiceRepository.countByStatus(ReconciliationStatus.OVERPAID);
        long unmatchedPayments = paymentRepository.countByStatus(ReconciliationStatus.UNMATCHED);
        long duplicates = paymentRepository.countByStatus(ReconciliationStatus.DUPLICATE);

        BigDecimal totalInvoiceAmount = orZero(invoiceRepository.sumTotalAmount());
        BigDecimal totalPaymentReceived = orZero(paymentRepository.sumTotalAmount());
        BigDecimal matchedAmount = orZero(invoiceRepository.sumMatchedAmount());
        BigDecimal outstandingBalance = orZero(invoiceRepository.sumOutstandingBalance());

        return ResponseEntity.ok(Map.ofEntries(
                Map.entry("totalInvoices", totalInvoices),
                Map.entry("totalPayments", totalPayments),
                Map.entry("fullyMatched", fullyMatched),
                Map.entry("partiallyMatched", partiallyMatched),
                Map.entry("unmatchedInvoices", unmatched),
                Map.entry("unmatchedPayments", unmatchedPayments),
                Map.entry("overpaid", overpaid),
                Map.entry("duplicates", duplicates),
                Map.entry("totalInvoiceAmount", totalInvoiceAmount),
                Map.entry("totalPaymentReceived", totalPaymentReceived),
                Map.entry("matchedAmount", matchedAmount),
                Map.entry("outstandingBalance", outstandingBalance)
        ));
    }

    @GetMapping("/reconciliations/recent")
    public ResponseEntity<List<ReconciliationResult>> recentReconciliations() {
        return ResponseEntity.ok(reconciliationRepository.findTop50ByOrderByReconciledAtDesc());
    }

    @GetMapping("/reports/latest")
    public ResponseEntity<?> latestReport() {
        Optional<ReportSummary> report = reportingService.getLatestReport();
        return report.map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    private BigDecimal orZero(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}
