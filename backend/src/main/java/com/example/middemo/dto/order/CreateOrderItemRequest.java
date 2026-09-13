package com.example.middemo.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateOrderItemRequest(

        @NotNull(message = "must not be null")
        Long productId,

        @NotNull(message = "must not be null")
        @Min(value = 1, message = "must be greater than or equal to 1")
        Integer quantity
) {
}
