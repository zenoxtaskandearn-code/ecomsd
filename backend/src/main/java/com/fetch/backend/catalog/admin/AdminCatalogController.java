package com.fetch.backend.catalog.admin;

import com.fetch.backend.catalog.dto.CatalogDtos;
import com.fetch.backend.catalog.entity.Category;
import com.fetch.backend.catalog.entity.Product;
import com.fetch.backend.catalog.entity.ProductImage;
import com.fetch.backend.catalog.entity.ProductVariant;
import com.fetch.backend.catalog.repo.CategoryRepository;
import com.fetch.backend.catalog.repo.ProductImageRepository;
import com.fetch.backend.catalog.repo.ProductRepository;
import com.fetch.backend.catalog.repo.ProductVariantRepository;
import com.fetch.backend.common.error.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminCatalogController {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;

    public AdminCatalogController(CategoryRepository categoryRepository, ProductRepository productRepository,
                                  ProductVariantRepository variantRepository, ProductImageRepository imageRepository) {
        this.categoryRepository = categoryRepository; this.productRepository = productRepository;
        this.variantRepository = variantRepository; this.imageRepository = imageRepository;
    }

    @GetMapping("/categories")
    public List<Map<String, Object>> categories() { return categoryRepository.findAll().stream().map(this::cat).toList(); }

    @PostMapping("/categories")
    public Map<String, Object> createCategory(@Valid @RequestBody CatalogDtos.CategoryRequest req) { Category c = new Category(); apply(c, req); return cat(categoryRepository.save(c)); }

    @PutMapping("/categories/{id}")
    public Map<String, Object> updateCategory(@PathVariable Long id, @Valid @RequestBody CatalogDtos.CategoryRequest req) {
        Category c = categoryRepository.findById(id).orElseThrow(() -> new NotFoundException("Category not found")); apply(c, req); return cat(categoryRepository.save(c));
    }

    @GetMapping("/products")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> products() {
        return productRepository.findAll().stream().map(p -> {
            Map<String, Object> m = prod(p);
            m.put("variants", variantRepository.findByProductId(p.getId()).stream().map(v -> var(v, p.getSupplierShippingCostAmount())).toList());
            m.put("images", imageRepository.findByProductIdOrderBySortOrderAscIdAsc(p.getId()).stream().map(this::img).toList());
            return m;
        }).toList();
    }

    @PostMapping("/products")
    @Transactional
    public Map<String, Object> createProduct(@Valid @RequestBody CatalogDtos.ProductRequest req) {
        Category cat = categoryRepository.findById(req.categoryId()).orElseThrow(() -> new NotFoundException("Category not found"));
        Product p = new Product(); p.setCategory(cat); apply(p, req); return prod(productRepository.save(p));
    }

    @PutMapping("/products/{id}")
    @Transactional
    public Map<String, Object> updateProduct(@PathVariable Long id, @Valid @RequestBody CatalogDtos.ProductRequest req) {
        Product p = productRepository.findById(id).orElseThrow(() -> new NotFoundException("Product not found"));
        Category cat = categoryRepository.findById(req.categoryId()).orElseThrow(() -> new NotFoundException("Category not found"));
        p.setCategory(cat); apply(p, req); return prod(productRepository.save(p));
    }

    @GetMapping("/products/{id}/variants")
    public List<Map<String, Object>> variants(@PathVariable Long id) { return variantRepository.findByProductId(id).stream().map(this::var).toList(); }

    @PostMapping("/products/{id}/variants")
    public Map<String, Object> createVariant(@PathVariable Long id, @Valid @RequestBody CatalogDtos.VariantRequest req) {
        Product p = productRepository.findById(id).orElseThrow(() -> new NotFoundException("Product not found"));
        ProductVariant v = new ProductVariant(); v.setProduct(p); apply(v, req); return var(variantRepository.save(v));
    }

    @PutMapping("/variants/{variantId}")
    public Map<String, Object> updateVariant(@PathVariable Long variantId, @Valid @RequestBody CatalogDtos.VariantRequest req) {
        ProductVariant v = variantRepository.findById(variantId).orElseThrow(() -> new NotFoundException("Variant not found")); apply(v, req); return var(variantRepository.save(v));
    }

    @PostMapping("/products/{id}/images")
    public Map<String, Object> createImage(@PathVariable Long id, @Valid @RequestBody CatalogDtos.ImageRequest req) {
        productRepository.findById(id).orElseThrow(() -> new NotFoundException("Product not found"));
        ProductImage i = new ProductImage(); i.setProductId(id); i.setVariantId(req.variantId()); i.setUrl(req.url()); i.setAltText(req.altText()); if (req.sortOrder()!=null) i.setSortOrder(req.sortOrder());
        return img(imageRepository.save(i));
    }

    @DeleteMapping("/images/{imageId}")
    public void deleteImage(@PathVariable Long imageId) { imageRepository.deleteById(imageId); }

    @PostMapping("/products/auto-pricing")
    @Transactional
    public Map<String, Object> autoPricing(@RequestBody Map<String, Object> body) {
        BigDecimal markupPercent = toBig(body.get("markupPercent")); // increase selling price by %
        BigDecimal marginPercent = toBig(body.get("marginPercent")); // set selling price from (cost+shipping)/(1-margin%) when costs known
        BigDecimal fulfillmentShippingCost = toBig(body.get("fulfillmentShippingCost")); // default fulfillment shipping cost (your cost) to set when missing
        BigDecimal supplierCost = toBig(body.get("supplierCost")); // optional default supplier cost to set when missing (use carefully)
        Boolean onlyMissingShippingCost = body.get("onlyMissingShippingCost") == null ? Boolean.TRUE : Boolean.valueOf(String.valueOf(body.get("onlyMissingShippingCost")));
        Boolean onlyMissingSupplierCost = body.get("onlyMissingSupplierCost") == null ? Boolean.TRUE : Boolean.valueOf(String.valueOf(body.get("onlyMissingSupplierCost")));
        Boolean updateProductBasePrice = body.get("updateProductBasePrice") == null ? Boolean.TRUE : Boolean.valueOf(String.valueOf(body.get("updateProductBasePrice")));

        if ((markupPercent == null || markupPercent.compareTo(BigDecimal.ZERO) == 0)
                && (marginPercent == null || marginPercent.compareTo(BigDecimal.ZERO) == 0)
                && fulfillmentShippingCost == null && supplierCost == null) {
            throw new com.fetch.backend.common.error.BadRequestException("Provide at least one pricing action (markupPercent, marginPercent, fulfillmentShippingCost, or supplierCost).");
        }
        if (marginPercent != null && (marginPercent.compareTo(BigDecimal.ZERO) < 0 || marginPercent.compareTo(new BigDecimal("95")) >= 0)) {
            throw new com.fetch.backend.common.error.BadRequestException("marginPercent must be between 0 and < 95.");
        }

        List<Product> products = productRepository.findAll();
        int variantsUpdated = 0;
        int productsUpdated = 0;
        int pricesChanged = 0;
        int costsSet = 0;

        for (Product p : products) {
            boolean productChanged = false;
            List<ProductVariant> vars = variantRepository.findByProductId(p.getId());
            for (ProductVariant v : vars) {
                boolean changed = false;

                // set default fulfillment (your) shipping cost for profit tracking when missing
                if (fulfillmentShippingCost != null && fulfillmentShippingCost.compareTo(BigDecimal.ZERO) >= 0) {
                    if (!Boolean.TRUE.equals(onlyMissingShippingCost) || v.getShippingCostAmount() == null) {
                        if (!eqMoney(v.getShippingCostAmount(), fulfillmentShippingCost)) { v.setShippingCostAmount(fulfillmentShippingCost); changed = true; costsSet++; }
                    }
                }
                // optional default supplier cost when missing (admin-provided assumption)
                if (supplierCost != null && supplierCost.compareTo(BigDecimal.ZERO) >= 0) {
                    if (!Boolean.TRUE.equals(onlyMissingSupplierCost) || v.getSupplierCostAmount() == null) {
                        if (!eqMoney(v.getSupplierCostAmount(), supplierCost)) { v.setSupplierCostAmount(supplierCost); changed = true; costsSet++; }
                    }
                }

                BigDecimal newPrice = v.getPriceAmount();
                // 1) margin-based pricing when costs known: price = (supplier + fulfillmentShipping) / (1 - margin)
                if (marginPercent != null && marginPercent.compareTo(BigDecimal.ZERO) > 0
                        && v.getSupplierCostAmount() != null) {
                    BigDecimal ship = v.getShippingCostAmount() != null ? v.getShippingCostAmount() : BigDecimal.ZERO;
                    BigDecimal totalCost = v.getSupplierCostAmount().add(ship);
                    BigDecimal denom = BigDecimal.ONE.subtract(marginPercent.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
                    if (denom.compareTo(new BigDecimal("0.05")) > 0) {
                        newPrice = totalCost.divide(denom, 2, RoundingMode.HALF_UP);
                    }
                }
                // 2) markup on selling price (can stack as simple increase when requested explicitly)
                if (markupPercent != null && markupPercent.compareTo(BigDecimal.ZERO) != 0) {
                    BigDecimal base = newPrice != null ? newPrice : v.getPriceAmount();
                    if (base != null) {
                        newPrice = base.multiply(BigDecimal.ONE.add(markupPercent.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP))).setScale(2, RoundingMode.HALF_UP);
                    }
                }

                if (newPrice != null && newPrice.compareTo(BigDecimal.ZERO) > 0 && !eqMoney(newPrice, v.getPriceAmount())) {
                    v.setPriceAmount(newPrice);
                    changed = true;
                    pricesChanged++;
                }

                if (changed) { variantRepository.save(v); variantsUpdated++; productChanged = true; }
            }

            if (Boolean.TRUE.equals(updateProductBasePrice)) {
                List<ProductVariant> refreshed = variantRepository.findByProductId(p.getId());
                BigDecimal min = null;
                for (ProductVariant v : refreshed) {
                    if (v.getPriceAmount() == null) continue;
                    if (min == null || v.getPriceAmount().compareTo(min) < 0) min = v.getPriceAmount();
                }
                if (min != null && !eqMoney(min, p.getBasePriceAmount())) {
                    p.setBasePriceAmount(min);
                    productRepository.save(p);
                    productsUpdated++;
                    productChanged = true;
                }
            }
            if (productChanged) { /* counted */ }
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("productsScanned", products.size());
        res.put("productsUpdated", productsUpdated);
        res.put("variantsUpdated", variantsUpdated);
        res.put("pricesChanged", pricesChanged);
        res.put("costFieldsSet", costsSet);
        res.put("message", "Auto-pricing applied. Selling prices updated where rules matched. Supplier/fulfillment costs were set only for fields you provided (and missing-only when selected). For accurate profit, set real CJ supplier and fulfillment shipping costs per variant/route.");
        return res;
    }

    private static BigDecimal toBig(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o).trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) return null;
        try { return new BigDecimal(s); } catch (Exception e) { return null; }
    }
    private static boolean eqMoney(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.setScale(2, RoundingMode.HALF_UP).compareTo(b.setScale(2, RoundingMode.HALF_UP)) == 0;
    }

    private void apply(Category c, CatalogDtos.CategoryRequest req) {
        c.setName(req.name()); c.setSlug(req.slug()); c.setDescription(req.description());
        if (req.active() != null) c.setActive(req.active()); if (req.sortOrder() != null) c.setSortOrder(req.sortOrder()); c.setParentId(req.parentId());
    }
    private void apply(Product p, CatalogDtos.ProductRequest req) {
        p.setName(req.name()); p.setSlug(req.slug()); p.setDescription(req.description());
        p.setBasePriceAmount(req.basePriceAmount()); p.setCurrency(req.currency() == null ? "USD" : req.currency());
        p.setStatus(req.status()); p.setSeoTitle(req.seoTitle()); p.setSeoDescription(req.seoDescription());
        p.setCjProductId(req.cjProductId()); p.setSupplierSource(req.supplierSource()); p.setSupplierId(req.supplierId());
        p.setEstimatedDelivery(req.estimatedDelivery()); p.setShippingOptionsJson(req.shippingOptionsJson());
        p.setSupplierShippingCostAmount(req.supplierShippingCostAmount()); p.setInternalNotes(req.internalNotes());
    }
    private void apply(ProductVariant v, CatalogDtos.VariantRequest req) {
        v.setSku(req.sku()); v.setTitle(req.title()); v.setOptionValuesJson(req.optionValuesJson());
        v.setPriceAmount(req.priceAmount()); v.setCurrency(req.currency() == null ? "USD" : req.currency()); if (req.active()!=null) v.setActive(req.active());
        v.setCjVariantId(req.cjVariantId()); v.setSupplierSku(req.supplierSku());
        v.setSupplierCostAmount(req.supplierCostAmount()); v.setShippingCostAmount(req.shippingCostAmount());
        v.setWeightGrams(req.weightGrams()); v.setStockAvailability(req.stockAvailability());
        v.setWarehouseInfo(req.warehouseInfo()); v.setDimensionsJson(req.dimensionsJson());
    }
    private Map<String,Object> cat(Category c){ Map<String,Object> m=new LinkedHashMap<>(); m.put("id",c.getId()); m.put("name",c.getName()); m.put("slug",c.getSlug()); m.put("description",c.getDescription()); m.put("active",c.isActive()); m.put("sortOrder",c.getSortOrder()); m.put("parentId",c.getParentId()); return m; }
    private Map<String,Object> prod(Product p){ Map<String,Object> m=new LinkedHashMap<>(); m.put("id",p.getId()); m.put("categoryId",p.getCategory()!=null?p.getCategory().getId():null); m.put("categorySlug",p.getCategory()!=null?p.getCategory().getSlug():null); m.put("categoryName",p.getCategory()!=null?p.getCategory().getName():null); m.put("name",p.getName()); m.put("slug",p.getSlug()); m.put("description",p.getDescription()); m.put("basePriceAmount",p.getBasePriceAmount()); m.put("currency",p.getCurrency()); m.put("status",p.getStatus()); m.put("seoTitle",p.getSeoTitle()); m.put("seoDescription",p.getSeoDescription());
        m.put("cjProductId",p.getCjProductId()); m.put("supplierSource",p.getSupplierSource()); m.put("supplierId",p.getSupplierId());
        m.put("estimatedDelivery",p.getEstimatedDelivery()); m.put("shippingOptionsJson",p.getShippingOptionsJson());
        m.put("supplierShippingCostAmount",p.getSupplierShippingCostAmount()); m.put("internalNotes",p.getInternalNotes());
        return m; }
    private Map<String,Object> var(ProductVariant v){ return var(v, null); }
    private Map<String,Object> var(ProductVariant v, BigDecimal productShippingCost){
        Map<String,Object> m=new LinkedHashMap<>(); m.put("id",v.getId()); m.put("productId",v.getProduct()!=null?v.getProduct().getId():null); m.put("sku",v.getSku()); m.put("title",v.getTitle()); m.put("priceAmount",v.getPriceAmount()); m.put("currency",v.getCurrency()); m.put("active",v.isActive()); m.put("optionValuesJson",v.getOptionValuesJson());
        m.put("cjVariantId",v.getCjVariantId()); m.put("supplierSku",v.getSupplierSku()); m.put("supplierCostAmount",v.getSupplierCostAmount());
        m.put("shippingCostAmount",v.getShippingCostAmount()); m.put("weightGrams",v.getWeightGrams()); m.put("stockAvailability",v.getStockAvailability());
        m.put("warehouseInfo",v.getWarehouseInfo()); m.put("dimensionsJson",v.getDimensionsJson());
        BigDecimal ship = v.getShippingCostAmount() != null ? v.getShippingCostAmount() : (productShippingCost != null ? productShippingCost : BigDecimal.ZERO);
        BigDecimal cost = v.getSupplierCostAmount() != null ? v.getSupplierCostAmount() : BigDecimal.ZERO;
        if (v.getPriceAmount() != null && (v.getSupplierCostAmount() != null || v.getShippingCostAmount() != null || productShippingCost != null)) {
            m.put("estimatedProfitAmount", v.getPriceAmount().subtract(cost).subtract(ship));
        } else {
            m.put("estimatedProfitAmount", null);
        }
        return m;
    }
    private Map<String,Object> img(ProductImage i){ Map<String,Object> m=new LinkedHashMap<>(); m.put("id",i.getId()); m.put("productId",i.getProductId()); m.put("variantId",i.getVariantId()); m.put("url",i.getUrl()); m.put("altText",i.getAltText()); m.put("sortOrder",i.getSortOrder()); return m; }
}
