package com.fetch.backend.payment.web;

import com.fetch.backend.payment.service.PaymentService;
import com.fetch.backend.security.UserPrincipal;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/paypal")
public class PaymentController {
    private final PaymentService paymentService;
    private final com.fetch.backend.payment.service.PayPalProperties payPalProperties;

    public PaymentController(PaymentService paymentService, com.fetch.backend.payment.service.PayPalProperties payPalProperties) {
        this.paymentService = paymentService;
        this.payPalProperties = payPalProperties;
    }

    public record CreateRequest(@NotBlank String orderNumber) {}
    public record CaptureRequest(@NotBlank String orderNumber, @NotBlank String paypalOrderId) {}

    @GetMapping("/config")
    public Map<String, Object> config() {
        return Map.of(
                "enabled", payPalProperties.isConfigured(),
                "mode", payPalProperties.getMode() == null ? "sandbox" : payPalProperties.getMode(),
                "clientId", payPalProperties.isConfigured() && payPalProperties.getClientId() != null ? payPalProperties.getClientId() : "",
                "currency", "USD"
        );
    }

    @PostMapping("/create")
    public Map<String, Object> create(@AuthenticationPrincipal UserPrincipal principal, @RequestBody CreateRequest request) {
        return paymentService.createPayPalOrder(request.orderNumber(), principal == null ? null : principal.getId());
    }

    @PostMapping("/capture")
    public Map<String, Object> capture(@AuthenticationPrincipal UserPrincipal principal, @RequestBody CaptureRequest request) {
        return paymentService.capturePayPalOrder(request.orderNumber(), request.paypalOrderId(), principal == null ? null : principal.getId());
    }
}
