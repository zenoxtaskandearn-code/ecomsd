package com.fetch.backend.catalog.admin;

import com.fetch.backend.catalog.cj.CjAutoImportService;
import com.fetch.backend.catalog.cj.CjDropshippingClient;
import com.fetch.backend.catalog.cj.CjProductRefreshTx;
import com.fetch.backend.catalog.dto.CatalogDtos;
import com.fetch.backend.catalog.entity.Category;
import com.fetch.backend.catalog.entity.Product;
import com.fetch.backend.catalog.entity.ProductImage;
import com.fetch.backend.catalog.entity.ProductVariant;
import com.fetch.backend.catalog.repo.CategoryRepository;
import com.fetch.backend.catalog.repo.ProductImageRepository;
import com.fetch.backend.catalog.repo.ProductRepository;
import com.fetch.backend.catalog.repo.ProductVariantRepository;
import com.fetch.backend.common.error.BadRequestException;
import com.fetch.backend.common.error.NotFoundException;
import com.fetch.backend.supplier.repo.SupplierRepository;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/v1/admin/import/cj")
public class AdminCjImportController {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final SupplierRepository supplierRepository;
    private final CjDropshippingClient cjClient;
    private final CjAutoImportService autoImportService;
    private final CjProductRefreshTx refreshTx;
    private final com.fetch.backend.catalog.cj.CjStockSyncService stockSyncService;

    public AdminCjImportController(CategoryRepository categoryRepository, ProductRepository productRepository,
                                    ProductVariantRepository variantRepository, ProductImageRepository imageRepository,
                                    SupplierRepository supplierRepository,
                                    CjDropshippingClient cjClient, CjAutoImportService autoImportService,
                                    CjProductRefreshTx refreshTx,
                                    com.fetch.backend.catalog.cj.CjStockSyncService stockSyncService) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.imageRepository = imageRepository;
        this.supplierRepository = supplierRepository;
        this.cjClient = cjClient;
        this.autoImportService = autoImportService;
        this.refreshTx = refreshTx;
        this.stockSyncService = stockSyncService;
    }

    @GetMapping("/status")
    public CatalogDtos.CjImportStatusResponse status() {
        boolean openIdAvailable = false;
        try { String oid = cjClient.getOpenId(); openIdAvailable = oid != null && !oid.isBlank(); } catch (Exception ignored) {}
        if (cjClient.isConfigured()) {
            return new CatalogDtos.CjImportStatusResponse(true, "API", "Official CJdropshipping API credentials are configured. You can auto-import your CJ My Products or use manual import.", openIdAvailable);
        }
        return new CatalogDtos.CjImportStatusResponse(false, "MANUAL", "CJdropshipping API access is not configured. Use manual product import (no scraping/fake endpoints). Configure CJ_API_ENABLED=true and CJ_API_KEY for official API auto-import.", openIdAvailable);
    }

    @PostMapping("/my-products")
    public CatalogDtos.CjAutoImportSummary importMyProducts(@Valid @RequestBody CatalogDtos.CjAutoImportRequest req) {
        return autoImportService.importMyProducts(req);
    }

    @PostMapping("/my-products/preview")
    public CatalogDtos.CjMyProductPreviewResponse previewMyProducts(@RequestBody(required = false) CatalogDtos.CjPreviewRequest req) {
        return autoImportService.previewMyProducts(req);
    }

    @PostMapping("/my-products/import-selected")
    public CatalogDtos.CjAutoImportSummary importSelected(@Valid @RequestBody CatalogDtos.CjImportSelectedRequest req) {
        return autoImportService.importSelected(req);
    }

    @PostMapping("/products/{productId}/refresh")
    public Map<String, Object> refreshFromCj(@PathVariable Long productId) {
        if (!cjClient.isConfigured()) throw new BadRequestException("CJdropshipping API is not configured. Set CJ_API_ENABLED=true and CJ_API_KEY, then restart the backend.");
        Product product = productRepository.findById(productId).orElseThrow(() -> new NotFoundException("Product not found"));
        if (product.getCjProductId() == null || product.getCjProductId().isBlank()) throw new BadRequestException("This product has no CJ product ID to refresh from.");
        CjDropshippingClient.CjProductDetail detail = cjClient.getProductDetail(product.getCjProductId());
        return refreshTx.refresh(productId, detail);
    }

    @PostMapping("/products/{productId}/sync-stock")
    public Map<String, Object> syncStockFromCj(@PathVariable Long productId) {
        return stockSyncService.syncProductStock(productId);
    }

    @PostMapping("/manual")
    @Transactional
    public Map<String, Object> manualImport(@Valid @RequestBody CatalogDtos.CjProductImportRequest req) {
        Category category = categoryRepository.findById(req.categoryId()).orElseThrow(() -> new NotFoundException("Category not found"));
        if (req.supplierId() != null) {
            supplierRepository.findById(req.supplierId()).orElseThrow(() -> new NotFoundException("Supplier not found"));
        }
        if ((req.title() == null || req.title().isBlank()) && (req.cjProductId() == null || req.cjProductId().isBlank())) {
            throw new BadRequestException("Product title or CJ product ID is required");
        }

        Product p = new Product();
        p.setCategory(category);
        p.setName(req.title().trim());
        p.setSlug(uniqueProductSlug(blankToNull(req.slug()) != null ? req.slug() : slugify(req.title(), req.cjProductId())));
        p.setDescription(blankToNull(req.description()));
        BigDecimal base = req.sellingPriceAmount() != null ? req.sellingPriceAmount() : deriveBasePrice(req.variants());
        if (base == null) base = BigDecimal.ZERO;
        p.setBasePriceAmount(base);
        p.setCurrency(blankToDefault(req.currency(), "USD"));
        p.setStatus("ACTIVE"); // publish imported CJ products so they are available immediately
        p.setCjProductId(blankToNull(req.cjProductId()));
        p.setSupplierSource("CJ_MANUAL");
        p.setSupplierId(req.supplierId());
        p.setEstimatedDelivery(blankToNull(req.estimatedDelivery()));
        p.setShippingOptionsJson(blankToNull(req.shippingOptionsJson()));
        p.setSupplierShippingCostAmount(req.supplierShippingCostAmount());
        p.setInternalNotes(blankToNull(req.internalNotes()));
        p = productRepository.save(p);

        List<Map<String, Object>> variantMaps = new ArrayList<>();
        List<CatalogDtos.CjVariantImport> variants = req.variants() == null || req.variants().isEmpty()
                ? List.of(defaultVariant(req))
                : req.variants();
        int idx = 1;
        for (CatalogDtos.CjVariantImport vi : variants) {
            ProductVariant v = new ProductVariant();
            v.setProduct(p);
            String sku = blankToNull(vi.sku());
            if (sku == null) sku = generateSku(p.getId(), req.cjProductId(), vi.cjVariantId(), idx);
            v.setSku(uniqueSku(sku));
            v.setTitle(blankToNull(vi.title()));
            v.setOptionValuesJson(blankToNull(vi.optionValuesJson()));
            BigDecimal price = vi.sellingPriceAmount() != null ? vi.sellingPriceAmount() : base;
            v.setPriceAmount(price != null ? price : BigDecimal.ZERO);
            v.setCurrency(blankToDefault(vi.currency() != null ? vi.currency() : req.currency(), "USD"));
            v.setCjVariantId(blankToNull(vi.cjVariantId()));
            v.setSupplierSku(blankToNull(vi.supplierSku()));
            v.setSupplierCostAmount(vi.supplierCostAmount());
            v.setShippingCostAmount(vi.shippingCostAmount());
            v.setWeightGrams(vi.weightGrams());
            v.setStockAvailability(blankToNull(vi.stockAvailability()));
            v.setWarehouseInfo(blankToNull(vi.warehouseInfo()));
            v.setDimensionsJson(blankToNull(vi.dimensionsJson()));
            v.setActive(vi.active() == null || vi.active());
            v = variantRepository.save(v);
            variantMaps.add(varMap(v, req.supplierShippingCostAmount()));
            idx++;
        }

        List<Map<String, Object>> imageMaps = new ArrayList<>();
        if (req.imageUrls() != null) {
            AtomicInteger sort = new AtomicInteger(0);
            for (String url : req.imageUrls()) {
                if (url == null || url.isBlank()) continue;
                ProductImage img = new ProductImage();
                img.setProductId(p.getId());
                img.setUrl(url.trim());
                img.setAltText(p.getName());
                img.setSortOrder(sort.getAndIncrement());
                imageMaps.add(imgMap(imageRepository.save(img)));
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", p.getId()); result.put("name", p.getName()); result.put("slug", p.getSlug());
        result.put("status", p.getStatus()); result.put("cjProductId", p.getCjProductId());
        result.put("supplierSource", p.getSupplierSource()); result.put("supplierId", p.getSupplierId());
        result.put("basePriceAmount", p.getBasePriceAmount()); result.put("currency", p.getCurrency());
        result.put("supplierShippingCostAmount", p.getSupplierShippingCostAmount());
        result.put("estimatedDelivery", p.getEstimatedDelivery());
        result.put("variants", variantMaps); result.put("images", imageMaps);
        result.put("message", "CJ product imported and published (ACTIVE). Review details, variants, images, and selling prices anytime.");
        return result;
    }

    private CatalogDtos.CjVariantImport defaultVariant(CatalogDtos.CjProductImportRequest req) {
        return new CatalogDtos.CjVariantImport(null, null, null, "Default", null, req.sellingPriceAmount(), null, null,
                req.currency(), null, null, null, null, true);
    }

    private BigDecimal deriveBasePrice(List<CatalogDtos.CjVariantImport> variants) {
        if (variants == null) return null;
        return variants.stream().map(CatalogDtos.CjVariantImport::sellingPriceAmount).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private String uniqueProductSlug(String base) {
        String slug = base == null || base.isBlank() ? "imported-product" : base;
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
        return base.length() > 180 ? base.substring(0, 180) : base;
    }

    private Map<String, Object> varMap(ProductVariant v, BigDecimal productShippingCost) {
        Map<String,Object> m=new LinkedHashMap<>(); m.put("id",v.getId()); m.put("sku",v.getSku()); m.put("title",v.getTitle());
        m.put("priceAmount",v.getPriceAmount()); m.put("currency",v.getCurrency()); m.put("active",v.isActive());
        m.put("cjVariantId",v.getCjVariantId()); m.put("supplierSku",v.getSupplierSku()); m.put("supplierCostAmount",v.getSupplierCostAmount());
        m.put("shippingCostAmount",v.getShippingCostAmount()); m.put("stockAvailability",v.getStockAvailability());
        m.put("warehouseInfo",v.getWarehouseInfo()); m.put("dimensionsJson",v.getDimensionsJson()); m.put("weightGrams",v.getWeightGrams());
        BigDecimal ship = v.getShippingCostAmount() != null ? v.getShippingCostAmount() : (productShippingCost != null ? productShippingCost : BigDecimal.ZERO);
        BigDecimal cost = v.getSupplierCostAmount() != null ? v.getSupplierCostAmount() : BigDecimal.ZERO;
        if (v.getPriceAmount() != null && (v.getSupplierCostAmount() != null || v.getShippingCostAmount() != null || productShippingCost != null))
            m.put("estimatedProfitAmount", v.getPriceAmount().subtract(cost).subtract(ship));
        else m.put("estimatedProfitAmount", null);
        return m;
    }
    private Map<String,Object> imgMap(ProductImage i){ Map<String,Object> m=new LinkedHashMap<>(); m.put("id",i.getId()); m.put("url",i.getUrl()); m.put("altText",i.getAltText()); m.put("sortOrder",i.getSortOrder()); return m; }
    private String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
    private String blankToDefault(String s, String d) { return (s == null || s.isBlank()) ? d : s.trim(); }
}
