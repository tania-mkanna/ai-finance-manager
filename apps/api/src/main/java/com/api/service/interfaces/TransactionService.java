package com.api.service.interfaces;

import com.api.dto.CreateTransactionRequest;
import com.api.dto.PaginatedTransactionResponse;
import com.api.dto.TransactionResponse;
import com.api.dto.UpdateTransactionRequest;
import com.api.enums.TransactionSource;
import com.api.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public interface TransactionService {
    TransactionResponse createTransaction(UUID currentUserId, CreateTransactionRequest request);

    PaginatedTransactionResponse getTransactions(
            UUID currentUserId,
            int page,
            int size,
            TransactionType type,
            UUID categoryId,
            UUID accountId,
            String merchant,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            LocalDateTime from,
            LocalDateTime to,
            TransactionSource source,
            String sort
    );

    TransactionResponse getTransaction(UUID currentUserId, UUID transactionId);

    TransactionResponse updateTransaction(UUID currentUserId, UUID transactionId, UpdateTransactionRequest request);

    void deleteTransaction(UUID currentUserId, UUID transactionId);
}
