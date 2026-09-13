package com.example.middemo.exception;

import com.example.middemo.entity.OrderStatus;
import org.springframework.http.HttpStatus;

public class InvalidOrderStatusException extends BusinessException {

    public InvalidOrderStatusException(Long orderId, OrderStatus from, OrderStatus to) {
        super(
                "INVALID_ORDER_STATUS",
                "Order " + orderId + " cannot change status from " + from + " to " + to,
                HttpStatus.CONFLICT
        );
    }
}
