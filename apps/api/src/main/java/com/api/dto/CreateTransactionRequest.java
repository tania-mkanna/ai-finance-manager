package com.api.dto;

import com.api.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateTransactionRequest(
        @NotNull(message = "Category ID is required")
        UUID categoryId,

        @NotNull(message = "Account ID is required")
        UUID accountId,

        @NotNull(message = "Transaction type is required")
        TransactionType type,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than 0")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be a valid 3-character ISO code")
        String currency,

        @NotNull(message = "Transaction date is required")
        LocalDateTime transactionDate,

        String merchantName,

        String description
) {
}
