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

    @Transactional
    public void ingestInvoices(String filePath) {
        System.out.println("⏳ Reading Invoices from: " + filePath);
        int success = 0;
        int skipped = 0;
        List<Invoice> batch = new ArrayList<>();

        try (Reader reader = new FileReader(filePath);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : csvParser) {
                try {
                    String number = record.get("invoice_number");
                    BigDecimal amount = new BigDecimal(record.get("amount"));
                    LocalDate date = LocalDate.parse(record.get("date"));

                    batch.add(new Invoice(number, amount, date));
                    success++;
                } catch (Exception e) {
                    skipped++;
                }
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
        int success = 0;
        int skipped = 0;
        List<Payment> batch = new ArrayList<>();

        try (Reader reader = new FileReader(filePath);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : csvParser) {
                try {
                    String txId = record.get("transaction_id");
                    BigDecimal amount = new BigDecimal(record.get("amount"));
                    LocalDate date = LocalDate.parse(record.get("date"));
                    String ref = record.get("reference");

                    batch.add(new Payment(txId, amount, date, ref));
                    success++;
                } catch (Exception e) {
                    skipped++;
                }
            }
            paymentRepository.saveAll(batch);
            auditLogRepository.save(new AuditLog("INGEST_PAYMENTS", "Success: " + success + ", Skipped: " + skipped));
            
            System.out.printf("✔ Ingestion Complete.%n   Processed: %d%n   Skipped: %d%n", success, skipped);

        } catch (Exception e) {
            throw new RuntimeException("Failed to ingest payments: " + e.getMessage());
        }
    }
}