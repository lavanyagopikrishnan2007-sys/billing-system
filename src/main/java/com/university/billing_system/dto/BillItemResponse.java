package com.university.billing_system.dto;

import com.university.billing_system.entity.BillItem;
import java.math.BigDecimal;

public record BillItemResponse(
        Long productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal) {

    public static BillItemResponse from(BillItem item) {
        return new BillItemResponse(
                item.getProduct().getProductId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal());
    }
}