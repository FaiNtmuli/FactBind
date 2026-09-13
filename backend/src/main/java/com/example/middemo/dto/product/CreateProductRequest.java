package com.example.middemo.dto.product;

import com.example.middemo.entity.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProductRequest(

        @NotBlank(message = "must not be blank")
        @Size(max = 100, message = "must be at most 100 characters")
        String name,

        @NotBlank(message = "must not be blank")
        @Size(max = 50, message = "must be at most 50 characters")
        String sku,

        @NotNull(message = "must not be null")
        @DecimalMin(value = "0.01", message = "must be greater than 0")
        @Digits(integer = 10, fraction = 2, message = "must have at most 10 integer digits and 2 decimals")
        BigDecimal price,

        @NotNull(message = "must not be null")
        @Min(value = 0, message = "must be greater than or equal to 0")
        Integer stock,

        ProductStatus status
) {
}
