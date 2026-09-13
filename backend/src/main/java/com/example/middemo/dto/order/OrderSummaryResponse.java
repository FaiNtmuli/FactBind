package com.example.middemo.dto.order;

import com.example.middemo.entity.Order;
import com.example.middemo.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Lightweight order representation used by the dashboard.
 */
public record OrderSummaryResponse(
        Long id,
        Long userId,
        String userName,
        OrderStatus status,
        BigDecimal totalAmount,
        int itemCount,
        Instant createdAt
) {

    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getUser().getId(),
                order.getUser().getName(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getItems().size(),
                order.getCreatedAt()
        );
    }
}
