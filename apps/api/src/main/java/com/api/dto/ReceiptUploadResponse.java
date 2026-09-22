package com.api.dto;

import com.api.enums.ReceiptStatus;
import com.api.model.Receipt;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReceiptUploadResponse(
        UUID id,
        String fileName,
        String mimeType,
        ReceiptStatus status,
        String currency,
        LocalDateTime uploadedAt
) {
    public static ReceiptUploadResponse from(Receipt receipt) {
        return new ReceiptUploadResponse(
                receipt.getId(),
                receipt.getFileName(),
                receipt.getMimeType(),
                receipt.getStatus(),
                receipt.getCurrency(),
                receipt.getUploadedAt()
        );
    }
}
