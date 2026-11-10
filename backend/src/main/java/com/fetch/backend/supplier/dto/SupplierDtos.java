package com.fetch.backend.supplier.dto;

import jakarta.validation.constraints.NotBlank;

public class SupplierDtos {
    public record SupplierRequest(@NotBlank String name, @NotBlank String code, String contactEmail, String status, String apiMode, String notes) {}
}
