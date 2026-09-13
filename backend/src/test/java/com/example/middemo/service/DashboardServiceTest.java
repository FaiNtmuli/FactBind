package com.example.middemo.service;

import com.example.middemo.dto.dashboard.DashboardSummaryResponse;
import com.example.middemo.dto.order.CreateOrderItemRequest;
import com.example.middemo.dto.order.CreateOrderRequest;
import com.example.middemo.dto.order.OrderSummaryResponse;
import com.example.middemo.dto.product.CreateProductRequest;
import com.example.middemo.dto.user.CreateUserRequest;
import com.example.middemo.entity.ProductStatus;
import com.example.middemo.entity.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DashboardServiceTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Test
    @DisplayName("getSummary counts users, products and orders")
    void getSummaryCountsEverything() {
        DashboardSummaryResponse before = dashboardService.getSummary();

        var user = userService.createUser(new CreateUserRequest(
                "Dashboard User", "dash-" + UUID.randomUUID() + "@example.com", 30, UserStatus.ACTIVE));
        var disabledUser = userService.createUser(new CreateUserRequest(
                "Dashboard Disabled", "dash-disabled-" + UUID.randomUUID() + "@example.com", 30, UserStatus.DISABLED));
        productService.createProduct(new CreateProductRequest(
                "Dashboard Product", "SKU-" + UUID.randomUUID().toString().substring(0, 8), new BigDecimal("10.00"), 5, ProductStatus.ON_SALE));
        productService.createProduct(new CreateProductRequest(
                "Dashboard Off Sale", "SKU-" + UUID.randomUUID().toString().substring(0, 8), new BigDecimal("10.00"), 5, ProductStatus.OFF_SALE));

        DashboardSummaryResponse after = dashboardService.getSummary();

        assertThat(after.userCount()).isEqualTo(before.userCount() + 2);
        assertThat(after.activeUserCount()).isEqualTo(before.activeUserCount() + 1);
        assertThat(after.productCount()).isEqualTo(before.productCount() + 2);
        assertThat(after.onSaleProductCount()).isEqualTo(before.onSaleProductCount() + 1);
        assertThat(after.orderCount()).isEqualTo(before.orderCount());
        assertThat(disabledUser.status()).isEqualTo(UserStatus.DISABLED);
        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("getSummary counts CREATED orders")
    void getSummaryCountsCreatedOrders() {
        DashboardSummaryResponse before = dashboardService.getSummary();

        var user = userService.createUser(new CreateUserRequest(
                "Dashboard Buyer", "dash-buyer-" + UUID.randomUUID() + "@example.com", 30, UserStatus.ACTIVE));
        var product = productService.createProduct(new CreateProductRequest(
                "Dashboard Item", "SKU-" + UUID.randomUUID().toString().substring(0, 8), new BigDecimal("10.00"), 5, ProductStatus.ON_SALE));
        orderService.createOrder(new CreateOrderRequest(
                user.id(), null, List.of(new CreateOrderItemRequest(product.id(), 1))));

        DashboardSummaryResponse after = dashboardService.getSummary();

        assertThat(after.orderCount()).isEqualTo(before.orderCount() + 1);
        assertThat(after.createdOrderCount()).isEqualTo(before.createdOrderCount() + 1);
    }

    @Test
    @DisplayName("getRecentOrders returns at most the requested number of orders, newest first")
    void getRecentOrdersReturnsNewestFirst() {
        var user = userService.createUser(new CreateUserRequest(
                "Recent Buyer", "recent-" + UUID.randomUUID() + "@example.com", 30, UserStatus.ACTIVE));
        var product = productService.createProduct(new CreateProductRequest(
                "Recent Item", "SKU-" + UUID.randomUUID().toString().substring(0, 8), new BigDecimal("10.00"), 5, ProductStatus.ON_SALE));
        var first = orderService.createOrder(new CreateOrderRequest(
                user.id(), null, List.of(new CreateOrderItemRequest(product.id(), 1))));
        var second = orderService.createOrder(new CreateOrderRequest(
                user.id(), null, List.of(new CreateOrderItemRequest(product.id(), 2))));

        List<OrderSummaryResponse> recent = dashboardService.getRecentOrders(5);

        assertThat(recent).hasSizeLessThanOrEqualTo(5);
        assertThat(recent.getFirst().id()).isEqualTo(second.id());
        assertThat(recent.getFirst().itemCount()).isEqualTo(1);
        assertThat(recent).extracting(OrderSummaryResponse::id).contains(first.id(), second.id());
    }
}
