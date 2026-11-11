package com.fetch.backend.auth.web;

import com.fetch.backend.auth.dto.AuthDtos;
import com.fetch.backend.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDtos.RegisterResponse register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthDtos.AuthResponse refresh(@Valid @RequestBody AuthDtos.RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody(required = false) AuthDtos.RefreshRequest request) {
        authService.logout(request == null ? null : request.refreshToken());
    }

    @PostMapping("/verify-email")
    public AuthDtos.SimpleMessageResponse verifyEmail(@Valid @RequestBody AuthDtos.VerifyEmailRequest request) {
        return authService.activateAccount(request.token());
    }

    @PostMapping("/resend-verification")
    public AuthDtos.SimpleMessageResponse resendVerification(@Valid @RequestBody AuthDtos.ResendVerificationRequest request) {
        return authService.resendActivation(request.email());
    }

    @PostMapping("/forgot-password")
    public AuthDtos.SimpleMessageResponse forgotPassword(@Valid @RequestBody AuthDtos.ForgotPasswordRequest request) {
        return authService.forgotPassword(request.email());
    }

    @PostMapping("/reset-password")
    public AuthDtos.SimpleMessageResponse resetPassword(@Valid @RequestBody AuthDtos.ResetPasswordRequest request) {
        return authService.resetPassword(request.token(), request.newPassword());
    }
}
