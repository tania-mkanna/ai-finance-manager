package com.api.dto;

import com.api.enums.ReceiptStatus;
import com.api.model.Receipt;
import com.api.model.ReceiptItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReceiptResponse(
        UUID id,
        String fileName,
        String mimeType,
        ReceiptStatus status,
        LocalDate receiptDate,
        String merchantName,
        BigDecimal totalAmount,
        String currency,
        LocalDateTime uploadedAt,
        LocalDateTime processedAt,
        String errorMessage,
        List<ReceiptItemResponse> items
) {
    public static ReceiptResponse from(Receipt receipt) {
        return from(receipt, receipt.getItems() == null ? List.of() : receipt.getItems());
    }

    public static ReceiptResponse from(Receipt receipt, List<ReceiptItem> items) {
        return new ReceiptResponse(
                receipt.getId(),
                receipt.getFileName(),
                receipt.getMimeType(),
                receipt.getStatus(),
                receipt.getReceiptDate(),
                receipt.getMerchantName(),
                receipt.getTotalAmount(),
                receipt.getCurrency(),
                receipt.getUploadedAt(),
                receipt.getProcessedAt(),
                receipt.getErrorMessage(),
                items == null ? List.of() : items.stream().map(ReceiptItemResponse::from).toList()
        );
    }
}
