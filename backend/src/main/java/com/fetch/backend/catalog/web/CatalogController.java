package com.fetch.backend.catalog.web;

import com.fetch.backend.catalog.dto.CatalogDtos;
import com.fetch.backend.catalog.service.CatalogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {
    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/categories")
    public List<CatalogDtos.CategoryResponse> categories() {
        return catalogService.listCategories();
    }

    @GetMapping("/products")
    public Page<CatalogDtos.ProductSummaryResponse> products(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String sort
    ) {
        Sort s = "priceAsc".equals(sort) ? Sort.by("basePriceAmount").ascending()
                : "priceDesc".equals(sort) ? Sort.by("basePriceAmount").descending()
                : Sort.by("updatedAt").descending();
        int safeSize = Math.min(Math.max(size, 1), 48);
        return catalogService.searchProducts(category, q, PageRequest.of(Math.max(page, 0), safeSize, s));
    }

    @GetMapping("/products/{slug}")
    public CatalogDtos.ProductDetailResponse product(@PathVariable String slug) {
        return catalogService.getProductBySlug(slug);
    }
}
