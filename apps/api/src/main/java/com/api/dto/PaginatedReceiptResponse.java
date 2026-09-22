package com.api.dto;

import java.util.List;

public record PaginatedReceiptResponse(
        List<ReceiptSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
