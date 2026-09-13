package com.example.middemo.dto.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateProductStockRequest(

        @NotNull(message = "must not be null")
        @Min(value = 0, message = "must be greater than or equal to 0")
        Integer stock
) {
}
