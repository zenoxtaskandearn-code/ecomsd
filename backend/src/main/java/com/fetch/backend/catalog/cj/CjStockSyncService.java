package com.fetch.backend.catalog.cj;

import com.fetch.backend.catalog.entity.Product;
import com.fetch.backend.catalog.entity.ProductVariant;
import com.fetch.backend.catalog.repo.ProductRepository;
import com.fetch.backend.catalog.repo.ProductVariantRepository;
import com.fetch.backend.common.error.BadRequestException;
import com.fetch.backend.common.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CjStockSyncService {
    private final CjDropshippingClient cj;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;

    public CjStockSyncService(CjDropshippingClient cj, ProductRepository productRepository, ProductVariantRepository variantRepository) {
        this.cj = cj; this.productRepository = productRepository; this.variantRepository = variantRepository;
    }

    @Transactional
    public Map<String, Object> syncProductStock(Long productId) {
        if (!cj.isConfigured()) throw new BadRequestException("CJdropshipping API is not configured.");
        Product product = productRepository.findById(productId).orElseThrow(() -> new NotFoundException("Product not found"));
        if (product.getCjProductId() == null || product.getCjProductId().isBlank()) throw new BadRequestException("Product has no CJ product ID.");
        List<ProductVariant> variants = variantRepository.findByProductId(productId);
        int updated = 0, checked = 0;
        List<String> notes = new ArrayList<>();
        for (ProductVariant v : variants) {
            if (v.getCjVariantId() == null || v.getCjVariantId().isBlank()) continue;
            checked++;
            try {
                List<CjDropshippingClient.CjVariantStock> stocks = cj.getVariantStock(v.getCjVariantId());
                if (stocks != null && !stocks.isEmpty()) {
                    int total = 0; boolean any = false;
                    List<String> areas = new ArrayList<>();
                    for (var s : stocks) { if (s.totalInventory() != null) { total += Math.max(0, s.totalInventory()); any = true; } if (s.areaEn() != null && areas.size() < 3) areas.add(s.areaEn() + (s.totalInventory()!=null?":"+s.totalInventory():"")); }
                    if (any) { v.setStockAvailability(total + " available" + (areas.isEmpty() ? "" : " (" + String.join(", ", areas) + ")")); variantRepository.save(v); updated++; }
                }
                Thread.sleep(250);
            } catch (Exception e) {
                if (notes.size() < 5) notes.add("Variant " + v.getSku() + ": " + safe(e.getMessage()));
            }
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("productId", productId); res.put("variantsChecked", checked); res.put("variantsUpdated", updated);
        res.put("message", checked == 0 ? "No CJ variant IDs found on this product." : "CJ stock sync completed (admin-only/internal data).");
        if (!notes.isEmpty()) res.put("notes", notes);
        return res;
    }

    private static String safe(String s) { if (s == null) return "unknown"; return s.length() > 120 ? s.substring(0, 120) : s; }
}
