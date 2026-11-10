package com.fetch.backend.contact.web;

import com.fetch.backend.contact.dto.ContactDtos;
import com.fetch.backend.contact.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/contact")
public class ContactController {
    private final ContactService contactService;
    public ContactController(ContactService contactService) { this.contactService = contactService; }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> submit(@Valid @RequestBody ContactDtos.ContactRequest request) {
        contactService.submit(request);
        return Map.of("status", "received");
    }
}
