package com.example.middemo.service;

import com.example.middemo.dto.order.CreateOrderItemRequest;
import com.example.middemo.dto.order.CreateOrderRequest;
import com.example.middemo.dto.order.OrderResponse;
import com.example.middemo.dto.order.UpdateOrderStatusRequest;
import com.example.middemo.dto.product.CreateProductRequest;
import com.example.middemo.dto.user.CreateUserRequest;
import com.example.middemo.entity.OrderStatus;
import com.example.middemo.entity.ProductStatus;
import com.example.middemo.entity.UserStatus;
import com.example.middemo.exception.InsufficientStockException;
import com.example.middemo.exception.InvalidOrderStatusException;
import com.example.middemo.exception.OrderNotFoundException;
import com.example.middemo.exception.ProductNotFoundException;
import com.example.middemo.exception.ProductNotOnSaleException;
import com.example.middemo.exception.UserNotActiveException;
import com.example.middemo.exception.UserNotFoundException;
import com.example.middemo.repository.OrderRepository;
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

/**
 * Service level tests for the real business rules of the project: order creation, stock
 * handling, transaction rollback and the order status machine.
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Long activeUserId() {
        return userService.createUser(new CreateUserRequest(
                "Order User", "order-" + UUID.randomUUID() + "@example.com", 30, UserStatus.ACTIVE)).id();
    }

    private Long productId(String name, String price, int stock, ProductStatus status) {
        return productService.createProduct(new CreateProductRequest(
                name, "SKU-" + UUID.randomUUID().toString().substring(0, 8), new BigDecimal(price), stock, status)).id();
    }

    @Test
    @DisplayName("createOrder calculates totals on the server and decreases the stock")
    void createOrderCalculatesTotalsAndDecreasesStock() {
        Long userId = activeUserId();
        Long keyboardId = productId("Keyboard", "129.90", 10, ProductStatus.ON_SALE);
        Long mouseId = productId("Mouse", "49.50", 10, ProductStatus.ON_SALE);

        OrderResponse order = orderService.createOrder(new CreateOrderRequest(
                userId,
                "Please deliver soon",
                List.of(
                        new CreateOrderItemRequest(keyboardId, 2),
                        new CreateOrderItemRequest(mouseId, 1)
                )
        ));

        assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.totalAmount()).isEqualByComparingTo("309.30");
        assertThat(order.items()).hasSize(2);
        assertThat(order.items().getFirst().unitPrice()).isEqualByComparingTo("129.90");
        assertThat(order.items().getFirst().subtotal()).isEqualByComparingTo("259.80");
        assertThat(order.userId()).isEqualTo(userId);

        assertThat(productRepository.findById(keyboardId).orElseThrow().getStock()).isEqualTo(8);
        assertThat(productRepository.findById(mouseId).orElseThrow().getStock()).isEqualTo(9);
    }

    @Test
    @DisplayName("createOrder rejects an unknown user")
    void createOrderRejectsUnknownUser() {
        Long productId = productId("Keyboard", "10.00", 1, ProductStatus.ON_SALE);

        assertThatThrownBy(() -> orderService.createOrder(new CreateOrderRequest(
                999_999L, null, List.of(new CreateOrderItemRequest(productId, 1)))))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("createOrder rejects a DISABLED user")
    void createOrderRejectsDisabledUser() {
        Long userId = userService.createUser(new CreateUserRequest(
                "Disabled User", "disabled-" + UUID.randomUUID() + "@example.com", 30, UserStatus.DISABLED)).id();
        Long productId = productId("Keyboard", "10.00", 5, ProductStatus.ON_SALE);

        assertThatThrownBy(() -> orderService.createOrder(new CreateOrderRequest(
                userId, null, List.of(new CreateOrderItemRequest(productId, 1)))))
                .isInstanceOf(UserNotActiveException.class)
                .hasMessageContaining("is not ACTIVE");
    }

    @Test
    @DisplayName("createOrder rejects an unknown product")
    void createOrderRejectsUnknownProduct() {
        Long userId = activeUserId();

        assertThatThrownBy(() -> orderService.createOrder(new CreateOrderRequest(
                userId, null, List.of(new CreateOrderItemRequest(999_999L, 1)))))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("createOrder rejects a product that is not ON_SALE")
    void createOrderRejectsOffSaleProduct() {
        Long userId = activeUserId();
        Long productId = productId("Off Sale Product", "10.00", 5, ProductStatus.OFF_SALE);

        assertThatThrownBy(() -> orderService.createOrder(new CreateOrderRequest(
                userId, null, List.of(new CreateOrderItemRequest(productId, 1)))))
                .isInstanceOf(ProductNotOnSaleException.class);
    }

    @Test
    @DisplayName("createOrder rejects a quantity larger than the stock")
    void createOrderRejectsInsufficientStock() {
        Long userId = activeUserId();
        Long productId = productId("Rare Product", "10.00", 2, ProductStatus.ON_SALE);

        assertThatThrownBy(() -> orderService.createOrder(new CreateOrderRequest(
                userId, null, List.of(new CreateOrderItemRequest(productId, 5)))))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("only 2 item(s) in stock");
    }

    @Test
    @DisplayName("createOrder rolls the whole transaction back when one item fails")
    void createOrderRollsBackWhenOneItemFails() {
        Long userId = activeUserId();
        Long goodProductId = productId("Good Product", "10.00", 10, ProductStatus.ON_SALE);
        Long badProductId = productId("Bad Product", "10.00", 1, ProductStatus.ON_SALE);
        long ordersBefore = orderRepository.count();

        assertThatThrownBy(() -> orderService.createOrder(new CreateOrderRequest(
                userId,
                null,
                List.of(
                        new CreateOrderItemRequest(goodProductId, 3),
                        new CreateOrderItemRequest(badProductId, 99)
                ))))
                .isInstanceOf(InsufficientStockException.class);

        // The first item was already processed, but nothing may be persisted.
        assertThat(productRepository.findById(goodProductId).orElseThrow().getStock()).isEqualTo(10);
        assertThat(productRepository.findById(badProductId).orElseThrow().getStock()).isEqualTo(1);
        assertThat(orderRepository.count()).isEqualTo(ordersBefore);
    }

    @Test
    @DisplayName("updateOrderStatus walks the allowed transitions")
    void updateOrderStatusFollowsTheAllowedTransitions() {
        Long userId = activeUserId();
        Long productId = productId("Product", "10.00", 10, ProductStatus.ON_SALE);
        OrderResponse order = orderService.createOrder(new CreateOrderRequest(
                userId, null, List.of(new CreateOrderItemRequest(productId, 1))));

        OrderResponse paid = orderService.updateOrderStatus(order.id(), false, new UpdateOrderStatusRequest(OrderStatus.PAID));
        assertThat(paid.status()).isEqualTo(OrderStatus.PAID);

        OrderResponse completed = orderService.updateOrderStatus(order.id(), true, new UpdateOrderStatusRequest(OrderStatus.COMPLETED));
        assertThat(completed.status()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("updateOrderStatus allows CREATED -> CANCELLED")
    void updateOrderStatusAllowsCancellation() {
        Long userId = activeUserId();
        Long productId = productId("Product", "10.00", 10, ProductStatus.ON_SALE);
        OrderResponse order = orderService.createOrder(new CreateOrderRequest(
                userId, null, List.of(new CreateOrderItemRequest(productId, 1))));

        OrderResponse cancelled = orderService.updateOrderStatus(
                order.id(), false, new UpdateOrderStatusRequest(OrderStatus.CANCELLED));

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("updateOrderStatus rejects CANCELLED -> PAID")
    void updateOrderStatusRejectsCancelledToPaid() {
        Long userId = activeUserId();
        Long productId = productId("Product", "10.00", 10, ProductStatus.ON_SALE);
        OrderResponse order = orderService.createOrder(new CreateOrderRequest(
                userId, null, List.of(new CreateOrderItemRequest(productId, 1))));
        orderService.updateOrderStatus(order.id(), false, new UpdateOrderStatusRequest(OrderStatus.CANCELLED));

        assertThatThrownBy(() -> orderService.updateOrderStatus(
                order.id(), false, new UpdateOrderStatusRequest(OrderStatus.PAID)))
                .isInstanceOf(InvalidOrderStatusException.class)
                .hasMessageContaining("cannot change status from CANCELLED to PAID");
    }

    @Test
    @DisplayName("updateOrderStatus rejects an unknown order")
    void updateOrderStatusRejectsUnknownOrder() {
        assertThatThrownBy(() -> orderService.updateOrderStatus(
                777_777L, true, new UpdateOrderStatusRequest(OrderStatus.PAID)))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("searchOrders filters by user and status")
    void searchOrdersFilters() {
        Long userId = activeUserId();
        Long otherUserId = activeUserId();
        Long productId = productId("Product", "10.00", 20, ProductStatus.ON_SALE);

        orderService.createOrder(new CreateOrderRequest(userId, null, List.of(new CreateOrderItemRequest(productId, 1))));
        OrderResponse secondOrder = orderService.createOrder(
                new CreateOrderRequest(userId, null, List.of(new CreateOrderItemRequest(productId, 1))));
        orderService.updateOrderStatus(secondOrder.id(), false, new UpdateOrderStatusRequest(OrderStatus.PAID));
        orderService.createOrder(new CreateOrderRequest(otherUserId, null, List.of(new CreateOrderItemRequest(productId, 1))));

        assertThat(orderService.searchOrders(userId, null, 0, 20).totalElements()).isEqualTo(2);

        var paidOrders = orderService.searchOrders(userId, OrderStatus.PAID, 0, 20);
        assertThat(paidOrders.content()).hasSize(1);
        assertThat(paidOrders.content().getFirst().id()).isEqualTo(secondOrder.id());

        assertThat(orderService.searchOrders(otherUserId, null, 0, 20).totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("deleteOrder removes the order together with its items")
    void deleteOrder() {
        Long userId = activeUserId();
        Long productId = productId("Product", "10.00", 10, ProductStatus.ON_SALE);
        OrderResponse order = orderService.createOrder(new CreateOrderRequest(
                userId, null, List.of(new CreateOrderItemRequest(productId, 2))));

        orderService.deleteOrder(order.id());

        assertThat(orderRepository.findById(order.id())).isEmpty();
        // Stock is not restored when an order is deleted, see README "Known limitations".
        assertThat(productRepository.findById(productId).orElseThrow().getStock()).isEqualTo(8);
    }
}
