package com.fetch.backend.catalog.repo;

import com.fetch.backend.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByActiveTrueOrderBySortOrderAscNameAsc();
    Optional<Category> findBySlugAndActiveTrue(String slug);
    Optional<Category> findBySlug(String slug);
}
