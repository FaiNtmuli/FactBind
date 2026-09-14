package com.example.middemo.exception;

public class DuplicateSkuException extends BusinessException {

    public DuplicateSkuException(String sku) {
        super("DUPLICATE_SKU", "SKU " + sku + " already exists");
    }
}
