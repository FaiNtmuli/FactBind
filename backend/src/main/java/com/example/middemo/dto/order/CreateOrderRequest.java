package com.example.middemo.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(

        @NotNull(message = "must not be null")
        Long userId,

        @Size(max = 255, message = "must be at most 255 characters")
        String remark,

        @NotEmpty(message = "must contain at least one item")
        List<@Valid CreateOrderItemRequest> items
) {
}
