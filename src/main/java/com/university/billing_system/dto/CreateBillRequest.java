package com.university.billing_system.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateBillRequest(
        @NotEmpty(message = "A bill needs at least one item")
        @Valid List<AddItemRequest> items) {
}