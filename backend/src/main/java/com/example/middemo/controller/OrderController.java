package com.example.middemo.controller;

import com.example.middemo.dto.common.PageResponse;
import com.example.middemo.dto.order.CreateOrderRequest;
import com.example.middemo.dto.order.OrderResponse;
import com.example.middemo.dto.order.UpdateOrderStatusRequest;
import com.example.middemo.entity.OrderStatus;
import com.example.middemo.service.OrderService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Order endpoints.
 *
 * <p>The status endpoint is intentionally the most complex binding of the whole API:
 * path parameter + query parameter ({@code notify}, optional with default value) + JSON body.
 */
@RestController
@Validated
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** {@code GET /api/orders?userId=1&status=PAID&page=0&size=20} */
    @GetMapping(ApiPaths.ORDERS)
    public PageResponse<OrderResponse> listOrders(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "status", required = false) OrderStatus status,
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return orderService.searchOrders(userId, status, page, size);
    }

    /** {@code GET /api/orders/{id}} */
    @GetMapping(ApiPaths.ORDER_BY_ID)
    public OrderResponse getOrder(@PathVariable("id") Long id) {
        return orderService.getOrder(id);
    }

    /** {@code POST /api/orders} - totals are always calculated by the server. */
    @PostMapping(ApiPaths.ORDERS)
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse created = orderService.createOrder(request);
        return ResponseEntity
                .created(URI.create(ApiPaths.withId(ApiPaths.ORDER_BY_ID, created.id())))
                .body(created);
    }

    /** {@code PATCH /api/orders/{id}/status?notify=true} */
    @PatchMapping(ApiPaths.ORDER_STATUS)
    public OrderResponse updateOrderStatus(
            @PathVariable("id") Long id,
            @RequestParam(value = "notify", required = false, defaultValue = "false") Boolean notify,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        return orderService.updateOrderStatus(id, notify, request);
    }

    /** {@code DELETE /api/orders/{id}} */
    @DeleteMapping(ApiPaths.ORDER_BY_ID)
    public ResponseEntity<Void> deleteOrder(@PathVariable("id") Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}
