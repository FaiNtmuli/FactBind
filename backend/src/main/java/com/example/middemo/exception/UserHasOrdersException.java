package com.example.middemo.exception;

import org.springframework.http.HttpStatus;

public class UserHasOrdersException extends BusinessException {

    public UserHasOrdersException(Long id) {
        super("USER_HAS_ORDERS", "User " + id + " still has orders and cannot be deleted", HttpStatus.CONFLICT);
    }
}
