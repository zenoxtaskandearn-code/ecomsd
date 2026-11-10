package com.fetch.backend.auth.service;

import com.fetch.backend.auth.dto.AuthDtos;
import com.fetch.backend.common.error.ConflictException;
import com.fetch.backend.common.error.UnauthorizedException;
import com.fetch.backend.security.jwt.JwtService;
import com.fetch.backend.user.dto.UserDtos;
import com.fetch.backend.user.entity.User;
import com.fetch.backend.user.repo.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("An account with this email already exists");
        }
        User user = new User();
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(trimToNull(request.firstName()));
        user.setLastName(trimToNull(request.lastName()));
        user.setPhone(trimToNull(request.phone()));
        user.setRole("CUSTOMER");
        user.setStatus("ACTIVE");
        userRepository.save(user);
        return tokensFor(user);
    }

    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus()) || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        return tokensFor(user);
    }

    @Transactional
    public AuthDtos.AuthResponse refresh(AuthDtos.RefreshRequest request) {
        RefreshTokenService.RotatedRefresh rotated = refreshTokenService.rotate(request.refreshToken());
        User user = rotated.user();
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new UnauthorizedException("Account is not active");
        }
        return new AuthDtos.AuthResponse(
                jwtService.generateAccessToken(user),
                rotated.refreshToken(),
                "Bearer",
                jwtService.getAccessTtlSeconds(),
                UserDtos.UserResponse.from(user)
        );
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revoke(rawRefreshToken);
        }
    }

    private AuthDtos.AuthResponse tokensFor(User user) {
        return new AuthDtos.AuthResponse(
                jwtService.generateAccessToken(user),
                refreshTokenService.issue(user),
                "Bearer",
                jwtService.getAccessTtlSeconds(),
                UserDtos.UserResponse.from(user)
        );
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
