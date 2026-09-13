package com.example.middemo.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException(Long id) {
        super("USER_NOT_FOUND", "User " + id + " not found", HttpStatus.NOT_FOUND);
    }
}
