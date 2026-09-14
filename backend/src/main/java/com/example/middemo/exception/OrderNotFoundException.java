package com.example.middemo.exception;

public class OrderNotFoundException extends BusinessException {

    public OrderNotFoundException(Long id) {
        super("ORDER_NOT_FOUND", "Order " + id + " not found");
    }
}
