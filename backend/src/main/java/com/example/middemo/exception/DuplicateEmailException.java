package com.example.middemo.exception;

public class DuplicateEmailException extends BusinessException {

    public DuplicateEmailException(String email) {
        super("DUPLICATE_EMAIL", "Email " + email + " already exists");
    }
}
