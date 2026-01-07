package com.finance;

import java.util.Arrays;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ReconciliationApplication {

    private final IngestionService ingestionService;
    private final ReconciliationService reconciliationService;
    private final ReportingService reportingService;

    public ReconciliationApplication(IngestionService ingestionService,
                                     ReconciliationService reconciliationService,
                                     ReportingService reportingService) {
        this.ingestionService = ingestionService;
        this.reconciliationService = reconciliationService;
        this.reportingService = reportingService;
    }

    public static void main(String[] args) {
        SpringApplication.run(ReconciliationApplication.class, args);
    }

    @Bean
    public CommandLineRunner run() {
        return args -> {
            if (args.length == 0) {
                printUsage();
                return;
            }

            String command = args[0];
            try {
                switch (command) {
                    case "ingest-invoices" -> {
                        validateArgs(args, 2);
                        ingestionService.ingestInvoices(args[1]);
                    }
                    case "ingest-payments" -> {
                        validateArgs(args, 2);
                        ingestionService.ingestPayments(args[1]);
                    }
                    case "reconcile" -> {
                        int batchSize = extractBatchSize(args);
                        reconciliationService.runReconciliation(batchSize);
                    }
                    case "show-unmatched" -> reportingService.showUnmatched();
                    case "report" -> reportingService.generateDailyReport();
                    case "audit-log" -> reportingService.showAuditLog();
                    
                    // 🌟 NEW: ALL-IN-ONE COMMAND 🌟
                    case "run-all" -> {
                        System.out.println("🚀 STARTING FULL RECONCILIATION CYCLE...");
                        
                        // 1. Ingest (Assumes files are named invoices.csv and payments.csv)
                        ingestionService.ingestInvoices("invoices.csv");
                        ingestionService.ingestPayments("payments.csv");

                        // 2. Reconcile
                        reconciliationService.runReconciliation(100);

                        // 3. Report
                        reportingService.generateDailyReport();
                        
                        System.out.println("🏁 CYCLE COMPLETE.");
                    }
                    
                    default -> {
                        System.err.println("❌ Unknown command: " + command);
                        printUsage();
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Error executing command: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }

    private void validateArgs(String[] args, int expected) {
        if (args.length < expected) {
            throw new IllegalArgumentException("Missing required arguments.");
        }
    }

    private int extractBatchSize(String[] args) {
        return Arrays.stream(args)
                .filter(arg -> arg.startsWith("--batch-size="))
                .map(arg -> Integer.parseInt(arg.split("=")[1]))
                .findFirst()
                .orElse(100); 
    }

    private void printUsage() {
        System.out.println("""
            
            💰 Automated Payment Reconciliation System (CLI)
            ------------------------------------------------
            Usage:
              java -jar app.jar run-all                <-- DO EVERYTHING
              java -jar app.jar ingest-invoices <file>
              java -jar app.jar ingest-payments <file>
              java -jar app.jar reconcile
              java -jar app.jar report
            """);
    }
}