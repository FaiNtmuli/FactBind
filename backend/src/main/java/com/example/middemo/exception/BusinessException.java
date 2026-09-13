package com.example.middemo.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for every expected business failure.
 *
 * <p>{@code code} is a stable machine readable identifier that is returned to the client,
 * {@code status} is the HTTP status that should be used for the response.
 */
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public BusinessException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
