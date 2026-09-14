package com.example.middemo.exception;

public class UserNotActiveException extends BusinessException {

    public UserNotActiveException(Long id) {
        super("USER_NOT_ACTIVE", "User " + id + " is not ACTIVE and cannot place orders");
    }
}
