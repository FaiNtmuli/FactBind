package com.example.middemo.service;

import com.example.middemo.dto.dashboard.DashboardSummaryResponse;
import com.example.middemo.dto.order.OrderSummaryResponse;
import com.example.middemo.entity.OrderStatus;
import com.example.middemo.entity.ProductStatus;
import com.example.middemo.entity.UserStatus;
import com.example.middemo.repository.OrderRepository;
import com.example.middemo.repository.ProductRepository;
import com.example.middemo.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public DashboardService(UserRepository userRepository, ProductRepository productRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        return new DashboardSummaryResponse(
                userRepository.count(),
                userRepository.countByStatus(UserStatus.ACTIVE),
                productRepository.count(),
                productRepository.countByStatus(ProductStatus.ON_SALE),
                orderRepository.count(),
                orderRepository.countByStatus(OrderStatus.CREATED)
        );
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getRecentOrders(int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return orderRepository.findAll(pageRequest).stream()
                .map(OrderSummaryResponse::from)
                .toList();
    }
}
