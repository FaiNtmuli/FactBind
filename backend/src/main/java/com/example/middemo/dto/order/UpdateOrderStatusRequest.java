package com.example.middemo.dto.order;

import com.example.middemo.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(

        @NotNull(message = "must not be null")
        OrderStatus status
) {
}
