package com.fetch.backend.supplier.admin;

import com.fetch.backend.common.error.NotFoundException;
import com.fetch.backend.supplier.dto.SupplierDtos;
import com.fetch.backend.supplier.entity.Supplier;
import com.fetch.backend.supplier.repo.SupplierRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/suppliers")
public class AdminSupplierController {
    private final SupplierRepository repository;
    public AdminSupplierController(SupplierRepository repository) { this.repository = repository; }

    @GetMapping public List<Supplier> list() { return repository.findAll(); }
    @PostMapping public Supplier create(@Valid @RequestBody SupplierDtos.SupplierRequest req) { Supplier s = new Supplier(); apply(s, req); return repository.save(s); }
    @PutMapping("/{id}") public Supplier update(@PathVariable Long id, @Valid @RequestBody SupplierDtos.SupplierRequest req) {
        Supplier s = repository.findById(id).orElseThrow(() -> new NotFoundException("Supplier not found")); apply(s, req); return repository.save(s);
    }
    private void apply(Supplier s, SupplierDtos.SupplierRequest req) {
        s.setName(req.name()); s.setCode(req.code()); s.setContactEmail(req.contactEmail());
        s.setStatus(req.status() == null || req.status().isBlank() ? "ACTIVE" : req.status());
        s.setApiMode(req.apiMode() == null || req.apiMode().isBlank() ? "MANUAL" : req.apiMode());
        s.setNotes(req.notes());
    }
}
