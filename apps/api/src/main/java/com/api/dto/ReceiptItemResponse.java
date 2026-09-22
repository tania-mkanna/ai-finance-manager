package com.api.dto;

import com.api.model.ReceiptItem;

import java.math.BigDecimal;
import java.util.UUID;

public record ReceiptItemResponse(
        UUID id,
        String name,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        CategorySummaryResponse category,
        BigDecimal confidenceScore
) {
    public static ReceiptItemResponse from(ReceiptItem item) {
        return new ReceiptItemResponse(
                item.getId(),
                item.getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice(),
                item.getCategory() == null ? null : CategorySummaryResponse.from(item.getCategory()),
                item.getConfidenceScore()
        );
    }
}
