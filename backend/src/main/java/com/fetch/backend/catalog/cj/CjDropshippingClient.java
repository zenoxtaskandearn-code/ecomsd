package com.fetch.backend.catalog.cj;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fetch.backend.common.error.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
public class CjDropshippingClient {
    private static final String BASE = "https://developers.cjdropshipping.com/api2.0/v1";
    private final RestClient rest;
    private final ObjectMapper mapper;
    private final boolean enabled;
    private final String apiKey;
    private String accessToken;
    private String openId;
    private Instant tokenExpiry = Instant.EPOCH;
    private final Object tokenLock = new Object();

    private final String orderCreatePath;

    public CjDropshippingClient(RestClient.Builder builder, ObjectMapper mapper,
                                @Value("${CJ_API_ENABLED:false}") boolean enabled,
                                @Value("${CJ_API_KEY:}") String apiKey,
                                @Value("${CJ_ORDER_CREATE_PATH:/shopping/order/createOrderV2}") String orderCreatePath) {
        this.rest = builder.baseUrl(BASE).build();
        this.mapper = mapper;
        this.enabled = enabled;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        String p = orderCreatePath == null ? "/shopping/order/createOrderV2" : orderCreatePath.trim();
        this.orderCreatePath = p.startsWith("/") ? p : ("/" + p);
    }

    public boolean isConfigured() { return enabled && !apiKey.isBlank(); }

    public String getAccessToken() {
        if (!isConfigured()) throw new BadRequestException("CJdropshipping API is not configured. Set CJ_API_ENABLED=true and CJ_API_KEY.");
        synchronized (tokenLock) {
            if (accessToken != null && Instant.now().isBefore(tokenExpiry.minusSeconds(300))) return accessToken;
            Map<String, String> body = Map.of("apiKey", apiKey);
            JsonNode root = post("/authentication/getAccessToken", body, null);
            JsonNode data = requireData(root);
            accessToken = text(data, "accessToken");
            openId = text(data, "openId");
            if (accessToken == null || accessToken.isBlank()) throw new BadRequestException("CJ authentication failed: no access token returned.");
            tokenExpiry = Instant.now().plus(Duration.ofDays(150)); // CJ tokens last ~180 days; refresh safely early
            String exp = text(data, "accessTokenExpiryDate");
            // keep conservative expiry; CJ returns date string but format varies
            return accessToken;
        }
    }

    public String getOpenId() {
        if (!isConfigured()) return null;
        synchronized (tokenLock) {
            if ((accessToken == null || Instant.now().isAfter(tokenExpiry.minusSeconds(300))) && (openId == null || openId.isBlank())) {
                try { getAccessToken(); } catch (Exception ignored) {}
            }
            return openId;
        }
    }

    public record CjMyProduct(String productId, String nameEn, String sku, String bigImage, BigDecimal sellPrice, String vid) {}
    public record CjMyProductPage(List<CjMyProduct> items, int pageNumber, int totalPages, long totalRecords) {}

    public CjMyProductPage listMyProducts(int pageNum, int pageSize, String keyword) {
        String token = getAccessToken();
        // Official endpoint: GET /product/myProduct/query. Send common pagination aliases for CJ API variants.
        int pn = Math.max(1, pageNum);
        int ps = Math.min(100, Math.max(1, pageSize));
        StringBuilder path = new StringBuilder("/product/myProduct/query?pageNum=").append(pn)
                .append("&pageSize=").append(ps)
                .append("&pageNumber=").append(pn)
                .append("&page=").append(pn)
                .append("&size=").append(ps);
        if (keyword != null && !keyword.isBlank()) path.append("&productNameEn=").append(urlEnc(keyword.trim())).append("&keyword=").append(urlEnc(keyword.trim()));
        JsonNode root = get(path.toString(), token);
        JsonNode data = requireData(root);
        List<CjMyProduct> items = new ArrayList<>();
        JsonNode content = data.path("content");
        if (!content.isArray()) content = data.path("list");
        if (!content.isArray()) content = data.path("records");
        if (content.isArray()) {
            for (JsonNode n : content) {
                String pid = firstText(n, "productId", "pid", "id");
                if (pid == null || pid.isBlank()) continue;
                items.add(new CjMyProduct(pid,
                        firstText(n, "nameEn", "name", "productNameEn", "productName"),
                        firstText(n, "sku", "productSku", "spu"),
                        firstText(n, "bigImage", "productImage", "image"),
                        decimal(n, "sellPrice", "discountPrice", "nowPrice", "totalPrice", "productSellPrice", "minPrice", "maxPrice", "sourceFromPrice", "price"),
                        firstText(n, "vid", "variantId")));
            }
        }
        int rpn = data.path("pageNumber").asInt(data.path("pageNum").asInt(data.path("page").asInt(pageNum)));
        int tp = data.path("totalPages").asInt(data.path("pages").asInt(1));
        if (tp <= 1 && data.has("total")) { long total = data.path("total").asLong(0); if (total > ps) tp = (int) Math.ceil(total / (double) ps); }
        long tr = data.path("totalRecords").asLong(data.path("total").asLong(data.path("count").asLong(items.size())));
        return new CjMyProductPage(items, rpn, tp, tr);
    }

    public record CjDetailVariant(String vid, String sku, String nameEn, String variantKey, BigDecimal sellPrice,
                                  BigDecimal weight, Integer lengthMm, Integer widthMm, Integer heightMm, String image) {}
    public record CjProductDetail(String pid, String nameEn, String sku, String description, BigDecimal sellPrice,
                                  String productKeyEn, String deliveryCycle, String suggestSellPrice,
                                  List<String> images, List<CjDetailVariant> variants) {}

    // ---- Order / fulfillment (official CJ shopping/logistic APIs) ----
    public record CjOrderProduct(String vid, int quantity) {}
    public record CjCreateOrderRequest(
            String orderNumber, String shippingCountryCode, String shippingCountry, String shippingProvince,
            String shippingCity, String shippingAddress, String shippingAddress2, String shippingCustomerName,
            String shippingPhone, String shippingZip, String email, String remark, String logisticName,
            String fromCountryCode, List<CjOrderProduct> products
    ) {}
    public record CjOrderResult(String cjOrderId, String cjOrderNumber, String status, JsonNode raw) {}
    public record CjTrackingInfo(String trackingNumber, String logisticName, String trackingUrl, String trackingStatus, String deliveryStatus, JsonNode raw) {}

    public CjProductDetail getProductDetail(String pid) {
        String token = getAccessToken();
        JsonNode root = get("/product/query?pid=" + urlEnc(pid) + "&features=enable_description", token);
        JsonNode d = requireData(root);
        List<String> images = new ArrayList<>();
        String big = firstText(d, "bigImage", "productImage");
        if (big != null && !big.isBlank()) images.add(big);
        JsonNode set = d.path("productImageSet");
        if (set.isArray()) for (JsonNode n : set) if (n.isTextual() && !n.asText().isBlank() && !images.contains(n.asText())) images.add(n.asText());
        if (set.isTextual() && set.asText().startsWith("[")) { // sometimes stringified
            try { JsonNode arr = mapper.readTree(set.asText()); if (arr.isArray()) for (JsonNode n : arr) if (n.isTextual() && !n.asText().isBlank() && !images.contains(n.asText())) images.add(n.asText()); } catch (Exception ignored) {}
        }
        List<CjDetailVariant> vars = new ArrayList<>();
        JsonNode variants = d.path("variants");
        if (variants.isArray()) {
            for (JsonNode v : variants) {
                vars.add(new CjDetailVariant(
                        firstText(v, "vid", "variantId"),
                        firstText(v, "variantSku", "sku"),
                        firstText(v, "variantNameEn", "variantName", "nameEn"),
                        firstText(v, "variantKey"),
                        decimal(v, "variantSellPrice", "variantSugSellPrice", "sellPrice"),
                        decimal(v, "variantWeight", "weight"),
                        intOrNull(v, "variantLength"), intOrNull(v, "variantWidth"), intOrNull(v, "variantHeight"),
                        firstText(v, "variantImage", "image")));
            }
        }
        return new CjProductDetail(
                firstText(d, "pid", "productId", "id"),
                firstText(d, "nameEn", "productNameEn", "productName", "name"),
                firstText(d, "productSku", "sku", "spu"),
                firstText(d, "description", "descriptionEn"),
                decimal(d, "sellPrice", "nowPrice", "discountPrice"),
                firstText(d, "productKeyEn"),
                firstText(d, "deliveryCycle"),
                firstText(d, "suggestSellPrice"),
                images, vars);
    }

    public CjOrderResult createOrder(CjCreateOrderRequest req) {
        if (!isConfigured()) throw new BadRequestException("CJdropshipping API is not configured. Set CJ_API_ENABLED=true and CJ_API_KEY.");
        if (req == null || req.products() == null || req.products().isEmpty()) throw new BadRequestException("CJ order requires at least one product.");
        for (CjOrderProduct p : req.products()) {
            if (p == null || p.vid() == null || p.vid().isBlank()) throw new BadRequestException("CJ order product is missing variant id (vid). Import/sync the product variant from CJ first.");
            if (p.quantity() <= 0) throw new BadRequestException("CJ order product quantity must be positive.");
        }
        String token = getAccessToken();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderNumber", nz(req.orderNumber())); // merchant order number
        body.put("shippingCountryCode", nz(req.shippingCountryCode()));
        body.put("shippingCountry", nz(req.shippingCountry()));
        body.put("shippingProvince", nz(req.shippingProvince()));
        body.put("shippingCity", nz(req.shippingCity()));
        body.put("shippingAddress", nz(req.shippingAddress()));
        body.put("shippingAddress2", nz(req.shippingAddress2()));
        body.put("shippingCustomerName", nz(req.shippingCustomerName()));
        body.put("shippingPhone", nz(req.shippingPhone()));
        body.put("shippingZip", nz(req.shippingZip()));
        if (req.email() != null && !req.email().isBlank()) body.put("email", req.email().trim());
        if (req.remark() != null && !req.remark().isBlank()) body.put("remark", req.remark().trim());
        if (req.logisticName() != null && !req.logisticName().isBlank()) body.put("logisticName", req.logisticName().trim());
        if (req.fromCountryCode() != null && !req.fromCountryCode().isBlank()) body.put("fromCountryCode", req.fromCountryCode().trim());
        body.put("products", req.products().stream().map(p -> Map.of("vid", p.vid().trim(), "quantity", p.quantity())).toList());
        if (req.fromCountryCode() != null && !req.fromCountryCode().isBlank()) body.put("storageId", req.fromCountryCode().trim()); // some CJ variants use warehouse/storage hints; harmless if ignored
        // Official CJ shopping order create endpoint (API v2.0). Path configurable via CJ_ORDER_CREATE_PATH. Uses merchant orderNumber.
        JsonNode root = post(orderCreatePath, body, token);
        JsonNode data = root.path("data");
        if (data.isMissingNode() || data.isNull()) data = root; // be tolerant
        String cjOrderId = firstText(data, "orderId", "cjOrderId", "id", "orderNum");
        String cjOrderNum = firstText(data, "orderNumber", "orderNum", "cjOrderNumber", "cjOrderId");
        String status = firstText(data, "orderStatus", "status");
        if ((cjOrderId == null || cjOrderId.isBlank()) && (cjOrderNum == null || cjOrderNum.isBlank())) {
            // some responses nest differently
            cjOrderId = firstText(root, "orderId", "cjOrderId");
        }
        return new CjOrderResult(cjOrderId, cjOrderNum != null ? cjOrderNum : cjOrderId, status, data);
    }

    public JsonNode getOrderDetail(String cjOrderId) {
        if (!isConfigured()) throw new BadRequestException("CJdropshipping API is not configured.");
        if (cjOrderId == null || cjOrderId.isBlank()) throw new BadRequestException("CJ order id is required.");
        String token = getAccessToken();
        // Official CJ order detail endpoint.
        JsonNode root = get("/shopping/order/getOrderDetail?orderId=" + urlEnc(cjOrderId.trim()), token);
        JsonNode data = root.path("data");
        return (data.isMissingNode() || data.isNull()) ? root : data;
    }

    public CjTrackingInfo getTrackingInfo(String trackingNumber) {
        if (!isConfigured()) throw new BadRequestException("CJdropshipping API is not configured.");
        if (trackingNumber == null || trackingNumber.isBlank()) throw new BadRequestException("Tracking number is required.");
        String token = getAccessToken();
        JsonNode root = get("/logistic/getTrackInfo?trackNumber=" + urlEnc(trackingNumber.trim()), token);
        JsonNode data = root.path("data");
        JsonNode d = (data.isMissingNode() || data.isNull()) ? root : (data.isArray() && data.size() > 0 ? data.get(0) : data);
        return new CjTrackingInfo(
                firstText(d, "trackingNumber", "trackNumber", "logisticNo"),
                firstText(d, "logisticName", "logisticsName", "carrier"),
                firstText(d, "trackingUrl", "trackUrl", "trackingUrl"),
                firstText(d, "trackingStatus", "trackStatus", "status"),
                firstText(d, "deliveryStatus", "deliveryTimeStatus"),
                d);
    }

    public record CjVariantStock(String vid, Integer totalInventory, String areaEn, String countryCode, JsonNode raw) {}

    public List<CjVariantStock> getVariantStock(String vid) {
        if (!isConfigured()) throw new BadRequestException("CJdropshipping API is not configured.");
        if (vid == null || vid.isBlank()) throw new BadRequestException("CJ variant id (vid) is required.");
        String token = getAccessToken();
        // Official CJ inventory/stock query (path may vary by API version; tolerant parsing).
        JsonNode root = get("/product/stock/queryByVid?vid=" + urlEnc(vid.trim()), token);
        JsonNode data = root.path("data");
        JsonNode arr = data.isArray() ? data : (data.path("list").isArray() ? data.path("list") : (data.path("inventories").isArray() ? data.path("inventories") : data));
        List<CjVariantStock> out = new ArrayList<>();
        if (arr.isArray()) {
            for (JsonNode n : arr) out.add(new CjVariantStock(firstText(n, "vid", "variantId"), intOrNullAny(n, "totalInventoryNum", "totalInventory", "stock", "quantity"), firstText(n, "areaEn", "area", "countryCode"), firstText(n, "countryCode"), n));
        } else if (!arr.isMissingNode() && !arr.isNull() && (arr.has("vid") || arr.has("totalInventoryNum") || arr.has("stock"))) {
            out.add(new CjVariantStock(firstText(arr, "vid", "variantId"), intOrNullAny(arr, "totalInventoryNum", "totalInventory", "stock", "quantity"), firstText(arr, "areaEn", "area"), firstText(arr, "countryCode"), arr));
        }
        return out;
    }

    private JsonNode get(String path, String token) {
        try {
            String body = rest.get().uri(path)
                    .header("CJ-Access-Token", token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve().body(String.class);
            return parse(body);
        } catch (org.springframework.web.client.RestClientResponseException e) {
            throw mapHttpError(e);
        } catch (org.springframework.web.client.RestClientException e) {
            throw new BadRequestException("CJ API request failed: " + safeErr(e.getMessage()));
        }
    }

    private JsonNode post(String path, Object req, String token) {
        try {
            RestClient.RequestBodySpec spec = rest.post().uri(path).contentType(MediaType.APPLICATION_JSON);
            if (token != null) spec.header("CJ-Access-Token", token);
            String body = spec.body(req).retrieve().body(String.class);
            return parse(body);
        } catch (org.springframework.web.client.RestClientResponseException e) {
            throw mapHttpError(e);
        } catch (org.springframework.web.client.RestClientException e) {
            throw new BadRequestException("CJ API request failed: " + safeErr(e.getMessage()));
        }
    }

    private BadRequestException mapHttpError(org.springframework.web.client.RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        if (body != null && !body.isBlank()) {
            try {
                JsonNode root = mapper.readTree(body);
                String msg = firstText(root, "message", "msg");
                if (msg != null && !msg.isBlank()) return new BadRequestException("CJ API error: " + msg);
            } catch (Exception ignored) {}
        }
        return new BadRequestException("CJ API HTTP " + e.getStatusCode().value() + ": request failed.");
    }
    private static String safeErr(String m) { if (m == null) return "unknown error"; return m.length() > 160 ? m.substring(0, 160) : m; }

    private JsonNode parse(String body) {
        try { return mapper.readTree(body == null ? "{}" : body); }
        catch (Exception e) { throw new BadRequestException("Invalid CJ API response."); }
    }

    private JsonNode requireData(JsonNode root) {
        boolean ok = root.path("result").asBoolean(false) || root.path("success").asBoolean(false) || root.path("code").asInt() == 200;
        if (!ok) {
            String msg = firstText(root, "message", "msg");
            throw new BadRequestException("CJ API error: " + (msg == null ? "request failed" : msg));
        }
        JsonNode data = root.path("data");
        if (data.isMissingNode() || data.isNull()) throw new BadRequestException("CJ API returned no data.");
        return data;
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.path(field);
        if (v.isMissingNode() || v.isNull()) return null;
        String s = v.asText();
        return (s == null || s.isBlank() || "null".equalsIgnoreCase(s)) ? null : s;
    }
    private static String firstText(JsonNode n, String... fields) {
        for (String f : fields) { String s = text(n, f); if (s != null) return s; }
        return null;
    }
    private static BigDecimal decimal(JsonNode n, String... fields) {
        for (String f : fields) {
            JsonNode v = n.path(f);
            if (v.isMissingNode() || v.isNull()) continue;
            try {
                if (v.isNumber()) return v.decimalValue();
                String s = v.asText();
                if (s != null && !s.isBlank() && !"null".equalsIgnoreCase(s)) return new BigDecimal(s.trim());
            } catch (Exception ignored) {}
        }
        return null;
    }
    private static Integer intOrNull(JsonNode n, String field) {
        JsonNode v = n.path(field);
        if (v.isMissingNode() || v.isNull()) return null;
        try { if (v.isNumber()) return v.intValue(); String s = v.asText(); if (s != null && !s.isBlank()) return (int) Double.parseDouble(s); } catch (Exception ignored) {}
        return null;
    }
    private static Integer intOrNullAny(JsonNode n, String... fields) {
        for (String f : fields) { Integer v = intOrNull(n, f); if (v != null) return v; }
        return null;
    }
    private static String nz(String s) { return s == null ? "" : s.trim(); }
    private static String urlEnc(String s) { return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8); }
}
