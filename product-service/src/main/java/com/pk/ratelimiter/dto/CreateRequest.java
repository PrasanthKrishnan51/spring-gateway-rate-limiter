package com.pk.ratelimiter.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class CreateRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 255)
    String name;

    String description;

    @NotBlank(message = "SKU is required")
    String sku;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    Double price;

    @NotBlank(message = "Category is required")
    String category;

    @Min(0)
    Integer stock;
}
