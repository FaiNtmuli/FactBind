package com.example.middemo.exception;

import org.springframework.http.HttpStatus;

public class OrderNotFoundException extends BusinessException {

    public OrderNotFoundException(Long id) {
        super("ORDER_NOT_FOUND", "Order " + id + " not found", HttpStatus.NOT_FOUND);
    }
}
