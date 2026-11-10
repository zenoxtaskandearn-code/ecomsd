package com.fetch.backend.catalog.repo;

import com.fetch.backend.catalog.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductIdAndActiveTrue(Long productId);
    List<ProductVariant> findByProductId(Long productId);
    Optional<ProductVariant> findBySkuIgnoreCase(String sku);
}
