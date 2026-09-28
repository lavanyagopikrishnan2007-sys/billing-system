package com.university.billing_system.dto;

import com.university.billing_system.entity.Bill;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BillResponse(
        Long id,
        String status,
        LocalDateTime createdAt,
        LocalDateTime finalizedAt,
        BigDecimal totalAmount,
        List<BillItemResponse> items) {

    public static BillResponse from(Bill bill) {
        return new BillResponse(
                bill.getId(),
                bill.getStatus().name(),
                bill.getCreatedAt(),
                bill.getFinalizedAt(),
                bill.getTotalAmount(),
                bill.getItems().stream().map(BillItemResponse::from).toList());
    }
}