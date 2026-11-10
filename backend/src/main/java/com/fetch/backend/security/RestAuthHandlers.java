package com.fetch.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fetch.backend.common.api.ApiErrorResponse;
import com.fetch.backend.common.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class RestAuthHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    public RestAuthHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        write(response, request, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        write(response, request, HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied");
    }

    private void write(HttpServletResponse response, HttpServletRequest request, HttpStatus status, String code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                request.getRequestURI(),
                MDC.get(CorrelationIdFilter.MDC_KEY),
                new ApiErrorResponse.ErrorBody(code, message)
        );
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
