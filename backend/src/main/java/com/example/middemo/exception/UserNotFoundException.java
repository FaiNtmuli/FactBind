package com.example.middemo.exception;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException(Long id) {
        super("USER_NOT_FOUND", "User " + id + " not found");
    }
}
