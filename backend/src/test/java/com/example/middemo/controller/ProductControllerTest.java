package com.example.middemo.controller;

import com.example.middemo.dto.common.PageResponse;
import com.example.middemo.dto.product.ProductResponse;
import com.example.middemo.entity.ProductStatus;
import com.example.middemo.exception.DuplicateSkuException;
import com.example.middemo.exception.ProductNotFoundException;
import com.example.middemo.service.ProductService;
import com.example.middemo.web.ApiPaths;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2024-01-01T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    private ProductResponse sampleProduct(Long id, ProductStatus status) {
        return new ProductResponse(id, "Mechanical Keyboard", "SKU-1001", new BigDecimal("129.90"), 25, status, CREATED_AT, CREATED_AT);
    }

    @Test
    @DisplayName("GET /api/products binds every query parameter")
    void listProducts() throws Exception {
        given(productService.searchProducts("keyboard", ProductStatus.ON_SALE, 1, 10))
                .willReturn(new PageResponse<>(List.of(sampleProduct(3L, ProductStatus.ON_SALE)), 1, 10, 11, 2, false, true));

        mockMvc.perform(get(ApiPaths.PRODUCTS)
                        .param("keyword", "keyboard")
                        .param("status", "ON_SALE")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("SKU-1001"))
                .andExpect(jsonPath("$.content[0].price").exists())
                .andExpect(jsonPath("$.totalPages").value(2));

        verify(productService).searchProducts("keyboard", ProductStatus.ON_SALE, 1, 10);
    }

    @Test
    @DisplayName("GET /api/products returns 400 for an invalid page value")
    void listProductsRejectsNegativePage() throws Exception {
        mockMvc.perform(get(ApiPaths.PRODUCTS).param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /api/products/{id} returns 404 for an unknown product")
    void getProductNotFound() throws Exception {
        given(productService.getProduct(42L)).willThrow(new ProductNotFoundException(42L));

        mockMvc.perform(get(ApiPaths.withId(ApiPaths.PRODUCT_BY_ID, 42)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /api/products returns 201")
    void createProduct() throws Exception {
        given(productService.createProduct(any())).willReturn(sampleProduct(26L, ProductStatus.ON_SALE));

        String body = """
                {"name":"Mechanical Keyboard","sku":"SKU-1001","price":129.90,"stock":25,"status":"ON_SALE"}
                """;

        mockMvc.perform(post(ApiPaths.PRODUCTS).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", ApiPaths.withId(ApiPaths.PRODUCT_BY_ID, 26)));
    }

    @Test
    @DisplayName("POST /api/products returns 400 for a non positive price")
    void createProductRejectsNonPositivePrice() throws Exception {
        String body = """
                {"name":"Mechanical Keyboard","sku":"SKU-1001","price":0,"stock":25}
                """;

        mockMvc.perform(post(ApiPaths.PRODUCTS).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.price").exists());
    }

    @Test
    @DisplayName("POST /api/products returns 409 for a duplicated SKU")
    void createProductReturnsConflictForDuplicateSku() throws Exception {
        given(productService.createProduct(any())).willThrow(new DuplicateSkuException("SKU-1001"));

        String body = """
                {"name":"Mechanical Keyboard","sku":"SKU-1001","price":129.90,"stock":25}
                """;

        mockMvc.perform(post(ApiPaths.PRODUCTS).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_SKU"));
    }

    @Test
    @DisplayName("PUT /api/products/{id} updates a product")
    void updateProduct() throws Exception {
        given(productService.updateProduct(eq(1L), any())).willReturn(sampleProduct(1L, ProductStatus.ON_SALE));

        String body = """
                {"name":"Mechanical Keyboard","sku":"SKU-1001","price":139.00,"stock":20}
                """;

        mockMvc.perform(put(ApiPaths.withId(ApiPaths.PRODUCT_BY_ID, 1)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("PATCH /api/products/{id}/stock changes the stock")
    void updateStock() throws Exception {
        given(productService.updateStock(eq(1L), any())).willReturn(sampleProduct(1L, ProductStatus.ON_SALE));

        mockMvc.perform(patch(ApiPaths.withId(ApiPaths.PRODUCT_STOCK, 1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\":0}"))
                .andExpect(status().isOk());

        verify(productService).updateStock(eq(1L), any());
    }

    @Test
    @DisplayName("PATCH /api/products/{id}/stock returns 400 for a negative stock")
    void updateStockRejectsNegativeValue() throws Exception {
        mockMvc.perform(patch(ApiPaths.withId(ApiPaths.PRODUCT_STOCK, 1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.stock").exists());
    }

    @Test
    @DisplayName("PATCH /api/products/{id}/status changes the sale status")
    void updateStatus() throws Exception {
        given(productService.updateStatus(eq(1L), any())).willReturn(sampleProduct(1L, ProductStatus.OFF_SALE));

        mockMvc.perform(patch(ApiPaths.withId(ApiPaths.PRODUCT_STATUS, 1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OFF_SALE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OFF_SALE"));
    }

    @Test
    @DisplayName("DELETE /api/products/{id} returns 204")
    void deleteProduct() throws Exception {
        mockMvc.perform(delete(ApiPaths.withId(ApiPaths.PRODUCT_BY_ID, 1)))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(1L);
    }

    @Test
    @DisplayName("GET /api/products/not-a-number returns 400")
    void getProductWithBadPathVariable() throws Exception {
        mockMvc.perform(get(ApiPaths.withId(ApiPaths.PRODUCT_BY_ID, "not-a-number")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }
}
