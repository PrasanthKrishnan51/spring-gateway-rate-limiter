package com.pk.ratelimiter.dto;

import com.pk.ratelimiter.model.Product;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.time.Instant;

/**
 * DTOs to decouple the API contract from the internal domain model.
 */
public class ProductDto {

    // ─────────────────────────────────── Request ────────────────────────────
    @Builder
    public record CreateRequest(
            @NotBlank(message = "Name is required")
            @Size(max = 255)
            String name,

            String description,

            @NotBlank(message = "SKU is required")
            String sku,

            @NotNull(message = "Price is required")
            @Positive(message = "Price must be positive")
            Double price,

            @NotBlank(message = "Category is required")
            String category,

            @Min(0) Integer stock
    ) {}

    @Builder
    public record UpdateRequest(
            @NotBlank String name,
            String description,
            @NotNull @Positive Double price,
            @NotBlank String category,
            @Min(0) Integer stock
    ) {}

    // ─────────────────────────────────── Response ───────────────────────────
    @Builder
    public record Response(
            String id,
            String name,
            String description,
            String sku,
            Double price,
            String category,
            Integer stock,
            Instant createdAt,
            Instant updatedAt
    ) {
        /** Map a Product entity to a Response DTO. */
        public static Response from(Product product) {
            return Response.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .description(product.getDescription())
                    .sku(product.getSku())
                    .price(product.getPrice())
                    .category(product.getCategory())
                    .stock(product.getStock())
                    .createdAt(product.getCreatedAt())
                    .updatedAt(product.getUpdatedAt())
                    .build();
        }
    }
}
