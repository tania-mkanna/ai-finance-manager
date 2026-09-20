package com.api.dto;

import com.api.enums.TransactionType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateTransactionRequest(
        UUID categoryId,

        UUID accountId,

        TransactionType type,

        @Positive(message = "Amount must be greater than 0")
        @Digits(integer = 15, fraction = 4, message = "Amount must have up to 15 integer digits and 4 decimal places")
        BigDecimal amount,

        @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be a valid 3-character ISO code")
        String currency,

        LocalDateTime transactionDate,

        @Size(max = 255, message = "Merchant name must not exceed 255 characters")
        String merchantName,

        String description
) {
}
