package com.university.billing_system.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddItemRequest(
        @NotNull(message = "Product id is required") Long productId,
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than 0") Integer quantity) {
}