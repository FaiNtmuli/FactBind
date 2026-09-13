package com.example.middemo.service;

import com.example.middemo.dto.common.PageResponse;
import com.example.middemo.dto.product.CreateProductRequest;
import com.example.middemo.dto.product.ProductResponse;
import com.example.middemo.dto.product.UpdateProductRequest;
import com.example.middemo.dto.product.UpdateProductStatusRequest;
import com.example.middemo.dto.product.UpdateProductStockRequest;
import com.example.middemo.entity.ProductStatus;
import com.example.middemo.exception.DuplicateSkuException;
import com.example.middemo.exception.ProductInUseException;
import com.example.middemo.exception.ProductNotFoundException;
import com.example.middemo.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    private String uniqueSku() {
        return "SKU-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    @DisplayName("createProduct stores the product as ON_SALE by default")
    void createProductUsesDefaultStatus() {
        ProductResponse created = productService.createProduct(
                new CreateProductRequest("Test Keyboard", uniqueSku(), new BigDecimal("99.50"), 12, null));

        assertThat(created.id()).isNotNull();
        assertThat(created.status()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(created.price()).isEqualByComparingTo("99.50");
    }

    @Test
    @DisplayName("createProduct rejects a duplicated SKU")
    void createProductRejectsDuplicateSku() {
        String sku = uniqueSku();
        productService.createProduct(new CreateProductRequest("First", sku, new BigDecimal("10.00"), 1, null));

        assertThatThrownBy(() -> productService.createProduct(new CreateProductRequest("Second", sku, new BigDecimal("20.00"), 1, null)))
                .isInstanceOf(DuplicateSkuException.class)
                .hasMessageContaining(sku);
    }

    @Test
    @DisplayName("updateProduct replaces the editable fields")
    void updateProduct() {
        ProductResponse created = productService.createProduct(
                new CreateProductRequest("Old Name", uniqueSku(), new BigDecimal("10.00"), 5, null));
        String newSku = uniqueSku();

        ProductResponse updated = productService.updateProduct(created.id(),
                new UpdateProductRequest("New Name", newSku, new BigDecimal("15.75"), 9));

        assertThat(updated.name()).isEqualTo("New Name");
        assertThat(updated.sku()).isEqualTo(newSku);
        assertThat(updated.price()).isEqualByComparingTo("15.75");
        assertThat(updated.stock()).isEqualTo(9);
    }

    @Test
    @DisplayName("updateStock changes only the stock")
    void updateStock() {
        ProductResponse created = productService.createProduct(
                new CreateProductRequest("Stock Product", uniqueSku(), new BigDecimal("10.00"), 5, null));

        ProductResponse updated = productService.updateStock(created.id(), new UpdateProductStockRequest(0));

        assertThat(updated.stock()).isZero();
        assertThat(updated.name()).isEqualTo("Stock Product");
    }

    @Test
    @DisplayName("updateStatus takes a product off sale")
    void updateStatus() {
        ProductResponse created = productService.createProduct(
                new CreateProductRequest("Status Product", uniqueSku(), new BigDecimal("10.00"), 5, null));

        ProductResponse updated = productService.updateStatus(created.id(),
                new UpdateProductStatusRequest(ProductStatus.OFF_SALE));

        assertThat(updated.status()).isEqualTo(ProductStatus.OFF_SALE);
    }

    @Test
    @DisplayName("getProduct rejects an unknown id")
    void getProductRejectsUnknownId() {
        assertThatThrownBy(() -> productService.getProduct(888_888L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product 888888 not found");
    }

    @Test
    @DisplayName("searchProducts filters by keyword and status")
    void searchProductsFilters() {
        String token = "zzp" + UUID.randomUUID().toString().substring(0, 8);
        productService.createProduct(new CreateProductRequest("On sale " + token, uniqueSku(), new BigDecimal("10.00"), 5, null));
        ProductResponse offSale = productService.createProduct(
                new CreateProductRequest("Off sale " + token, uniqueSku(), new BigDecimal("10.00"), 5, ProductStatus.OFF_SALE));

        assertThat(productService.searchProducts(token, null, 0, 20).totalElements()).isEqualTo(2);

        PageResponse<ProductResponse> onSale = productService.searchProducts(token, ProductStatus.ON_SALE, 0, 20);
        assertThat(onSale.content()).hasSize(1);

        PageResponse<ProductResponse> off = productService.searchProducts(token, ProductStatus.OFF_SALE, 0, 20);
        assertThat(off.content()).hasSize(1);
        assertThat(off.content().getFirst().id()).isEqualTo(offSale.id());
    }

    @Test
    @DisplayName("deleteProduct removes an unused product")
    void deleteProduct() {
        ProductResponse created = productService.createProduct(
                new CreateProductRequest("Deletable", uniqueSku(), new BigDecimal("10.00"), 5, null));

        productService.deleteProduct(created.id());

        assertThat(productRepository.findById(created.id())).isEmpty();
    }

    @Test
    @DisplayName("deleteProduct is refused while the product is used by an order")
    void deleteProductInUseIsRefused() {
        var user = userService.createUser(new com.example.middemo.dto.user.CreateUserRequest(
                "Buyer", "buyer-" + UUID.randomUUID() + "@example.com", 30, null));
        ProductResponse product = productService.createProduct(
                new CreateProductRequest("Used Product", uniqueSku(), new BigDecimal("10.00"), 5, null));
        orderService.createOrder(new com.example.middemo.dto.order.CreateOrderRequest(
                user.id(), null, List.of(new com.example.middemo.dto.order.CreateOrderItemRequest(product.id(), 1))));

        assertThatThrownBy(() -> productService.deleteProduct(product.id()))
                .isInstanceOf(ProductInUseException.class);
        assertThat(productRepository.findById(product.id())).isPresent();
    }
}
