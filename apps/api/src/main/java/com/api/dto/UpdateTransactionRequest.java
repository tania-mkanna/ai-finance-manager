package com.api.dto;

import com.api.enums.TransactionType;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateTransactionRequest(
        UUID categoryId,

        UUID accountId,

        TransactionType type,

        @Positive(message = "Amount must be greater than 0")
        BigDecimal amount,

        @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be a valid 3-character ISO code")
        String currency,

        LocalDateTime transactionDate,

        String merchantName,

        String description
) {
}
