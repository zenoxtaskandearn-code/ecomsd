package com.fetch.backend.catalog.repo;

import com.fetch.backend.catalog.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySlug(String slug);
    Optional<Product> findBySlugAndStatus(String slug, String status);
    Optional<Product> findFirstByCjProductIdIgnoreCase(String cjProductId);

    @Query("""
            select p from Product p
            where p.status = 'ACTIVE'
              and (:categorySlug is null or p.category.slug = :categorySlug)
              and (:q is null or lower(cast(p.name as string)) like lower(concat('%', cast(:q as string), '%')))
            """)
    Page<Product> searchActive(@Param("categorySlug") String categorySlug, @Param("q") String q, Pageable pageable);
}
