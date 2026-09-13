package com.example.middemo.api;

import com.example.middemo.dto.product.CreateProductRequest;
import com.example.middemo.dto.product.ProductResponse;
import com.example.middemo.dto.user.CreateUserRequest;
import com.example.middemo.dto.user.UserResponse;
import com.example.middemo.entity.ProductStatus;
import com.example.middemo.service.ProductService;
import com.example.middemo.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.closeTo;

/**
 * Full stack test: real controller, real service, real repository, real database.
 *
 * <p>The whole {@code POST /api/orders -> GET /api/orders/{id} -> PATCH /api/orders/{id}/status}
 * round trip is exercised through HTTP.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    private UserResponse createUser() {
        return userService.createUser(new CreateUserRequest(
                "Integration User", "integration-" + UUID.randomUUID() + "@example.com", 30, null));
    }

    private ProductResponse createProduct(int stock) {
        return productService.createProduct(new CreateProductRequest(
                "Integration Product",
                "SKU-" + UUID.randomUUID().toString().substring(0, 8),
                new BigDecimal("25.50"),
                stock,
                ProductStatus.ON_SALE));
    }

    @Test
    @DisplayName("an order can be created, read back and paid through the HTTP API")
    void orderRoundTrip() throws Exception {
        UserResponse user = createUser();
        ProductResponse product = createProduct(10);

        String createBody = """
                {
                  "userId": %d,
                  "remark": "integration test",
                  "items": [{"productId": %d, "quantity": 4}]
                }
                """.formatted(user.id(), product.id());

        MvcResult created = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(closeTo(102.0, 0.001)))
                .andExpect(jsonPath("$.items[0].quantity").value(4))
                .andReturn();

        String location = created.getResponse().getHeader("Location");

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.userEmail").value(user.email()));

        mockMvc.perform(patch(location + "/status").param("notify", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PAID\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        mockMvc.perform(patch(location + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CREATED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_ORDER_STATUS"));
    }

    @Test
    @DisplayName("the stock of the ordered product is decreased by the API call")
    void orderCreationDecreasesStock() throws Exception {
        UserResponse user = createUser();
        ProductResponse product = createProduct(3);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": %d, "items": [{"productId": %d, "quantity": 3}]}
                                """.formatted(user.id(), product.id())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/products/" + product.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(0));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": %d, "items": [{"productId": %d, "quantity": 1}]}
                                """.formatted(user.id(), product.id())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
    }

    @Test
    @DisplayName("GET /api/dashboard/summary and the list endpoints answer over HTTP")
    void dashboardAndListsAnswerOverHttp() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userCount").isNumber());

        mockMvc.perform(get("/api/users").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2));

        mockMvc.perform(get("/api/products").param("page", "0").param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(3));

        mockMvc.perform(get("/api/orders").param("status", "CREATED"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/dashboard/recent-orders").param("limit", "3"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("an unknown API path returns the 404 error body")
    void unknownPathReturnsErrorBody() throws Exception {
        mockMvc.perform(get("/api/unknown-module"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
