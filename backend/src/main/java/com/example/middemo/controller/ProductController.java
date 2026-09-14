package com.example.middemo.controller;

import com.example.middemo.dto.common.PageResponse;
import com.example.middemo.dto.product.CreateProductRequest;
import com.example.middemo.dto.product.ProductResponse;
import com.example.middemo.dto.product.UpdateProductRequest;
import com.example.middemo.dto.product.UpdateProductStatusRequest;
import com.example.middemo.dto.product.UpdateProductStockRequest;
import com.example.middemo.entity.ProductStatus;
import com.example.middemo.service.ProductService;
import com.example.middemo.factbind.FactBind;
import com.example.middemo.factbind.FactBindParam;
import com.example.middemo.factbind.ContractRegistry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

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
    private final ContractRegistry contract;

    public ProductController(ProductService productService, ContractRegistry contract) {
        this.productService = productService;
        this.contract = contract;
    }

    /** {@code GET /api/products?keyword=keyboard&status=ON_SALE&page=0&size=20} */
    @FactBind("Product.List")
    public PageResponse<ProductResponse> listProducts(
            @FactBindParam String keyword,
            @FactBindParam ProductStatus status,
            @FactBindParam @Min(0) int page,
            @FactBindParam @Min(1) @Max(100) int size
    ) {
        return productService.searchProducts(keyword, status, page, size);
    }

    /** {@code GET /api/products/{id}} */
    @FactBind("Product.Get")
    public ProductResponse getProduct(@FactBindParam Long id) {
        return productService.getProduct(id);
    }

    /** {@code POST /api/products} */
    @FactBind("Product.Create")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse created = productService.createProduct(request);
        return ResponseEntity
                .created(URI.create(contract.path("Product.Get", Map.of("id", created.id()))))
                .body(created);
    }

    /** {@code PUT /api/products/{id}} */
    @FactBind("Product.Update")
    public ProductResponse updateProduct(
            @FactBindParam Long id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        return productService.updateProduct(id, request);
    }

    /** {@code PATCH /api/products/{id}/stock} */
    @FactBind("Product.UpdateStock")
    public ProductResponse updateStock(
            @FactBindParam Long id,
            @Valid @RequestBody UpdateProductStockRequest request
    ) {
        return productService.updateStock(id, request);
    }

    /** {@code PATCH /api/products/{id}/status} */
    @FactBind("Product.UpdateStatus")
    public ProductResponse updateStatus(
            @FactBindParam Long id,
            @Valid @RequestBody UpdateProductStatusRequest request
    ) {
        return productService.updateStatus(id, request);
    }

    /** {@code DELETE /api/products/{id}} */
    @FactBind("Product.Delete")
    public ResponseEntity<Void> deleteProduct(@FactBindParam Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
