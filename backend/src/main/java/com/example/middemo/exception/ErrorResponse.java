package com.example.middemo.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Stable error body returned by every failing endpoint.
 *
 * <pre>
 * { "code": "USER_NOT_FOUND", "message": "User 100 not found" }
 * { "code": "VALIDATION_ERROR", "message": "Validation failed", "fields": { "email": "..." } }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String code, String message, Map<String, String> fields) {

    public ErrorResponse(String code, String message) {
        this(code, message, null);
    }
}
