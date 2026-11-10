package com.fetch.backend.supplier.repo;

import com.fetch.backend.supplier.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {}
