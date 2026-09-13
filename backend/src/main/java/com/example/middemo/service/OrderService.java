package com.example.middemo.service;

import com.example.middemo.dto.common.PageResponse;
import com.example.middemo.dto.order.CreateOrderItemRequest;
import com.example.middemo.dto.order.CreateOrderRequest;
import com.example.middemo.dto.order.OrderResponse;
import com.example.middemo.dto.order.UpdateOrderStatusRequest;
import com.example.middemo.entity.Order;
import com.example.middemo.entity.OrderItem;
import com.example.middemo.entity.OrderStatus;
import com.example.middemo.entity.Product;
import com.example.middemo.entity.User;
import com.example.middemo.exception.InsufficientStockException;
import com.example.middemo.exception.InvalidOrderStatusException;
import com.example.middemo.exception.OrderNotFoundException;
import com.example.middemo.exception.ProductNotFoundException;
import com.example.middemo.exception.ProductNotOnSaleException;
import com.example.middemo.exception.UserNotActiveException;
import com.example.middemo.exception.UserNotFoundException;
import com.example.middemo.repository.OrderRepository;
import com.example.middemo.repository.ProductRepository;
import com.example.middemo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, UserRepository userRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> searchOrders(Long userId, OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Specification<Order> spec = Specification.allOf(
                OrderRepository.hasUserId(userId),
                OrderRepository.hasStatus(status)
        );
        Page<Order> result = orderRepository.findAll(spec, pageable);
        return PageResponse.from(result, OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id) {
        return OrderResponse.from(requireOrder(id));
    }

    /**
     * Creates an order and decreases the stock of every ordered product.
     *
     * <p>Order creation, item creation and stock updates share one transaction: if any item fails
     * (missing product, product not on sale, not enough stock) the whole order is rolled back.
     *
     * <p>{@code totalAmount}, {@code unitPrice} and {@code subtotal} are always calculated on the
     * server, the client can never send them.
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));
        if (!user.isActive()) {
            throw new UserNotActiveException(user.getId());
        }

        Order order = new Order(user, request.remark());
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CreateOrderItemRequest requestedItem : request.items()) {
            Product product = productRepository.findById(requestedItem.productId())
                    .orElseThrow(() -> new ProductNotFoundException(requestedItem.productId()));
            if (!product.isOnSale()) {
                throw new ProductNotOnSaleException(product.getId());
            }
            if (product.getStock() < requestedItem.quantity()) {
                throw new InsufficientStockException(product.getId(), requestedItem.quantity(), product.getStock());
            }

            product.decreaseStock(requestedItem.quantity());
            OrderItem item = new OrderItem(product, requestedItem.quantity());
            order.addItem(item);
            totalAmount = totalAmount.add(item.getSubtotal());
        }

        order.setTotalAmount(totalAmount);
        Order saved = orderRepository.save(order);
        log.info("Order {} created for user {} with total {}", saved.getId(), user.getId(), saved.getTotalAmount());
        return OrderResponse.from(saved);
    }

    /**
     * Changes the status of an order following the {@link OrderStatus} transition rules.
     *
     * @param notify comes from the {@code ?notify=true} query parameter; the first version only
     *               writes a log line, it does not send any real notification.
     */
    @Transactional
    public OrderResponse updateOrderStatus(Long id, Boolean notify, UpdateOrderStatusRequest request) {
        Order order = requireOrder(id);
        OrderStatus current = order.getStatus();
        OrderStatus target = request.status();
        if (!current.canTransitionTo(target)) {
            throw new InvalidOrderStatusException(order.getId(), current, target);
        }

        order.setStatus(target);
        if (Boolean.TRUE.equals(notify)) {
            log.info("notification requested: order {} changed from {} to {}", order.getId(), current, target);
        }
        return OrderResponse.from(order);
    }

    @Transactional
    public void deleteOrder(Long id) {
        orderRepository.delete(requireOrder(id));
    }

    private Order requireOrder(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }
}
