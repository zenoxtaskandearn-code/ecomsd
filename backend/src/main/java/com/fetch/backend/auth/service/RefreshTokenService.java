package com.fetch.backend.auth.service;

import com.fetch.backend.auth.entity.RefreshToken;
import com.fetch.backend.auth.repo.RefreshTokenRepository;
import com.fetch.backend.common.error.UnauthorizedException;
import com.fetch.backend.security.jwt.JwtService;
import com.fetch.backend.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository repository;
    private final JwtService jwtService;

    public RefreshTokenService(RefreshTokenRepository repository, JwtService jwtService) {
        this.repository = repository;
        this.jwtService = jwtService;
    }

    @Transactional
    public String issue(User user) {
        String raw = TokenHash.randomToken();
        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(TokenHash.sha256(raw));
        entity.setExpiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()));
        repository.save(entity);
        return raw;
    }

    @Transactional
    public RotatedRefresh rotate(String rawRefreshToken) {
        String hash = TokenHash.sha256(rawRefreshToken);
        RefreshToken existing = repository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (!existing.isActive()) {
            // Possible reuse of rotated/revoked token - revoke all active tokens for user as a safety measure.
            repository.revokeActiveByUserId(existing.getUser().getId(), Instant.now());
            throw new UnauthorizedException("Refresh token is no longer valid");
        }
        existing.setRevokedAt(Instant.now());
        String nextRaw = issue(existing.getUser());
        RefreshToken next = repository.findByTokenHash(TokenHash.sha256(nextRaw)).orElseThrow();
        existing.setReplacedByTokenId(next.getId());
        repository.save(existing);
        return new RotatedRefresh(existing.getUser(), nextRaw);
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        repository.findByTokenHash(TokenHash.sha256(rawRefreshToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
                repository.save(token);
            }
        });
    }

    public record RotatedRefresh(User user, String refreshToken) {}
}
