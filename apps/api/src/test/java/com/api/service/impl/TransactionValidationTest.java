package com.api.service.impl;

import com.api.dto.CreateTransactionRequest;
import com.api.dto.UpdateTransactionRequest;
import com.api.enums.TransactionType;
import com.api.exception.InvalidRequestException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void createRequestRejectsAmountsAndMerchantNamesBeyondDatabaseLimits() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                TransactionType.EXPENSE,
                new BigDecimal("1000000000000000.0000"),
                "USD",
                LocalDateTime.now(),
                "a".repeat(256),
                null
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("amount", "merchantName");
    }

    @Test
    void updateRequestRejectsAmountsAndMerchantNamesBeyondDatabaseLimits() {
        UpdateTransactionRequest request = new UpdateTransactionRequest(
                null,
                null,
                null,
                new BigDecimal("1.00000"),
                null,
                null,
                "a".repeat(256),
                null
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("amount", "merchantName");
    }

    @Test
    void getTransactionsRejectsPageSizesAboveOneHundred() {
        TransactionServiceImpl service = new TransactionServiceImpl(null, null, null, null);

        assertThatThrownBy(() -> service.getTransactions(
                UUID.randomUUID(), 0, 101, null, null, null, null,
                null, null, null, null, null, null
        ))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Size must be between 1 and 100");
    }
}
