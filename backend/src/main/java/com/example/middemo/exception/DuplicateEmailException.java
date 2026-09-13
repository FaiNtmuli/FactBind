package com.example.middemo.exception;

import org.springframework.http.HttpStatus;

public class DuplicateEmailException extends BusinessException {

    public DuplicateEmailException(String email) {
        super("DUPLICATE_EMAIL", "Email " + email + " already exists", HttpStatus.CONFLICT);
    }
}
