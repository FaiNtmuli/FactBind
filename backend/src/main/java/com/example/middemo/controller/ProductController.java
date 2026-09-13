package com.example.middemo.controller;

import com.example.middemo.dto.common.PageResponse;
import com.example.middemo.dto.product.CreateProductRequest;
import com.example.middemo.dto.product.ProductResponse;
import com.example.middemo.dto.product.UpdateProductRequest;
import com.example.middemo.dto.product.UpdateProductStatusRequest;
import com.example.middemo.dto.product.UpdateProductStockRequest;
import com.example.middemo.entity.ProductStatus;
import com.example.middemo.service.ProductService;
import com.example.middemo.web.ApiPaths;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Product endpoints.
 *
 * <p>Binding styles covered here: query parameters, path parameters and JSON bodies,
 * including two different {@code PATCH} endpoints for stock and sale status.
 */
@RestController
@Validated
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /** {@code GET /api/products?keyword=keyboard&status=ON_SALE&page=0&size=20} */
    @GetMapping(ApiPaths.PRODUCTS)
    public PageResponse<ProductResponse> listProducts(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) ProductStatus status,
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return productService.searchProducts(keyword, status, page, size);
    }

    /** {@code GET /api/products/{id}} */
    @GetMapping(ApiPaths.PRODUCT_BY_ID)
    public ProductResponse getProduct(@PathVariable("id") Long id) {
        return productService.getProduct(id);
    }

    /** {@code POST /api/products} */
    @PostMapping(ApiPaths.PRODUCTS)
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse created = productService.createProduct(request);
        return ResponseEntity
                .created(URI.create(ApiPaths.withId(ApiPaths.PRODUCT_BY_ID, created.id())))
                .body(created);
    }

    /** {@code PUT /api/products/{id}} */
    @PutMapping(ApiPaths.PRODUCT_BY_ID)
    public ProductResponse updateProduct(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        return productService.updateProduct(id, request);
    }

    /** {@code PATCH /api/products/{id}/stock} */
    @PatchMapping(ApiPaths.PRODUCT_STOCK)
    public ProductResponse updateStock(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateProductStockRequest request
    ) {
        return productService.updateStock(id, request);
    }

    /** {@code PATCH /api/products/{id}/status} */
    @PatchMapping(ApiPaths.PRODUCT_STATUS)
    public ProductResponse updateStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateProductStatusRequest request
    ) {
        return productService.updateStatus(id, request);
    }

    /** {@code DELETE /api/products/{id}} */
    @DeleteMapping(ApiPaths.PRODUCT_BY_ID)
    public ResponseEntity<Void> deleteProduct(@PathVariable("id") Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
