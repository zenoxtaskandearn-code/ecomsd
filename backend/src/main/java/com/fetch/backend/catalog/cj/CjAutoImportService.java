package com.fetch.backend.catalog.cj;

import com.fetch.backend.catalog.dto.CatalogDtos;
import com.fetch.backend.catalog.entity.Category;
import com.fetch.backend.catalog.repo.CategoryRepository;
import com.fetch.backend.catalog.repo.ProductRepository;
import com.fetch.backend.common.error.BadRequestException;
import com.fetch.backend.common.error.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CjAutoImportService {
    private final CjDropshippingClient cj;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CjProductImportTx tx;

    public CjAutoImportService(CjDropshippingClient cj, CategoryRepository categoryRepository,
                               ProductRepository productRepository, CjProductImportTx tx) {
        this.cj = cj; this.categoryRepository = categoryRepository; this.productRepository = productRepository; this.tx = tx;
    }

    public CatalogDtos.CjAutoImportSummary importMyProducts(CatalogDtos.CjAutoImportRequest req) {
        if (!cj.isConfigured()) throw new BadRequestException("CJdropshipping API is not configured. Set CJ_API_ENABLED=true and CJ_API_KEY in the backend environment, then restart the backend.");
        Category category = categoryRepository.findById(req.categoryId()).orElseThrow(() -> new NotFoundException("Category not found"));
        int max = req.maxProducts() == null ? 50 : Math.min(200, Math.max(1, req.maxProducts()));
        List<String> messages = new ArrayList<>();
        List<Long> importedIds = new ArrayList<>();
        int scanned = 0, imported = 0, skipped = 0, failed = 0;
        int pageNum = 1, pageSize = 50, totalPages = 1;
        String keyword = req.keyword() == null || req.keyword().isBlank() ? null : req.keyword().trim();
        try {
            while (pageNum <= totalPages && scanned < max) {
                CjDropshippingClient.CjMyProductPage page = cj.listMyProducts(pageNum, pageSize, keyword);
                totalPages = Math.max(1, page.totalPages());
                if (page.items() == null || page.items().isEmpty()) break;
                for (CjDropshippingClient.CjMyProduct item : page.items()) {
                    if (scanned >= max) break;
                    scanned++;
                    String cjPid = item.productId();
                    if (cjPid != null && productRepository.findFirstByCjProductIdIgnoreCase(cjPid).isPresent()) {
                        skipped++; continue;
                    }
                    try {
                        CjDropshippingClient.CjProductDetail detail = null;
                        try { detail = cj.getProductDetail(item.productId()); } catch (Exception ignored) { /* fallback to list data */ }
                        Long id = tx.importOne(category.getId(), item, detail);
                        imported++; importedIds.add(id);
                        pause(350);
                    } catch (Exception e) {
                        failed++;
                        String name = item.nameEn() != null ? item.nameEn() : cjPid;
                        if (messages.size() < 15) messages.add("Failed: " + name + " — " + safeMsg(e.getMessage()));
                    }
                }
                pageNum++;
                pause(350);
            }
        } catch (Exception e) {
            messages.add("CJ API import stopped: " + safeMsg(e.getMessage()));
        }
        if (messages.isEmpty() && imported == 0 && skipped == 0 && failed == 0) messages.add("No CJ My Products found to import.");
        if (imported > 0) messages.add("Imported products are saved as DRAFTS. Review details, variants, images, and selling prices before publishing.");
        return new CatalogDtos.CjAutoImportSummary(max, scanned, imported, skipped, failed, messages, importedIds);
    }

    public CatalogDtos.CjMyProductPreviewResponse previewMyProducts(CatalogDtos.CjPreviewRequest req) {
        if (!cj.isConfigured()) throw new BadRequestException("CJdropshipping API is not configured. Set CJ_API_ENABLED=true and CJ_API_KEY in the backend environment, then restart the backend.");
        int max = req != null && req.maxProducts() != null ? Math.min(200, Math.max(1, req.maxProducts())) : 100;
        String keyword = req != null && req.keyword() != null && !req.keyword().isBlank() ? req.keyword().trim() : null;
        List<CatalogDtos.CjMyProductPreview> out = new ArrayList<>();
        int pageNum = 1, pageSize = 50, totalPages = 1;
        long totalFound = 0;
        try {
            while (pageNum <= totalPages && out.size() < max) {
                CjDropshippingClient.CjMyProductPage page = cj.listMyProducts(pageNum, pageSize, keyword);
                totalPages = Math.max(1, page.totalPages());
                totalFound = page.totalRecords();
                if (page.items() == null || page.items().isEmpty()) break;
                for (CjDropshippingClient.CjMyProduct item : page.items()) {
                    if (out.size() >= max) break;
                    boolean exists = item.productId() != null && productRepository.findFirstByCjProductIdIgnoreCase(item.productId()).isPresent();
                    out.add(new CatalogDtos.CjMyProductPreview(
                            item.productId(),
                            item.nameEn() != null ? item.nameEn() : item.sku(),
                            item.sku(),
                            item.bigImage(),
                            item.sellPrice(),
                            exists
                    ));
                }
                pageNum++;
                pause(250);
            }
        } catch (Exception e) {
            throw new BadRequestException("CJ API error while loading My Products: " + safeMsg(e.getMessage()));
        }
        String msg = out.isEmpty() ? "No CJ My Products found." : ("Loaded " + out.size() + " CJ My Product(s)." + (totalFound > out.size() ? " Showing up to " + max + " (total " + totalFound + ")." : ""));
        return new CatalogDtos.CjMyProductPreviewResponse(out.size(), out, msg);
    }

    public CatalogDtos.CjAutoImportSummary importSelected(CatalogDtos.CjImportSelectedRequest req) {
        if (!cj.isConfigured()) throw new BadRequestException("CJdropshipping API is not configured. Set CJ_API_ENABLED=true and CJ_API_KEY in the backend environment, then restart the backend.");
        Category category = categoryRepository.findById(req.categoryId()).orElseThrow(() -> new NotFoundException("Category not found"));
        List<String> ids = req.cjProductIds() == null ? List.of() : req.cjProductIds().stream().filter(s -> s != null && !s.isBlank()).distinct().toList();
        if (ids.isEmpty()) throw new BadRequestException("Select at least one CJ product to import.");
        if (ids.size() > 200) throw new BadRequestException("You can import up to 200 selected products at a time.");
        List<String> messages = new ArrayList<>();
        List<Long> importedIds = new ArrayList<>();
        int scanned = 0, imported = 0, skipped = 0, failed = 0;
        for (String cjPid : ids) {
            scanned++;
            if (productRepository.findFirstByCjProductIdIgnoreCase(cjPid).isPresent()) { skipped++; continue; }
            try {
                CjDropshippingClient.CjProductDetail detail = null;
                try { detail = cj.getProductDetail(cjPid); } catch (Exception ignored) {}
                CjDropshippingClient.CjMyProduct item = new CjDropshippingClient.CjMyProduct(
                        cjPid,
                        detail != null ? detail.nameEn() : null,
                        detail != null ? detail.sku() : null,
                        (detail != null && detail.images() != null && !detail.images().isEmpty()) ? detail.images().get(0) : null,
                        detail != null ? detail.sellPrice() : null,
                        (detail != null && detail.variants() != null && !detail.variants().isEmpty()) ? detail.variants().get(0).vid() : null
                );
                Long id = tx.importOne(category.getId(), item, detail);
                imported++; importedIds.add(id);
                pause(350);
            } catch (Exception e) {
                failed++;
                if (messages.size() < 15) messages.add("Failed: " + cjPid + " — " + safeMsg(e.getMessage()));
            }
        }
        if (imported > 0) messages.add("Imported products are saved as DRAFTS. Review details, variants, images, and selling prices before publishing.");
        return new CatalogDtos.CjAutoImportSummary(ids.size(), scanned, imported, skipped, failed, messages, importedIds);
    }

    private static void pause(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
    private static String safeMsg(String m) { if (m == null) return "unknown error"; return m.length() > 140 ? m.substring(0, 140) : m; }
}
