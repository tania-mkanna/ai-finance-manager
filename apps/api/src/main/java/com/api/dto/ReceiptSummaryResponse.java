package com.api.dto;

import com.api.enums.ReceiptStatus;
import com.api.model.Receipt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReceiptSummaryResponse(
        UUID id,
        String fileName,
        ReceiptStatus status,
        String merchantName,
        BigDecimal totalAmount,
        String currency,
        LocalDate receiptDate,
        LocalDateTime uploadedAt
) {
    public static ReceiptSummaryResponse from(Receipt receipt) {
        return new ReceiptSummaryResponse(
                receipt.getId(),
                receipt.getFileName(),
                receipt.getStatus(),
                receipt.getMerchantName(),
                receipt.getTotalAmount(),
                receipt.getCurrency(),
                receipt.getReceiptDate(),
                receipt.getUploadedAt()
        );
    }
}
