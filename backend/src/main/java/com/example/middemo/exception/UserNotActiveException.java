package com.example.middemo.exception;

import org.springframework.http.HttpStatus;

public class UserNotActiveException extends BusinessException {

    public UserNotActiveException(Long id) {
        super("USER_NOT_ACTIVE", "User " + id + " is not ACTIVE and cannot place orders", HttpStatus.CONFLICT);
    }
}
