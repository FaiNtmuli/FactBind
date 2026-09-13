package com.example.middemo.exception;

import org.springframework.http.HttpStatus;

public class DuplicateSkuException extends BusinessException {

    public DuplicateSkuException(String sku) {
        super("DUPLICATE_SKU", "SKU " + sku + " already exists", HttpStatus.CONFLICT);
    }
}
