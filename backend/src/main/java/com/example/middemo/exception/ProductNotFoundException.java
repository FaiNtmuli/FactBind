package com.example.middemo.exception;

public class ProductNotFoundException extends BusinessException {

    public ProductNotFoundException(Long id) {
        super("PRODUCT_NOT_FOUND", "Product " + id + " not found");
    }
}
