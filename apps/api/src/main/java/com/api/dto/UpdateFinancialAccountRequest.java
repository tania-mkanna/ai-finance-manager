package com.api.dto;

import com.api.enums.FinancialAccountType;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateFinancialAccountRequest(
        @Size(min = 1, max = 255, message = "Financial account name must be between 1 and 255 characters")
        String name,

        FinancialAccountType type,

        @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be a valid 3-character ISO code")
        String currency
) {
}
