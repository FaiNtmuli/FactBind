package com.example.middemo.controller;

import com.example.middemo.dto.common.PageResponse;
import com.example.middemo.dto.order.OrderItemResponse;
import com.example.middemo.dto.order.OrderResponse;
import com.example.middemo.entity.OrderStatus;
import com.example.middemo.exception.InvalidOrderStatusException;
import com.example.middemo.exception.OrderNotFoundException;
import com.example.middemo.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2024-01-01T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    private OrderResponse sampleOrder(Long id, OrderStatus status) {
        return new OrderResponse(
                id, 1L, "Alice Anderson", "alice@example.com", status,
                new BigDecimal("259.80"), "Please deliver soon",
                List.of(
                        new OrderItemResponse(1L, 10L, "Mechanical Keyboard", new BigDecimal("129.90"), 2, new BigDecimal("259.80"))
                ),
                CREATED_AT, CREATED_AT
        );
    }

    @Test
    @DisplayName("GET /api/orders binds userId, status and pagination")
    void listOrders() throws Exception {
        given(orderService.searchOrders(1L, OrderStatus.PAID, 0, 20))
                .willReturn(new PageResponse<>(List.of(sampleOrder(5L, OrderStatus.PAID)), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/api/orders")
                        .param("userId", "1")
                        .param("status", "PAID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value(1))
                .andExpect(jsonPath("$.content[0].items[0].productName").value("Mechanical Keyboard"));

        verify(orderService).searchOrders(1L, OrderStatus.PAID, 0, 20);
    }

    @Test
    @DisplayName("GET /api/orders/{id} returns the order with its items")
    void getOrder() throws Exception {
        given(orderService.getOrder(5L)).willReturn(sampleOrder(5L, OrderStatus.CREATED));

        mockMvc.perform(get("/api/orders/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    @DisplayName("GET /api/orders/{id} returns 404 for an unknown order")
    void getOrderNotFound() throws Exception {
        given(orderService.getOrder(404L)).willThrow(new OrderNotFoundException(404L));

        mockMvc.perform(get("/api/orders/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /api/orders returns 201 and the server side total")
    void createOrder() throws Exception {
        given(orderService.createOrder(any())).willReturn(sampleOrder(100L, OrderStatus.CREATED));

        String body = """
                {
                  "userId": 1,
                  "remark": "Please deliver soon",
                  "items": [
                    {"productId": 10, "quantity": 2},
                    {"productId": 20, "quantity": 1}
                  ]
                }
                """;

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/orders/100"))
                .andExpect(jsonPath("$.totalAmount").exists());
    }

    @Test
    @DisplayName("POST /api/orders returns 400 when the item list is empty")
    void createOrderRejectsEmptyItems() throws Exception {
        String body = """
                {"userId": 1, "items": []}
                """;

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.items").value("must contain at least one item"));
    }

    @Test
    @DisplayName("POST /api/orders returns 400 when a nested item is invalid")
    void createOrderRejectsInvalidNestedItem() throws Exception {
        String body = """
                {"userId": 1, "items": [{"productId": 10, "quantity": 0}]}
                """;

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields['items[0].quantity']").exists());
    }

    @Test
    @DisplayName("POST /api/orders returns 409 when the product stock is not enough")
    void createOrderReturnsConflictOnInsufficientStock() throws Exception {
        given(orderService.createOrder(any()))
                .willThrow(new com.example.middemo.exception.InsufficientStockException(10L, 5, 2));

        String body = """
                {"userId": 1, "items": [{"productId": 10, "quantity": 5}]}
                """;

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/status uses notify=false by default")
    void updateOrderStatusUsesNotifyDefault() throws Exception {
        given(orderService.updateOrderStatus(eq(5L), eq(false), any()))
                .willReturn(sampleOrder(5L, OrderStatus.PAID));

        mockMvc.perform(patch("/api/orders/5/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PAID\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        verify(orderService).updateOrderStatus(5L, false, new com.example.middemo.dto.order.UpdateOrderStatusRequest(OrderStatus.PAID));
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/status?notify=true binds the query parameter")
    void updateOrderStatusBindsNotifyQueryParameter() throws Exception {
        given(orderService.updateOrderStatus(eq(5L), eq(true), any()))
                .willReturn(sampleOrder(5L, OrderStatus.COMPLETED));

        mockMvc.perform(patch("/api/orders/5/status")
                        .param("notify", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk());

        verify(orderService).updateOrderStatus(eq(5L), eq(true), any());
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/status returns 409 for a forbidden transition")
    void updateOrderStatusReturnsConflictForInvalidTransition() throws Exception {
        given(orderService.updateOrderStatus(eq(5L), any(), any()))
                .willThrow(new InvalidOrderStatusException(5L, OrderStatus.CANCELLED, OrderStatus.PAID));

        mockMvc.perform(patch("/api/orders/5/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PAID\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_ORDER_STATUS"));
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/status returns 400 when the body has no status")
    void updateOrderStatusRequiresBody() throws Exception {
        mockMvc.perform(patch("/api/orders/5/status").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.status").exists());
    }

    @Test
    @DisplayName("DELETE /api/orders/{id} returns 204")
    void deleteOrder() throws Exception {
        mockMvc.perform(delete("/api/orders/5"))
                .andExpect(status().isNoContent());

        verify(orderService).deleteOrder(5L);
    }
}
