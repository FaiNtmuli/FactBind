package com.example.middemo.controller;

import com.example.middemo.dto.dashboard.DashboardSummaryResponse;
import com.example.middemo.dto.order.OrderSummaryResponse;
import com.example.middemo.entity.OrderStatus;
import com.example.middemo.service.DashboardService;
import com.example.middemo.web.ApiPaths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    @DisplayName("GET /api/dashboard/summary returns every counter")
    void getSummary() throws Exception {
        given(dashboardService.getSummary()).willReturn(new DashboardSummaryResponse(20, 16, 25, 21, 40, 12));

        mockMvc.perform(get(ApiPaths.DASHBOARD_SUMMARY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userCount").value(20))
                .andExpect(jsonPath("$.activeUserCount").value(16))
                .andExpect(jsonPath("$.productCount").value(25))
                .andExpect(jsonPath("$.onSaleProductCount").value(21))
                .andExpect(jsonPath("$.orderCount").value(40))
                .andExpect(jsonPath("$.createdOrderCount").value(12));
    }

    @Test
    @DisplayName("GET /api/dashboard/recent-orders uses limit=5 by default")
    void getRecentOrders() throws Exception {
        given(dashboardService.getRecentOrders(5)).willReturn(List.of(
                new OrderSummaryResponse(9L, 3L, "Carol Clark", OrderStatus.PAID, new BigDecimal("199.80"), 2, Instant.parse("2024-01-01T10:00:00Z"))
        ));

        mockMvc.perform(get(ApiPaths.DASHBOARD_RECENT_ORDERS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9))
                .andExpect(jsonPath("$[0].itemCount").value(2))
                .andExpect(jsonPath("$[0].status").value("PAID"));

        verify(dashboardService).getRecentOrders(5);
    }

    @Test
    @DisplayName("GET /api/dashboard/recent-orders binds a custom limit")
    void getRecentOrdersWithLimit() throws Exception {
        given(dashboardService.getRecentOrders(10)).willReturn(List.of());

        mockMvc.perform(get(ApiPaths.DASHBOARD_RECENT_ORDERS).param("limit", "10"))
                .andExpect(status().isOk());

        verify(dashboardService).getRecentOrders(10);
    }

    @Test
    @DisplayName("GET /api/dashboard/recent-orders returns 400 when limit is too large")
    void getRecentOrdersRejectsTooLargeLimit() throws Exception {
        mockMvc.perform(get(ApiPaths.DASHBOARD_RECENT_ORDERS).param("limit", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
