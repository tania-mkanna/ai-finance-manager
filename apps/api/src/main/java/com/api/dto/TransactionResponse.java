package com.api.dto;

import com.api.enums.TransactionSource;
import com.api.enums.TransactionType;
import com.api.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        CategorySummaryResponse category,
        AccountSummaryResponse account,
        TransactionType type,
        BigDecimal amount,
        String currency,
        LocalDateTime transactionDate,
        String merchantName,
        String description,
        TransactionSource source,
        UUID receiptId,
        LocalDateTime createdAt
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                CategorySummaryResponse.from(transaction.getCategory()),
                AccountSummaryResponse.from(transaction.getAccount()),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getTransactionDate(),
                transaction.getMerchantName(),
                transaction.getDescription(),
                transaction.getSource(),
                transaction.getReceipt() == null ? null : transaction.getReceipt().getId(),
                transaction.getCreatedAt()
        );
    }
}
