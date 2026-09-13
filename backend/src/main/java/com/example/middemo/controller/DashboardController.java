package com.example.middemo.controller;

import com.example.middemo.dto.dashboard.DashboardSummaryResponse;
import com.example.middemo.dto.order.OrderSummaryResponse;
import com.example.middemo.service.DashboardService;
import com.example.middemo.web.ApiPaths;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** {@code GET /api/dashboard/summary} */
    @GetMapping(ApiPaths.DASHBOARD_SUMMARY)
    public DashboardSummaryResponse getSummary() {
        return dashboardService.getSummary();
    }

    /** {@code GET /api/dashboard/recent-orders?limit=5} */
    @GetMapping(ApiPaths.DASHBOARD_RECENT_ORDERS)
    public List<OrderSummaryResponse> getRecentOrders(
            @RequestParam(value = "limit", defaultValue = "5") @Min(1) @Max(20) int limit
    ) {
        return dashboardService.getRecentOrders(limit);
    }
}
