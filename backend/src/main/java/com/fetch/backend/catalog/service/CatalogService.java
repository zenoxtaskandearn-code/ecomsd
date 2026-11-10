package com.fetch.backend.catalog.service;

import com.fetch.backend.catalog.dto.CatalogDtos;
import com.fetch.backend.catalog.entity.*;
import com.fetch.backend.catalog.repo.*;
import com.fetch.backend.common.error.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class CatalogService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;

    public CatalogService(CategoryRepository categoryRepository, ProductRepository productRepository,
                          ProductVariantRepository variantRepository, ProductImageRepository imageRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.imageRepository = imageRepository;
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.CategoryResponse> listCategories() {
        return categoryRepository.findByActiveTrueOrderBySortOrderAscNameAsc().stream().map(this::toCategory).toList();
    }

    @Transactional(readOnly = true)
    public Page<CatalogDtos.ProductSummaryResponse> searchProducts(String categorySlug, String q, Pageable pageable) {
        String cat = blankToNull(categorySlug);
        String query = blankToNull(q);
        return productRepository.searchActive(cat, query, pageable).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public CatalogDtos.ProductDetailResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlugAndStatus(slug, "ACTIVE")
                .orElseThrow(() -> new NotFoundException("Product not found"));
        List<ProductImage> imageEntities = imageRepository.findByProductIdOrderBySortOrderAscIdAsc(product.getId());
        List<CatalogDtos.VariantResponse> variants = variantRepository.findByProductIdAndActiveTrue(product.getId())
                .stream().map(v -> toVariant(v, imageEntities)).toList();
        List<CatalogDtos.ImageResponse> images = imageEntities
                .stream().map(i -> new CatalogDtos.ImageResponse(i.getId(), i.getUrl(), i.getAltText(), i.getSortOrder(), i.getVariantId())).toList();
        return new CatalogDtos.ProductDetailResponse(product.getId(), product.getName(), product.getSlug(), product.getDescription(),
                product.getBasePriceAmount(), product.getCurrency(), product.getStatus(), product.getEstimatedDelivery(), toCategory(product.getCategory()), variants, images,
                product.getShippingOptionsJson(), product.getSeoTitle(), product.getSeoDescription());
    }

    private CatalogDtos.CategoryResponse toCategory(Category c) {
        return new CatalogDtos.CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getDescription(), c.isActive(), c.getSortOrder());
    }

    private CatalogDtos.VariantResponse toVariant(ProductVariant v, List<ProductImage> images) {
        String imageUrl = images == null ? null : images.stream()
                .filter(i -> i.getVariantId() != null && i.getVariantId().equals(v.getId()))
                .map(ProductImage::getUrl).filter(u -> u != null && !u.isBlank()).findFirst().orElse(null);
        return new CatalogDtos.VariantResponse(v.getId(), v.getSku(), v.getTitle(), v.getPriceAmount(), v.getCurrency(), v.isActive(), v.getOptionValuesJson(), imageUrl,
                v.getStockAvailability(), v.getWeightGrams(), v.getWarehouseInfo());
    }

    private CatalogDtos.ProductSummaryResponse toSummary(Product p) {
        List<ProductVariant> variants = variantRepository.findByProductIdAndActiveTrue(p.getId());
        ProductVariant first = variants.stream().min(Comparator.comparing(ProductVariant::getId)).orElse(null);
        String image = imageRepository.findByProductIdOrderBySortOrderAscIdAsc(p.getId()).stream().findFirst().map(ProductImage::getUrl).orElse(null);
        return new CatalogDtos.ProductSummaryResponse(p.getId(), p.getName(), p.getSlug(), p.getDescription(),
                first != null ? first.getPriceAmount() : p.getBasePriceAmount(), p.getCurrency(), p.getStatus(),
                p.getCategory().getSlug(), p.getCategory().getName(), image, first != null ? first.getTitle() : null);
    }

    private String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
}
