package com.fetch.backend.contact.repo;

import com.fetch.backend.contact.entity.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {}
