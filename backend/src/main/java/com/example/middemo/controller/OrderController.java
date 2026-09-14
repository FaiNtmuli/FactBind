package com.example.middemo.controller;

import com.example.middemo.dto.common.PageResponse;
import com.example.middemo.dto.order.CreateOrderRequest;
import com.example.middemo.dto.order.OrderResponse;
import com.example.middemo.dto.order.UpdateOrderStatusRequest;
import com.example.middemo.entity.OrderStatus;
import com.example.middemo.service.OrderService;
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
 * Order endpoints.
 *
 * <p>The status endpoint is intentionally the most complex binding of the whole API:
 * path parameter + query parameter ({@code notify}, optional with default value) + JSON body.
 */
@RestController
@Validated
public class OrderController {

    private final OrderService orderService;
    private final ContractRegistry contract;

    public OrderController(OrderService orderService, ContractRegistry contract) {
        this.orderService = orderService;
        this.contract = contract;
    }

    /** {@code GET /api/orders?userId=1&status=PAID&page=0&size=20} */
    @FactBind("Order.List")
    public PageResponse<OrderResponse> listOrders(
            @FactBindParam Long userId,
            @FactBindParam OrderStatus status,
            @FactBindParam @Min(0) int page,
            @FactBindParam @Min(1) @Max(100) int size
    ) {
        return orderService.searchOrders(userId, status, page, size);
    }

    /** {@code GET /api/orders/{id}} */
    @FactBind("Order.Get")
    public OrderResponse getOrder(@FactBindParam Long id) {
        return orderService.getOrder(id);
    }

    /** {@code POST /api/orders} - totals are always calculated by the server. */
    @FactBind("Order.Create")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse created = orderService.createOrder(request);
        return ResponseEntity
                .created(URI.create(contract.path("Order.Get", Map.of("id", created.id()))))
                .body(created);
    }

    /** {@code PATCH /api/orders/{id}/status?notify=true} */
    @FactBind("Order.UpdateStatus")
    public OrderResponse updateOrderStatus(
            @FactBindParam Long id,
            @FactBindParam Boolean notify,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        return orderService.updateOrderStatus(id, notify, request);
    }

    /** {@code DELETE /api/orders/{id}} */
    @FactBind("Order.Delete")
    public ResponseEntity<Void> deleteOrder(@FactBindParam Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}
