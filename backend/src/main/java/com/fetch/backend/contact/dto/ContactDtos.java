package com.fetch.backend.contact.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ContactDtos {
    public record ContactRequest(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Email @Size(max = 255) String email,
            @Size(max = 200) String subject,
            @NotBlank @Size(max = 5000) String message
    ) {}
}
