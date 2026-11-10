package com.fetch.backend.catalog.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public class CatalogDtos {
    public record CategoryResponse(Long id, String name, String slug, String description, boolean active, int sortOrder) {}
    public record CategoryRequest(@NotBlank String name, @NotBlank String slug, String description, Boolean active, Integer sortOrder, Long parentId) {}

    public record ImageResponse(Long id, String url, String altText, int sortOrder, Long variantId) {}
    public record VariantResponse(Long id, String sku, String title, BigDecimal priceAmount, String currency, boolean active,
                                   String optionValuesJson, String imageUrl, String stockAvailability, Integer weightGrams, String warehouseInfo) {}

    public record ProductSummaryResponse(Long id, String name, String slug, String description, BigDecimal priceAmount, String currency,
                                         String status, String categorySlug, String categoryName, String primaryImageUrl, String variantTitle) {}

    public record ProductDetailResponse(Long id, String name, String slug, String description, BigDecimal basePriceAmount, String currency,
                                        String status, String estimatedDelivery, CategoryResponse category, List<VariantResponse> variants, List<ImageResponse> images,
                                        String shippingOptionsJson, String seoTitle, String seoDescription) {}

    public record ProductRequest(
            @NotNull Long categoryId,
            @NotBlank String name,
            @NotBlank String slug,
            String description,
            @NotNull @DecimalMin("0.00") BigDecimal basePriceAmount,
            String currency,
            @NotBlank String status,
            String seoTitle,
            String seoDescription,
            String cjProductId,
            String supplierSource,
            Long supplierId,
            String estimatedDelivery,
            String shippingOptionsJson,
            BigDecimal supplierShippingCostAmount,
            String internalNotes
    ) {}

    public record VariantRequest(
            @NotBlank String sku,
            String title,
            String optionValuesJson,
            @NotNull @DecimalMin("0.00") BigDecimal priceAmount,
            String currency,
            Boolean active,
            String cjVariantId,
            String supplierSku,
            BigDecimal supplierCostAmount,
            BigDecimal shippingCostAmount,
            Integer weightGrams,
            String stockAvailability,
            String warehouseInfo,
            String dimensionsJson
    ) {}

    public record ImageRequest(@NotBlank String url, String altText, Integer sortOrder, Long variantId) {}

    public record CjVariantImport(
            String cjVariantId,
            String sku,
            String supplierSku,
            String title,
            String optionValuesJson,
            BigDecimal sellingPriceAmount,
            BigDecimal supplierCostAmount,
            BigDecimal shippingCostAmount,
            String currency,
            Integer weightGrams,
            String dimensionsJson,
            String stockAvailability,
            String warehouseInfo,
            Boolean active
    ) {}

    public record CjProductImportRequest(
            @NotNull Long categoryId,
            @NotBlank String title,
            String slug,
            String description,
            String cjProductId,
            Long supplierId,
            BigDecimal sellingPriceAmount,
            String currency,
            String estimatedDelivery,
            String shippingOptionsJson,
            BigDecimal supplierShippingCostAmount,
            String status,
            List<String> imageUrls,
            List<CjVariantImport> variants,
            String internalNotes
    ) {}

    public record CjImportStatusResponse(boolean apiConfigured, String mode, String message, boolean openIdAvailable) {}

    public record CjAutoImportRequest(
            @NotNull Long categoryId,
            Integer maxProducts,
            String keyword
    ) {}

    public record CjAutoImportSummary(
            int requestedMax,
            int scanned,
            int imported,
            int skippedExisting,
            int failed,
            List<String> messages,
            List<Long> importedProductIds
    ) {}

    public record CjPreviewRequest(Integer maxProducts, String keyword) {}

    public record CjMyProductPreview(
            String cjProductId,
            String name,
            String sku,
            String imageUrl,
            BigDecimal sellPrice,
            boolean alreadyImported
    ) {}

    public record CjMyProductPreviewResponse(int totalFound, List<CjMyProductPreview> products, String message) {}

    public record CjImportSelectedRequest(
            @NotNull Long categoryId,
            @NotNull List<String> cjProductIds
    ) {}
}
