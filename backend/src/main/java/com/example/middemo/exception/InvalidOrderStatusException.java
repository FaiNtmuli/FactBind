package com.example.middemo.exception;

import com.example.middemo.entity.OrderStatus;

public class InvalidOrderStatusException extends BusinessException {

    public InvalidOrderStatusException(Long orderId, OrderStatus from, OrderStatus to) {
        super(
                "INVALID_ORDER_STATUS",
                "Order " + orderId + " cannot change status from " + from + " to " + to
        );
    }
}
