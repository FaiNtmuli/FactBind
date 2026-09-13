package com.example.middemo.dto.product;

import com.example.middemo.entity.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateProductStatusRequest(

        @NotNull(message = "must not be null")
        ProductStatus status
) {
}
