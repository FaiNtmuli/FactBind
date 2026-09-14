package com.example.middemo.exception;

public class InsufficientStockException extends BusinessException {

    public InsufficientStockException(Long productId, int requested, int available) {
        super(
                "INSUFFICIENT_STOCK",
                "Product " + productId + " has only " + available + " item(s) in stock, but " + requested + " requested"
        );
    }
}
