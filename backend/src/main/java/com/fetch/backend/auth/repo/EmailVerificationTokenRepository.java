package com.fetch.backend.auth.repo;

import com.fetch.backend.auth.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
    long deleteByUserIdAndUsedAtIsNull(Long userId);
    long deleteByExpiresAtBefore(Instant cutoff);
}
