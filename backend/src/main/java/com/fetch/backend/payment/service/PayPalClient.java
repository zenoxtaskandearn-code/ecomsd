package com.fetch.backend.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fetch.backend.common.error.BadRequestException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
@EnableConfigurationProperties(PayPalProperties.class)
public class PayPalClient {
    private final PayPalProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public PayPalClient(PayPalProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().baseUrl(properties.getBaseUrl()).build();
    }

    public void requireConfigured() {
        if (!properties.isConfigured()) {
            throw new BadRequestException("PayPal is not configured. Set PAYPAL_CLIENT_ID and PAYPAL_CLIENT_SECRET.");
        }
    }

    public String getAccessToken() {
        requireConfigured();
        String auth = Base64.getEncoder().encodeToString((properties.getClientId() + ":" + properties.getClientSecret()).getBytes(StandardCharsets.UTF_8));
        String body = restClient.post()
                .uri("/v1/oauth2/token")
                .header("Authorization", "Basic " + auth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=client_credentials")
                .retrieve()
                .body(String.class);
        return readTree(body).path("access_token").asText();
    }

    public JsonNode createOrder(String internalOrderNumber, BigDecimal amount, String currency) {
        String token = getAccessToken();
        Map<String, Object> payload = Map.of(
                "intent", "CAPTURE",
                "purchase_units", new Object[]{Map.of(
                        "reference_id", internalOrderNumber,
                        "custom_id", internalOrderNumber,
                        "amount", Map.of("currency_code", currency, "value", amount.toPlainString())
                )}
        );
        String body = restClient.post()
                .uri("/v2/checkout/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);
        return readTree(body);
    }

    public JsonNode captureOrder(String paypalOrderId) {
        String token = getAccessToken();
        String body = restClient.post()
                .uri("/v2/checkout/orders/{id}/capture", paypalOrderId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{}")
                .retrieve()
                .body(String.class);
        return readTree(body);
    }

    public JsonNode getOrder(String paypalOrderId) {
        String token = getAccessToken();
        String body = restClient.get()
                .uri("/v2/checkout/orders/{id}", paypalOrderId)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(String.class);
        return readTree(body);
    }

    public boolean verifyWebhookSignature(Map<String, String> headers, String rawBody) {
        requireConfigured();
        if (properties.getWebhookId() == null || properties.getWebhookId().isBlank()) {
            throw new BadRequestException("PAYPAL_WEBHOOK_ID is not configured; cannot verify webhook.");
        }
        String token = getAccessToken();
        Map<String, Object> payload = Map.of(
                "auth_algo", headers.getOrDefault("paypal-auth-algo", ""),
                "cert_url", headers.getOrDefault("paypal-cert-url", ""),
                "transmission_id", headers.getOrDefault("paypal-transmission-id", ""),
                "transmission_sig", headers.getOrDefault("paypal-transmission-sig", ""),
                "transmission_time", headers.getOrDefault("paypal-transmission-time", ""),
                "webhook_id", properties.getWebhookId(),
                "webhook_event", readTree(rawBody)
        );
        String body = restClient.post()
                .uri("/v1/notifications/verify-webhook-signature")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);
        return "SUCCESS".equalsIgnoreCase(readTree(body).path("verification_status").asText());
    }

    private JsonNode readTree(String json) {
        try { return objectMapper.readTree(json == null ? "{}" : json); }
        catch (Exception e) { throw new BadRequestException("Invalid PayPal response"); }
    }
}
