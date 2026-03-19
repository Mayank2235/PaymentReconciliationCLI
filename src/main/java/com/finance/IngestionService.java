package com.finance;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class IngestionService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final AuditLogRepository auditLogRepository;

    public IngestionService(InvoiceRepository invoiceRepository,
                            PaymentRepository paymentRepository,
                            AuditLogRepository auditLogRepository) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.auditLogRepository = auditLogRepository;
    }

    // ─── JSON / API Ingestion ────────────────────────────────────────────────

    @Transactional
    public int ingestInvoicesFromJson(List<Map<String, Object>> payload) {
        int saved = 0;
        List<Invoice> batch = new ArrayList<>();
        for (Map<String, Object> item : payload) {
            try {
                String number = (String) item.get("invoiceNumber");
                if (number == null || invoiceRepository.existsByInvoiceNumber(number)) continue;
                BigDecimal amount = new BigDecimal(item.get("amount").toString());
                LocalDate date = LocalDate.parse((String) item.get("date"));
                String vendor = (String) item.getOrDefault("vendor", "Unknown");
                batch.add(new Invoice(number, amount, date, vendor));
                saved++;
            } catch (Exception ignored) {}
        }
        invoiceRepository.saveAll(batch);
        auditLogRepository.save(new AuditLog("API_INGEST_INVOICES", "Saved: " + saved + " of " + payload.size()));
        return saved;
    }

    @Transactional
    public int ingestPaymentsFromJson(List<Map<String, Object>> payload) {
        int saved = 0;
        List<Payment> batch = new ArrayList<>();
        for (Map<String, Object> item : payload) {
            try {
                String txId = (String) item.get("transactionId");
                if (txId == null || paymentRepository.existsByTransactionId(txId)) continue;
                BigDecimal amount = new BigDecimal(item.get("amount").toString());
                LocalDate date = LocalDate.parse(
                    item.containsKey("paymentDate") ? (String) item.get("paymentDate") : (String) item.get("date")
                );
                String ref = (String) item.getOrDefault("referenceNote", "");
                batch.add(new Payment(txId, amount, date, ref));
                saved++;
            } catch (Exception ignored) {}
        }
        paymentRepository.saveAll(batch);
        auditLogRepository.save(new AuditLog("API_INGEST_PAYMENTS", "Saved: " + saved + " of " + payload.size()));
        return saved;
    }

    // ─── CSV Ingestion (backward compat) ────────────────────────────────────

    @Transactional
    public void ingestInvoices(String filePath) {
        System.out.println("⏳ Reading Invoices from: " + filePath);
        int success = 0, skipped = 0;
        List<Invoice> batch = new ArrayList<>();
        try (Reader reader = new FileReader(filePath);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            for (CSVRecord record : csvParser) {
                try {
                    String number = record.get("invoice_number");
                    if (invoiceRepository.existsByInvoiceNumber(number)) { skipped++; continue; }
                    BigDecimal amount = new BigDecimal(record.get("amount"));
                    LocalDate date = LocalDate.parse(record.get("date"));
                    batch.add(new Invoice(number, amount, date));
                    success++;
                } catch (Exception e) { skipped++; }
            }
            invoiceRepository.saveAll(batch);
            auditLogRepository.save(new AuditLog("INGEST_INVOICES", "Success: " + success + ", Skipped: " + skipped));
            System.out.printf("✔ Ingestion Complete.%n   Processed: %d%n   Skipped: %d%n", success, skipped);
        } catch (Exception e) {
            throw new RuntimeException("Failed to ingest invoices: " + e.getMessage());
        }
    }

    @Transactional
    public void ingestPayments(String filePath) {
        System.out.println("⏳ Reading Payments from: " + filePath);
        int success = 0, skipped = 0;
        List<Payment> batch = new ArrayList<>();
        try (Reader reader = new FileReader(filePath);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            for (CSVRecord record : csvParser) {
                try {
                    String txId = record.get("transaction_id");
                    if (paymentRepository.existsByTransactionId(txId)) { skipped++; continue; }
                    BigDecimal amount = new BigDecimal(record.get("amount"));
                    LocalDate date = LocalDate.parse(record.get("date"));
                    String ref = record.get("reference");
                    batch.add(new Payment(txId, amount, date, ref));
                    success++;
                } catch (Exception e) { skipped++; }
            }
            paymentRepository.saveAll(batch);
            auditLogRepository.save(new AuditLog("INGEST_PAYMENTS", "Success: " + success + ", Skipped: " + skipped));
            System.out.printf("✔ Ingestion Complete.%n   Processed: %d%n   Skipped: %d%n", success, skipped);
        } catch (Exception e) {
            throw new RuntimeException("Failed to ingest payments: " + e.getMessage());
        }
    }
}