package com.example.middemo.dto.order;

import com.example.middemo.entity.Order;
import com.example.middemo.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Full order representation: order fields + basic user information + item list.
 */
public record OrderResponse(
        Long id,
        Long userId,
        String userName,
        String userEmail,
        OrderStatus status,
        BigDecimal totalAmount,
        String remark,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getUser().getName(),
                order.getUser().getEmail(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getRemark(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
