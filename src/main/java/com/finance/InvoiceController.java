package com.finance;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
@CrossOrigin(origins = "*")
public class InvoiceController {

    private final IngestionService ingestionService;
    private final InvoiceRepository invoiceRepository;

    public InvoiceController(IngestionService ingestionService, InvoiceRepository invoiceRepository) {
        this.ingestionService = ingestionService;
        this.invoiceRepository = invoiceRepository;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> receiveInvoices(@RequestBody List<Map<String, Object>> payload) {
        int saved = ingestionService.ingestInvoicesFromJson(payload);
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "received", payload.size(),
                "saved", saved
        ));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Invoice>> recentInvoices() {
        return ResponseEntity.ok(invoiceRepository.findTop50ByOrderByCreatedAtDesc());
    }
}
