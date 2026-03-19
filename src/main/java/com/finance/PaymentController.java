package com.finance;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final IngestionService ingestionService;
    private final PaymentRepository paymentRepository;

    public PaymentController(IngestionService ingestionService, PaymentRepository paymentRepository) {
        this.ingestionService = ingestionService;
        this.paymentRepository = paymentRepository;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> receivePayments(@RequestBody List<Map<String, Object>> payload) {
        int saved = ingestionService.ingestPaymentsFromJson(payload);
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "received", payload.size(),
                "saved", saved
        ));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Payment>> recentPayments() {
        return ResponseEntity.ok(paymentRepository.findTop50ByOrderByCreatedAtDesc());
    }
}
