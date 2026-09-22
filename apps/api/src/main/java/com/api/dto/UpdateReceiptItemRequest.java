package com.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateReceiptItemRequest(
        UUID id,
        String name,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        UUID categoryId
) {
}
