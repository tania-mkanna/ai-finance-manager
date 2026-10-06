package com.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record UpdateReceiptRequest(
        LocalDate receiptDate,
        String merchantName,
        BigDecimal totalAmount,
        String currency,
        List<UpdateReceiptItemRequest> items
) {
}
