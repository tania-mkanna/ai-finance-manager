package com.api.service.interfaces;

import com.api.dto.CreateFinancialAccountRequest;
import com.api.dto.FinancialAccountResponse;
import com.api.dto.UpdateFinancialAccountRequest;

import java.util.List;
import java.util.UUID;

public interface FinancialAccountService {
    List<FinancialAccountResponse> getAccounts(UUID currentUserId);

    FinancialAccountResponse getAccount(UUID currentUserId, UUID accountId);

    FinancialAccountResponse createAccount(UUID currentUserId, CreateFinancialAccountRequest request);

    FinancialAccountResponse updateAccount(UUID currentUserId, UUID accountId, UpdateFinancialAccountRequest request);

    void deleteAccount(UUID currentUserId, UUID accountId);
}
