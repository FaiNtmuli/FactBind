package com.example.middemo.service;

import com.example.middemo.dto.common.PageResponse;
import com.example.middemo.dto.product.CreateProductRequest;
import com.example.middemo.dto.product.ProductResponse;
import com.example.middemo.dto.product.UpdateProductRequest;
import com.example.middemo.dto.product.UpdateProductStatusRequest;
import com.example.middemo.dto.product.UpdateProductStockRequest;
import com.example.middemo.entity.Product;
import com.example.middemo.entity.ProductStatus;
import com.example.middemo.exception.DuplicateSkuException;
import com.example.middemo.exception.ProductInUseException;
import com.example.middemo.exception.ProductNotFoundException;
import com.example.middemo.repository.OrderItemRepository;
import com.example.middemo.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    public ProductService(ProductRepository productRepository, OrderItemRepository orderItemRepository) {
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchProducts(String keyword, ProductStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Specification<Product> spec = Specification.allOf(
                ProductRepository.hasKeyword(keyword),
                ProductRepository.hasStatus(status)
        );
        Page<Product> result = productRepository.findAll(spec, pageable);
        return PageResponse.from(result, ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        return ProductResponse.from(requireProduct(id));
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateSkuException(request.sku());
        }
        Product product = new Product(
                request.name(),
                request.sku(),
                request.price(),
                request.stock(),
                request.status()
        );
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        Product product = requireProduct(id);
        if (!product.getSku().equalsIgnoreCase(request.sku())
                && productRepository.existsBySku(request.sku())) {
            throw new DuplicateSkuException(request.sku());
        }
        product.setName(request.name());
        product.setSku(request.sku());
        product.setPrice(request.price());
        product.setStock(request.stock());
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse updateStock(Long id, UpdateProductStockRequest request) {
        Product product = requireProduct(id);
        product.setStock(request.stock());
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse updateStatus(Long id, UpdateProductStatusRequest request) {
        Product product = requireProduct(id);
        product.setStatus(request.status());
        return ProductResponse.from(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = requireProduct(id);
        if (orderItemRepository.existsByProductId(product.getId())) {
            throw new ProductInUseException(product.getId());
        }
        productRepository.delete(product);
    }

    private Product requireProduct(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }
}
