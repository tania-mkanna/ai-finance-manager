package com.api.dto;

import java.util.List;

public record PaginatedTransactionResponse(
        List<TransactionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
