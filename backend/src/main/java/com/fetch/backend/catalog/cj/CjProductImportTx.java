package com.fetch.backend.catalog.cj;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fetch.backend.catalog.entity.Category;
import com.fetch.backend.catalog.entity.Product;
import com.fetch.backend.catalog.entity.ProductImage;
import com.fetch.backend.catalog.entity.ProductVariant;
import com.fetch.backend.catalog.repo.CategoryRepository;
import com.fetch.backend.catalog.repo.ProductImageRepository;
import com.fetch.backend.catalog.repo.ProductRepository;
import com.fetch.backend.catalog.repo.ProductVariantRepository;
import com.fetch.backend.common.error.NotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class CjProductImportTx {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final ObjectMapper mapper;

    public CjProductImportTx(CategoryRepository categoryRepository, ProductRepository productRepository,
                             ProductVariantRepository variantRepository, ProductImageRepository imageRepository, ObjectMapper mapper) {
        this.categoryRepository = categoryRepository; this.productRepository = productRepository;
        this.variantRepository = variantRepository; this.imageRepository = imageRepository; this.mapper = mapper;
    }

    @Transactional
    public Long importOne(Long categoryId, CjDropshippingClient.CjMyProduct item, CjDropshippingClient.CjProductDetail detail) {
        // race-safe dedupe
        if (item.productId() != null && productRepository.findFirstByCjProductIdIgnoreCase(item.productId()).isPresent()) {
            return productRepository.findFirstByCjProductIdIgnoreCase(item.productId()).get().getId();
        }
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new NotFoundException("Category not found"));
        Product p = new Product();
        p.setCategory(category);
        String name = firstNonBlank(detail != null ? detail.nameEn() : null, item.nameEn(), "CJ Product " + item.productId());
        p.setName(trunc(name, 255));
        p.setSlug(uniqueProductSlug(slugify(name, item.productId())));
        String desc = detail != null ? detail.description() : null;
        p.setDescription(blankToNull(desc));
        BigDecimal base = firstNonNull(detail != null ? detail.sellPrice() : null, item.sellPrice(), deriveVariantBase(detail));
        p.setBasePriceAmount(base != null ? base : BigDecimal.ZERO);
        p.setCurrency("USD");
        p.setStatus("ACTIVE");
        p.setCjProductId(blankToNull(item.productId()));
        p.setSupplierSource("CJ_API");
        if (detail != null && blankToNull(detail.deliveryCycle()) != null) p.setEstimatedDelivery(detail.deliveryCycle().trim() + " days");
        p = productRepository.save(p);

        String productKeyEn = detail != null ? detail.productKeyEn() : null;
        List<CjDropshippingClient.CjDetailVariant> variants = detail != null && detail.variants() != null && !detail.variants().isEmpty()
                ? detail.variants() : List.of();
        AtomicInteger sort = new AtomicInteger(0);
        if (variants.isEmpty()) {
            ProductVariant v = new ProductVariant();
            v.setProduct(p);
            v.setSku(uniqueSku(generateSku(p.getId(), item.productId(), item.vid(), 1)));
            v.setTitle("Default");
            v.setPriceAmount(p.getBasePriceAmount());
            v.setCurrency("USD");
            v.setCjVariantId(blankToNull(item.vid()));
            v.setActive(true);
            v = variantRepository.save(v);
        } else {
            int idx = 1;
            for (CjDropshippingClient.CjDetailVariant dv : variants) {
                ProductVariant v = new ProductVariant();
                v.setProduct(p);
                String sku = blankToNull(dv.sku());
                if (sku == null) sku = generateSku(p.getId(), item.productId(), dv.vid(), idx);
                v.setSku(uniqueSku(sku));
                v.setTitle(blankToNull(firstNonBlank(dv.variantKey(), dv.nameEn(), "Variant " + idx)));
                v.setOptionValuesJson(optionJson(productKeyEn, dv.variantKey()));
                v.setPriceAmount(firstNonNull(dv.sellPrice(), p.getBasePriceAmount(), BigDecimal.ZERO));
                v.setCurrency("USD");
                v.setCjVariantId(blankToNull(dv.vid()));
                if (dv.weight() != null) { try { v.setWeightGrams(dv.weight().intValue()); } catch (Exception ignored) {} }
                v.setDimensionsJson(dimensionsJson(dv.lengthMm(), dv.widthMm(), dv.heightMm()));
                v.setActive(true);
                v = variantRepository.save(v);
                if (dv.image() != null && !dv.image().isBlank()) {
                    ProductImage vi = new ProductImage();
                    vi.setProductId(p.getId()); vi.setVariantId(v.getId());
                    vi.setUrl(dv.image().trim()); vi.setAltText((p.getName() + " - " + (v.getTitle() == null ? "" : v.getTitle())).trim());
                    vi.setSortOrder(sort.getAndIncrement());
                    imageRepository.save(vi);
                }
                idx++;
            }
        }

        List<String> images = detail != null && detail.images() != null && !detail.images().isEmpty() ? detail.images()
                : (item.bigImage() != null && !item.bigImage().isBlank() ? List.of(item.bigImage()) : List.of());
        for (String url : images) {
            if (url == null || url.isBlank()) continue;
            ProductImage img = new ProductImage();
            img.setProductId(p.getId());
            img.setUrl(url.trim());
            img.setAltText(p.getName());
            img.setSortOrder(sort.getAndIncrement());
            imageRepository.save(img);
        }
        return p.getId();
    }

    private String optionJson(String productKeyEn, String variantKey) {
        if ((productKeyEn == null || productKeyEn.isBlank()) && (variantKey == null || variantKey.isBlank())) return null;
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
            return m.isEmpty() ? null : mapper.writeValueAsString(m);
        } catch (Exception e) { return null; }
    }

    private String dimensionsJson(Integer l, Integer w, Integer h) {
        if (l == null && w == null && h == null) return null;
        Map<String, Object> m = new LinkedHashMap<>();
        if (l != null) m.put("lengthMm", l); if (w != null) m.put("widthMm", w); if (h != null) m.put("heightMm", h);
        try { return mapper.writeValueAsString(m); } catch (Exception e) { return null; }
    }

    private BigDecimal deriveVariantBase(CjDropshippingClient.CjProductDetail detail) {
        if (detail == null || detail.variants() == null) return null;
        for (var v : detail.variants()) if (v.sellPrice() != null) return v.sellPrice();
        return null;
    }
    private String uniqueProductSlug(String base) {
        String slug = base == null || base.isBlank() ? "cj-product" : base;
        String candidate = slug; int n = 2;
        while (productRepository.findBySlug(candidate).isPresent()) candidate = slug + "-" + n++;
        return candidate;
    }
    private String uniqueSku(String base) {
        String candidate = base; int n = 2;
        while (variantRepository.findBySkuIgnoreCase(candidate).isPresent()) candidate = base + "-" + n++;
        return candidate;
    }
    private String generateSku(Long productId, String cjProductId, String cjVariantId, int idx) {
        String ref = blankToNull(cjVariantId) != null ? cjVariantId : (blankToNull(cjProductId) != null ? cjProductId : String.valueOf(productId));
        return ("CJ-" + ref + "-" + idx).replaceAll("[^A-Za-z0-9-]", "").toUpperCase(Locale.ROOT);
    }
    private String slugify(String title, String cjId) {
        String base = title == null ? "" : Normalizer.normalize(title, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        base = base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isBlank()) base = "cj-product";
        if (cjId != null && !cjId.isBlank()) base = base + "-" + cjId.replaceAll("[^A-Za-z0-9]+", "-").toLowerCase(Locale.ROOT);
        return base.length() > 170 ? base.substring(0, 170) : base;
    }
    private static String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
    private static String trunc(String s, int max) { return s == null ? null : (s.length() <= max ? s : s.substring(0, max)); }
    private static String firstNonBlank(String... vals) { for (String v : vals) if (v != null && !v.isBlank()) return v.trim(); return null; }
    private static BigDecimal firstNonNull(BigDecimal... vals) { for (BigDecimal v : vals) if (v != null) return v; return null; }
}
