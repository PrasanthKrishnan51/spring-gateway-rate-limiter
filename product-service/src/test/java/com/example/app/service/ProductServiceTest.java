package com.example.app.service;

import com.example.app.dto.ProductDto;
import com.example.app.exception.ProductException;
import com.example.app.model.Product;
import com.example.app.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

    @Mock  ProductRepository productRepository;
    @InjectMocks ProductService productService;

    Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = Product.builder()
                .id("abc123")
                .name("Wireless Mouse")
                .description("Ergonomic wireless mouse")
                .sku("SKU-001")
                .price(29.99)
                .category("Electronics")
                .stock(50)
                .build();
    }

    @Nested
    @DisplayName("getAllProducts()")
    class GetAllProducts {

        @Test
        @DisplayName("returns mapped DTOs for every product in repository")
        void returnsAllProducts() {
            when(productRepository.findAll()).thenReturn(List.of(sampleProduct));

            List<ProductDto.Response> result = productService.getAllProducts();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("Wireless Mouse");
            verify(productRepository).findAll();
        }

        @Test
        @DisplayName("returns empty list when repository is empty")
        void returnsEmptyList() {
            when(productRepository.findAll()).thenReturn(List.of());
            assertThat(productService.getAllProducts()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getProductById()")
    class GetProductById {

        @Test
        @DisplayName("returns DTO when product exists")
        void found() {
            when(productRepository.findById("abc123")).thenReturn(Optional.of(sampleProduct));
            ProductDto.Response dto = productService.getProductById("abc123");
            assertThat(dto.id()).isEqualTo("abc123");
            assertThat(dto.sku()).isEqualTo("SKU-001");
        }

        @Test
        @DisplayName("throws ProductNotFoundException when product does not exist")
        void notFound() {
            when(productRepository.findById("missing")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> productService.getProductById("missing"))
                    .isInstanceOf(ProductException.ProductNotFoundException.class)
                    .hasMessageContaining("missing");
        }
    }

    @Nested
    @DisplayName("createProduct()")
    class CreateProduct {

        ProductDto.CreateRequest validRequest = new ProductDto.CreateRequest(
                "Mechanical Keyboard",
                "RGB backlit mechanical keyboard",
                "SKU-002",
                89.99,
                "Electronics",
                20
        );

        @Test
        @DisplayName("saves and returns new product when SKU is unique")
        void successfulCreate() {
            when(productRepository.existsBySku("SKU-002")).thenReturn(false);
            when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
                Product p = inv.getArgument(0);
                p.setId("newId");
                return p;
            });

            ProductDto.Response result = productService.createProduct(validRequest);

            assertThat(result.name()).isEqualTo("Mechanical Keyboard");
            assertThat(result.id()).isEqualTo("newId");
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("throws DuplicateSkuException when SKU already exists")
        void duplicateSku() {
            when(productRepository.existsBySku("SKU-002")).thenReturn(true);

            assertThatThrownBy(() -> productService.createProduct(validRequest))
                    .isInstanceOf(ProductException.DuplicateSkuException.class)
                    .hasMessageContaining("SKU-002");

            verify(productRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteProduct()")
    class DeleteProduct {

        @Test
        @DisplayName("deletes product when it exists")
        void successfulDelete() {
            when(productRepository.existsById("abc123")).thenReturn(true);
            productService.deleteProduct("abc123");
            verify(productRepository).deleteById("abc123");
        }

        @Test
        @DisplayName("throws ProductNotFoundException when product does not exist")
        void notFound() {
            when(productRepository.existsById("ghost")).thenReturn(false);
            assertThatThrownBy(() -> productService.deleteProduct("ghost"))
                    .isInstanceOf(ProductException.ProductNotFoundException.class);
            verify(productRepository, never()).deleteById(any());
        }
    }
}
