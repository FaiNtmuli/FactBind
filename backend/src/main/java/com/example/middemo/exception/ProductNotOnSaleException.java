package com.example.middemo.exception;

import org.springframework.http.HttpStatus;

public class ProductNotOnSaleException extends BusinessException {

    public ProductNotOnSaleException(Long id) {
        super("PRODUCT_NOT_ON_SALE", "Product " + id + " is not ON_SALE and cannot be ordered", HttpStatus.CONFLICT);
    }
}
