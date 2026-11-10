package com.fetch.backend.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        Instant timestamp,
        String path,
        String correlationId,
        ErrorBody error
) {
    public record ErrorBody(String code, String message, List<FieldDetail> details) {
        public ErrorBody(String code, String message) {
            this(code, message, null);
        }
    }

    public record FieldDetail(String field, String message) {
    }
}
