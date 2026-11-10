package com.fetch.backend.catalog.repo;

import com.fetch.backend.catalog.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProductIdOrderBySortOrderAscIdAsc(Long productId);
}
