package com.fetch.backend.payment.web;

import com.fetch.backend.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/webhooks/paypal")
public class PayPalWebhookController {
    private final PaymentService paymentService;

    public PayPalWebhookController(PaymentService paymentService) { this.paymentService = paymentService; }

    @PostMapping
    public ResponseEntity<Map<String, String>> handle(@RequestHeader Map<String, String> headers, @RequestBody String rawBody) {
        Map<String, String> normalized = headers.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toLowerCase(), Map.Entry::getValue, (a, b) -> a));
        paymentService.handleWebhook(normalized, rawBody);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
