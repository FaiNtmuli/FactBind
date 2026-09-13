package com.example.middemo.exception;

import org.springframework.http.HttpStatus;

public class ProductNotFoundException extends BusinessException {

    public ProductNotFoundException(Long id) {
        super("PRODUCT_NOT_FOUND", "Product " + id + " not found", HttpStatus.NOT_FOUND);
    }
}
