package com.api.dto;

import com.api.enums.FinancialAccountType;
import com.api.model.FinancialAccount;

import java.util.UUID;

public record FinancialAccountResponse(
        UUID id,
        String name,
        FinancialAccountType type,
        String currency
) {
    public static FinancialAccountResponse from(FinancialAccount account) {
        return new FinancialAccountResponse(
                account.getId(),
                account.getName(),
                account.getType(),
                account.getCurrency()
        );
    }
}
