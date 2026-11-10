package com.fetch.backend.contact.service;

import com.fetch.backend.contact.dto.ContactDtos;
import com.fetch.backend.contact.entity.ContactMessage;
import com.fetch.backend.contact.repo.ContactMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactService {
    private final ContactMessageRepository repository;
    public ContactService(ContactMessageRepository repository) { this.repository = repository; }

    @Transactional
    public void submit(ContactDtos.ContactRequest req) {
        ContactMessage msg = new ContactMessage();
        msg.setName(req.name().trim());
        msg.setEmail(req.email().trim().toLowerCase());
        msg.setSubject(req.subject() == null ? null : req.subject().trim());
        msg.setMessage(req.message().trim());
        repository.save(msg);
    }
}
