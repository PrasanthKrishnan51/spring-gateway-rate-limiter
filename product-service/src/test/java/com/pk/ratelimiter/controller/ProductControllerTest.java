package com.pk.ratelimiter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pk.ratelimiter.dto.CreateRequest;
import com.pk.ratelimiter.dto.ProductListResponse;
import com.pk.ratelimiter.dto.ProductResponse;
import com.pk.ratelimiter.exception.ProductException;
import com.pk.ratelimiter.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@DisplayName("ProductController Integration Tests")
class ProductControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean
    ProductService productService;

    private ProductResponse sampleResponse() {
        return new ProductResponse(
                "id1", "Wireless Mouse", "Ergonomic wireless mouse",
                "SKU-001", 29.99, "Electronics", 50,
                Instant.now(), Instant.now()
        );
    }

    @Nested
    @DisplayName("GET /api/v1/products")
    class GetAll {

        @Test
        @DisplayName("returns 200 with list of products")
        void getAllProducts() throws Exception {
            when(productService.getAllProducts()).thenReturn(new ProductListResponse((List<ProductResponse>) sampleResponse()));

            mockMvc.perform(get("/api/v1/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name", is("Wireless Mouse")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products/{id}")
    class GetById {

        @Test
        @DisplayName("returns 200 and product when found")
        void found() throws Exception {
            when(productService.getProductById("id1")).thenReturn(sampleResponse());

            mockMvc.perform(get("/api/v1/products/id1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is("id1")))
                    .andExpect(jsonPath("$.sku", is("SKU-001")));
        }

        @Test
        @DisplayName("returns 404 when product not found")
        void notFound() throws Exception {
            when(productService.getProductById("missing"))
                    .thenThrow(new ProductException.ProductNotFoundException("missing"));

            mockMvc.perform(get("/api/v1/products/missing"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/products")
    class Create {

        @Test
        @DisplayName("returns 201 on valid create request")
        void created() throws Exception {
            var req = new CreateRequest(
                    "Wireless Mouse", "Ergonomic wireless mouse",
                    "SKU-001", 29.99, "Electronics", 50);

            when(productService.createProduct(any())).thenReturn(sampleResponse());

            mockMvc.perform(post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.sku", is("SKU-001")));
        }

        @Test
        @DisplayName("returns 400 when request body is invalid")
        void validationFail() throws Exception {
            var bad = new CreateRequest("", null, "", -1.0, "", -5);

            mockMvc.perform(post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/products/{id}")
    class Delete {

        @Test
        @DisplayName("returns 200 with confirmation message")
        void deleted() throws Exception {
            mockMvc.perform(delete("/api/v1/products/id1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", is("Product deleted successfully")));
        }
    }
}
