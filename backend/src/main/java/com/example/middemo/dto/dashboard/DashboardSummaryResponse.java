package com.example.middemo.dto.dashboard;

public record DashboardSummaryResponse(
        long userCount,
        long activeUserCount,
        long productCount,
        long onSaleProductCount,
        long orderCount,
        long createdOrderCount
) {
}
