package com.pk.ratelimiter.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UpdateRequest {
    @NotBlank
    String name;

    String description;

    @NotNull
    @Positive
    Double price;

    @NotBlank
    String category;

    @Min(0)
    Integer stock;
}
