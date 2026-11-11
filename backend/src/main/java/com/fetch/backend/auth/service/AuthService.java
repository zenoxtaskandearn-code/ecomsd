package com.fetch.backend.auth.service;

import com.fetch.backend.auth.dto.AuthDtos;
import com.fetch.backend.auth.entity.EmailVerificationToken;
import com.fetch.backend.auth.entity.PasswordResetToken;
import com.fetch.backend.auth.repo.EmailVerificationTokenRepository;
import com.fetch.backend.auth.repo.PasswordResetTokenRepository;
import com.fetch.backend.common.error.BadRequestException;
import com.fetch.backend.common.error.ConflictException;
import com.fetch.backend.common.error.NotFoundException;
import com.fetch.backend.common.error.UnauthorizedException;
import com.fetch.backend.common.service.NotificationService;
import com.fetch.backend.security.jwt.JwtService;
import com.fetch.backend.user.dto.UserDtos;
import com.fetch.backend.user.entity.User;
import com.fetch.backend.user.repo.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final NotificationService notificationService;
    private final String frontendBaseUrl;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, RefreshTokenService refreshTokenService,
                       EmailVerificationTokenRepository emailVerificationTokenRepository, PasswordResetTokenRepository passwordResetTokenRepository,
                       NotificationService notificationService,
                       @Value("${app.frontend-base-url:${FRONTEND_BASE_URL:https://lupineandleash-frontend.onrender.com}}") String frontendBaseUrl) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.notificationService = notificationService;
        this.frontendBaseUrl = frontendBaseUrl == null ? "https://lupineandleash-frontend.onrender.com" : frontendBaseUrl.replaceAll("/+$", "");
    }

    @Transactional
    public AuthDtos.RegisterResponse register(AuthDtos.RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(trimToNull(request.firstName()));
        user.setLastName(trimToNull(request.lastName()));
        user.setPhone(trimToNull(request.phone()));
        user.setRole("CUSTOMER");
        user.setStatus("PENDING_VERIFICATION");
        user.setEmailVerifiedAt(null);
        userRepository.save(user);

        String rawToken = issueEmailVerificationToken(user, 24 * 60); // 24 hours
        String link = frontendBaseUrl + "/activate-account?token=" + rawToken;
        notificationService.enqueue("ACCOUNT_ACTIVATION", "EMAIL", user.getEmail(), "Activate your Lupine & Leash account",
                null, user.getId(),
                Map.of("email", user.getEmail(), "activationLink", link, "expiresInMinutes", 24 * 60, "name", safeName(user)),
                "user:" + user.getId() + ":activation:" + tokenFingerprint(rawToken));

        return new AuthDtos.RegisterResponse(true,
                "Registration successful. Please check your email for a one-time activation link to activate your account.",
                user.getEmail(), true);
    }

    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        if ("PENDING_VERIFICATION".equalsIgnoreCase(user.getStatus()) || user.getEmailVerifiedAt() == null) {
            throw new UnauthorizedException("Please activate your account using the one-time link sent to your email before signing in.");
        }
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new UnauthorizedException("Account is not active");
        }
        return tokensFor(user);
    }

    @Transactional
    public AuthDtos.SimpleMessageResponse activateAccount(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) throw new BadRequestException("Activation token is required");
        EmailVerificationToken token = emailVerificationTokenRepository.findByTokenHash(TokenHash.sha256(rawToken.trim()))
                .orElseThrow(() -> new BadRequestException("Invalid or expired activation link"));
        if (!token.isActive()) throw new BadRequestException("Activation link is invalid, expired, or already used");
        User user = token.getUser();
        token.setUsedAt(Instant.now());
        emailVerificationTokenRepository.save(token);
        // invalidate other unused tokens
        try { emailVerificationTokenRepository.deleteByUserIdAndUsedAtIsNull(user.getId()); } catch (Exception ignored) {}
        user.setEmailVerifiedAt(Instant.now());
        user.setStatus("ACTIVE");
        userRepository.save(user);
        return new AuthDtos.SimpleMessageResponse(true, "Account activated successfully. You can now sign in.");
    }

    @Transactional
    public AuthDtos.SimpleMessageResponse resendActivation(String emailRaw) {
        String email = emailRaw == null ? "" : emailRaw.trim().toLowerCase();
        // Always return generic success to avoid account enumeration
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            if (user.getEmailVerifiedAt() != null && "ACTIVE".equalsIgnoreCase(user.getStatus())) return;
            String rawToken = issueEmailVerificationToken(user, 24 * 60);
            String link = frontendBaseUrl + "/activate-account?token=" + rawToken;
            notificationService.enqueue("ACCOUNT_ACTIVATION_RESEND", "EMAIL", user.getEmail(), "Activate your Lupine & Leash account",
                    null, user.getId(),
                    Map.of("email", user.getEmail(), "activationLink", link, "expiresInMinutes", 24 * 60, "name", safeName(user)),
                    "user:" + user.getId() + ":activation-resend:" + tokenFingerprint(rawToken));
        });
        return new AuthDtos.SimpleMessageResponse(true, "If an account exists for this email and is not yet activated, a new activation link has been sent.");
    }

    @Transactional
    public AuthDtos.SimpleMessageResponse forgotPassword(String emailRaw) {
        String email = emailRaw == null ? "" : emailRaw.trim().toLowerCase();
        // Generic response to avoid account enumeration
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            // Invalidate previous unused reset tokens
            try { passwordResetTokenRepository.deleteByUserIdAndUsedAtIsNull(user.getId()); } catch (Exception ignored) {}
            String rawToken = TokenHash.randomToken();
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);
            token.setTokenHash(TokenHash.sha256(rawToken));
            token.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
            passwordResetTokenRepository.save(token);
            String link = frontendBaseUrl + "/reset-password?token=" + rawToken;
            notificationService.enqueue("PASSWORD_RESET", "EMAIL", user.getEmail(), "Reset your Lupine & Leash password",
                    null, user.getId(),
                    Map.of("email", user.getEmail(), "resetLink", link, "expiresInMinutes", 30, "name", safeName(user)),
                    "user:" + user.getId() + ":pwdreset:" + tokenFingerprint(rawToken));
        });
        return new AuthDtos.SimpleMessageResponse(true, "If an account exists for this email, a password reset link has been sent.");
    }

    @Transactional
    public AuthDtos.SimpleMessageResponse resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) throw new BadRequestException("Reset token is required");
        if (newPassword == null || newPassword.length() < 8) throw new BadRequestException("Password must be at least 8 characters");
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(TokenHash.sha256(rawToken.trim()))
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset link"));
        if (!token.isActive()) throw new BadRequestException("Reset link is invalid, expired, or already used");
        User user = token.getUser();
        token.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(token);
        try { passwordResetTokenRepository.deleteByUserIdAndUsedAtIsNull(user.getId()); } catch (Exception ignored) {}
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return new AuthDtos.SimpleMessageResponse(true, "Password reset successfully. You can now sign in with your new password.");
    }

    @Transactional
    public AuthDtos.AuthResponse refresh(AuthDtos.RefreshRequest request) {
        RefreshTokenService.RotatedRefresh rotated = refreshTokenService.rotate(request.refreshToken());
        User user = rotated.user();
        if (user.getEmailVerifiedAt() == null || "PENDING_VERIFICATION".equalsIgnoreCase(user.getStatus())) {
            throw new UnauthorizedException("Account is not activated");
        }
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

    private String issueEmailVerificationToken(User user, int minutes) {
        try { emailVerificationTokenRepository.deleteByUserIdAndUsedAtIsNull(user.getId()); } catch (Exception ignored) {}
        String rawToken = TokenHash.randomToken();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(TokenHash.sha256(rawToken));
        token.setExpiresAt(Instant.now().plus(minutes, ChronoUnit.MINUTES));
        emailVerificationTokenRepository.save(token);
        return rawToken;
    }

    private String safeName(User user) {
        String fn = trimToNull(user.getFirstName());
        return fn == null ? "there" : fn;
    }
    private String tokenFingerprint(String raw) {
        try { return TokenHash.sha256(raw).substring(0, 12); } catch (Exception e) { return String.valueOf(System.currentTimeMillis()); }
    }
    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

