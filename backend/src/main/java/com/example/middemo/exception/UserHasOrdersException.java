package com.example.middemo.exception;

public class UserHasOrdersException extends BusinessException {

    public UserHasOrdersException(Long id) {
        super("USER_HAS_ORDERS", "User " + id + " still has orders and cannot be deleted");
    }
}
