package com.example.middemo.exception;

public class ProductInUseException extends BusinessException {

    public ProductInUseException(Long id) {
        super("PRODUCT_IN_USE", "Product " + id + " is referenced by existing orders and cannot be deleted");
    }
}
