package com.fetch.backend.catalog.cj;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fetch.backend.catalog.entity.Product;
import com.fetch.backend.catalog.entity.ProductImage;
import com.fetch.backend.catalog.entity.ProductVariant;
import com.fetch.backend.catalog.repo.ProductImageRepository;
import com.fetch.backend.catalog.repo.ProductRepository;
import com.fetch.backend.catalog.repo.ProductVariantRepository;
import com.fetch.backend.common.error.BadRequestException;
import com.fetch.backend.common.error.NotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class CjProductRefreshTx {
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final ObjectMapper mapper;

    public CjProductRefreshTx(ProductRepository productRepository, ProductVariantRepository variantRepository,
                              ProductImageRepository imageRepository, ObjectMapper mapper) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.imageRepository = imageRepository;
        this.mapper = mapper;
    }

    @Transactional
    public Map<String, Object> refresh(Long productId, CjDropshippingClient.CjProductDetail detail) {
        if (detail == null) throw new BadRequestException("CJ returned no product detail to refresh.");
        Product p = productRepository.findById(productId).orElseThrow(() -> new NotFoundException("Product not found"));
        if (p.getCjProductId() == null || p.getCjProductId().isBlank()) throw new BadRequestException("This product has no CJ product ID.");
        // Safe merge: DO NOT overwrite admin-set selling prices, status, category, supplier costs/shipping cost, internal notes, SEO.
        if (detail.nameEn() != null && !detail.nameEn().isBlank()) p.setName(trunc(detail.nameEn().trim(), 255));
        if (detail.description() != null && !detail.description().isBlank()) p.setDescription(detail.description());
        if (detail.deliveryCycle() != null && !detail.deliveryCycle().isBlank()) p.setEstimatedDelivery(detail.deliveryCycle().trim() + " days");
        if (p.getSupplierSource() == null || p.getSupplierSource().isBlank()) p.setSupplierSource("CJ_API");
        productRepository.save(p);

        List<ProductVariant> existing = new ArrayList<>(variantRepository.findByProductId(p.getId()));
        Map<String, ProductVariant> byCjVid = new HashMap<>();
        for (ProductVariant v : existing) if (v.getCjVariantId() != null && !v.getCjVariantId().isBlank()) byCjVid.put(v.getCjVariantId().trim(), v);
        List<CjDropshippingClient.CjDetailVariant> cjVariants = detail.variants() == null ? List.of() : detail.variants();
        int created = 0, updated = 0;
        AtomicInteger sort = new AtomicInteger(maxSort(p.getId()) + 1);
        for (CjDropshippingClient.CjDetailVariant dv : cjVariants) {
            String vid = blankToNull(dv.vid());
            ProductVariant v = vid != null ? byCjVid.get(vid) : null;
            if (v == null) {
                // try match by SKU (careful)
                String sku = blankToNull(dv.sku());
                if (sku != null) v = existing.stream().filter(x -> sku.equalsIgnoreCase(x.getSku()) && (x.getCjVariantId() == null || x.getCjVariantId().isBlank() || x.getCjVariantId().equalsIgnoreCase(vid))).findFirst().orElse(null);
            }
            if (v == null) {
                v = new ProductVariant();
                v.setProduct(p);
                v.setSku(uniqueSku(generateSku(p.getId(), p.getCjProductId(), vid, created + updated + 1)));
                v.setPriceAmount(firstNonNull(dv.sellPrice(), p.getBasePriceAmount(), BigDecimal.ZERO)); // default only on create
                v.setCurrency(p.getCurrency() == null ? "USD" : p.getCurrency());
                v.setActive(true); // draft/review may still apply; status is on product
                created++;
            } else {
                updated++;
            }
            v.setCjVariantId(vid);
            if (dv.sku() != null && !dv.sku().isBlank() && (v.getSupplierSku() == null || v.getSupplierSku().isBlank())) v.setSupplierSku(dv.sku().trim());
            if (dv.variantKey() != null || dv.nameEn() != null) v.setTitle(blankToNull(firstNonBlank(dv.variantKey(), dv.nameEn(), v.getTitle())));
            v.setOptionValuesJson(optionJson(detail.productKeyEn(), dv.variantKey(), v.getOptionValuesJson()));
            if (dv.weight() != null) { try { v.setWeightGrams(dv.weight().intValue()); } catch (Exception ignored) {} }
            String dims = dimensionsJson(dv.lengthMm(), dv.widthMm(), dv.heightMm());
            if (dims != null) v.setDimensionsJson(dims);
            // preserve: priceAmount, supplierCostAmount, shippingCostAmount, active, currency
            v = variantRepository.save(v);
            if (vid != null) byCjVid.put(vid, v);
            upsertVariantImage(p, v, dv.image(), sort);
        }

        // Product images: add CJ URLs not already present (do not delete existing/manual)
        List<ProductImage> currentImages = imageRepository.findByProductIdOrderBySortOrderAscIdAsc(p.getId());
        Set<String> urls = new HashSet<>();
        for (ProductImage i : currentImages) if (i.getUrl() != null) urls.add(normUrl(i.getUrl()));
        if (detail.images() != null) {
            for (String url : detail.images()) {
                if (url == null || url.isBlank()) continue;
                String n = normUrl(url);
                if (urls.contains(n)) continue;
                ProductImage img = new ProductImage();
                img.setProductId(p.getId());
                img.setUrl(url.trim());
                img.setAltText(p.getName());
                img.setSortOrder(sort.getAndIncrement());
                imageRepository.save(img);
                urls.add(n);
            }
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("productId", p.getId());
        res.put("cjProductId", p.getCjProductId());
        res.put("variantsUpdated", updated);
        res.put("variantsCreated", created);
        res.put("message", "Refreshed from CJ. Selling prices, status, category, supplier/internal costs, and internal notes were preserved.");
        return res;
    }

    private void upsertVariantImage(Product p, ProductVariant v, String url, AtomicInteger sort) {
        if (url == null || url.isBlank()) return;
        List<ProductImage> imgs = imageRepository.findByProductIdOrderBySortOrderAscIdAsc(p.getId());
        String n = normUrl(url);
        for (ProductImage i : imgs) {
            if (i.getVariantId() != null && i.getVariantId().equals(v.getId())) {
                if (i.getUrl() == null || i.getUrl().isBlank() || normUrl(i.getUrl()).equals(n)) { i.setUrl(url.trim()); i.setAltText((p.getName()+" - "+nz(v.getTitle())).trim()); imageRepository.save(i); return; }
            }
        }
        // if same URL exists as product-level image without variant, don't duplicate; otherwise add linked
        for (ProductImage i : imgs) if (i.getUrl() != null && normUrl(i.getUrl()).equals(n)) return;
        ProductImage vi = new ProductImage();
        vi.setProductId(p.getId()); vi.setVariantId(v.getId()); vi.setUrl(url.trim());
        vi.setAltText((p.getName()+" - "+nz(v.getTitle())).trim()); vi.setSortOrder(sort.getAndIncrement());
        imageRepository.save(vi);
    }

    private int maxSort(Long productId) {
        int m = 0; for (ProductImage i : imageRepository.findByProductIdOrderBySortOrderAscIdAsc(productId)) m = Math.max(m, i.getSortOrder()); return m;
    }
    private String optionJson(String productKeyEn, String variantKey, String fallback) {
        if ((productKeyEn == null || productKeyEn.isBlank()) && (variantKey == null || variantKey.isBlank())) return fallback;
        try {
            Map<String, String> m = new LinkedHashMap<>();
            String[] names = productKeyEn != null && !productKeyEn.isBlank() ? productKeyEn.split("-") : new String[0];
            String[] values = variantKey != null && !variantKey.isBlank() ? variantKey.split("-") : new String[0];
            int n = Math.max(names.length, values.length);
            for (int i = 0; i < n; i++) {
                String name = i < names.length ? names[i].trim() : ("Option " + (i + 1));
                String val = i < values.length ? values[i].trim() : "";
                if (!name.isBlank() && !val.isBlank()) m.put(name, val);
                else if (!val.isBlank()) m.put("Option " + (i + 1), val);
            }
            if (m.isEmpty() && variantKey != null && !variantKey.isBlank()) m.put("Variant", variantKey.trim());
            return m.isEmpty() ? fallback : mapper.writeValueAsString(m);
        } catch (Exception e) { return fallback; }
    }
    private String dimensionsJson(Integer l, Integer w, Integer h) {
        if (l == null && w == null && h == null) return null;
        Map<String, Object> m = new LinkedHashMap<>();
        if (l != null) m.put("lengthMm", l); if (w != null) m.put("widthMm", w); if (h != null) m.put("heightMm", h);
        try { return mapper.writeValueAsString(m); } catch (Exception e) { return null; }
    }
    private String uniqueSku(String base) { String c = base; int n = 2; while (variantRepository.findBySkuIgnoreCase(c).isPresent()) c = base + "-" + n++; return c; }
    private String generateSku(Long productId, String cjProductId, String cjVariantId, int idx) {
        String ref = blankToNull(cjVariantId) != null ? cjVariantId : (blankToNull(cjProductId) != null ? cjProductId : String.valueOf(productId));
        return ("CJ-" + ref + "-" + idx).replaceAll("[^A-Za-z0-9-]", "").toUpperCase(Locale.ROOT);
    }
    private static String normUrl(String u) { return u == null ? "" : u.trim().split("\\?")[0].toLowerCase(Locale.ROOT); }
    private static String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
    private static String nz(String s) { return s == null ? "" : s; }
    private static String trunc(String s, int max) { return s == null ? null : (s.length() <= max ? s : s.substring(0, max)); }
    private static String firstNonBlank(String... vals) { for (String v : vals) if (v != null && !v.isBlank()) return v.trim(); return null; }
    private static BigDecimal firstNonNull(BigDecimal... vals) { for (BigDecimal v : vals) if (v != null) return v; return null; }
}
