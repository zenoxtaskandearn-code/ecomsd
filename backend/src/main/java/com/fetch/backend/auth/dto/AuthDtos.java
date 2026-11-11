package com.fetch.backend.auth.dto;

import com.fetch.backend.user.dto.UserDtos;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {
    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8, max = 128) String password,
            String firstName,
            String lastName,
            String phone
    ) {}

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresInSeconds,
            UserDtos.UserResponse user
    ) {}

    public record RegisterResponse(boolean success, String message, String email, boolean requiresActivation) {}
    public record SimpleMessageResponse(boolean success, String message) {}
    public record VerifyEmailRequest(@NotBlank String token) {}
    public record ResendVerificationRequest(@Email @NotBlank String email) {}
    public record ForgotPasswordRequest(@Email @NotBlank String email) {}
    public record ResetPasswordRequest(@NotBlank String token, @NotBlank @Size(min = 8, max = 128) String newPassword) {}
}
