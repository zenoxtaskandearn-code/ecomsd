package com.fetch.backend.auth.repo;

import com.fetch.backend.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    long deleteByUserIdAndUsedAtIsNull(Long userId);
    long deleteByExpiresAtBefore(Instant cutoff);
}
