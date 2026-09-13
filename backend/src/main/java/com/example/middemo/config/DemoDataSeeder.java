package com.example.middemo.config;

import com.example.middemo.entity.Order;
import com.example.middemo.entity.OrderItem;
import com.example.middemo.entity.OrderStatus;
import com.example.middemo.entity.Product;
import com.example.middemo.entity.ProductStatus;
import com.example.middemo.entity.User;
import com.example.middemo.entity.UserStatus;
import com.example.middemo.repository.OrderRepository;
import com.example.middemo.repository.ProductRepository;
import com.example.middemo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Inserts a fixed demo data set on startup, so that search, filtering and pagination can be
 * tried out immediately.
 *
 * <p>The data is deterministic: a fixed pseudo random seed and fixed name tables are used, so
 * every fresh database ends up with the same rows.
 *
 * <p>Rows are only inserted when the database is still empty.
 */
@Component
@ConditionalOnProperty(name = "middemo.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final String[] FIRST_NAMES = {
            "Alice", "Bob", "Carol", "David", "Emma",
            "Frank", "Grace", "Henry", "Iris", "Jack",
            "Karen", "Liam", "Mia", "Noah", "Olivia",
            "Peter", "Quinn", "Rachel", "Sam", "Tina"
    };

    private static final String[] LAST_NAMES = {
            "Anderson", "Brown", "Clark", "Davis", "Evans",
            "Foster", "Green", "Harris", "Irving", "Johnson"
    };

    private static final String[] PRODUCT_NAMES = {
            "Mechanical Keyboard", "Wireless Mouse", "27 Inch Monitor", "USB-C Hub", "Laptop Stand",
            "Noise Cancelling Headphones", "Webcam 1080p", "External SSD 1TB", "Desk Lamp", "Ergonomic Chair",
            "Standing Desk", "Bluetooth Speaker", "Graphics Tablet", "Microphone Arm", "Cable Organizer",
            "Laptop Sleeve", "Wireless Charger", "Portable Projector", "Smart Plug", "HDMI Cable 2m",
            "Laser Printer", "Document Scanner", "Network Switch 8 Port", "Wi-Fi Router", "UPS 650VA"
    };

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public DemoDataSeeder(
            UserRepository userRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository
    ) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0 || productRepository.count() > 0 || orderRepository.count() > 0) {
            log.info("Demo data already present, seeding is skipped");
            return;
        }

        List<User> users = userRepository.saveAll(buildUsers());
        List<Product> products = productRepository.saveAll(buildProducts());
        List<Order> orders = orderRepository.saveAll(buildOrders(users, products));

        log.info("Seeded {} users, {} products and {} orders", users.size(), products.size(), orders.size());
    }

    private List<User> buildUsers() {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            String name = FIRST_NAMES[i] + " " + LAST_NAMES[i % LAST_NAMES.length];
            String email = FIRST_NAMES[i].toLowerCase() + "." + LAST_NAMES[i % LAST_NAMES.length].toLowerCase() + "@example.com";
            // Every 5th user is disabled, so status filtering always returns something.
            UserStatus status = (i % 5 == 4) ? UserStatus.DISABLED : UserStatus.ACTIVE;
            users.add(new User(name, email, 20 + i % 30, status));
        }
        return users;
    }

    private List<Product> buildProducts() {
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < PRODUCT_NAMES.length; i++) {
            BigDecimal price = new BigDecimal("19.90").add(BigDecimal.valueOf(i * 13L));
            // Every 6th product is off sale.
            ProductStatus status = (i % 6 == 5) ? ProductStatus.OFF_SALE : ProductStatus.ON_SALE;
            products.add(new Product(
                    PRODUCT_NAMES[i],
                    String.format("SKU-%04d", 1001 + i),
                    price,
                    5 + (i * 3) % 60,
                    status
            ));
        }
        return products;
    }

    private List<Order> buildOrders(List<User> users, List<Product> products) {
        Random random = new Random(20240101L);
        List<Order> orders = new ArrayList<>();

        for (int i = 0; i < 40; i++) {
            User user = users.get(random.nextInt(users.size()));
            Order order = new Order(user, "Seed order #" + (i + 1));

            BigDecimal total = BigDecimal.ZERO;
            int itemCount = 1 + random.nextInt(3);
            for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
                Product product = products.get(random.nextInt(products.size()));
                int quantity = 1 + random.nextInt(3);
                OrderItem item = new OrderItem(product, quantity);
                order.addItem(item);
                total = total.add(item.getSubtotal());
            }
            order.setTotalAmount(total);
            order.setStatus(randomStatus(random));
            orders.add(order);
        }
        return orders;
    }

    private OrderStatus randomStatus(Random random) {
        return switch (random.nextInt(10)) {
            case 0, 1, 2 -> OrderStatus.CREATED;
            case 3, 4, 5 -> OrderStatus.PAID;
            case 6, 7 -> OrderStatus.COMPLETED;
            default -> OrderStatus.CANCELLED;
        };
    }
}
