package com.api.dto;

import com.api.model.FinancialAccount;

import java.util.UUID;

public record AccountSummaryResponse(
        UUID id,
        String name
) {
    public static AccountSummaryResponse from(FinancialAccount account) {
        return new AccountSummaryResponse(
                account.getId(),
                account.getName()
        );
    }
}
