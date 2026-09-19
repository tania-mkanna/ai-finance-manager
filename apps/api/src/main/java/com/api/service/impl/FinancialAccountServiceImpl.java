package com.api.service.impl;

import com.api.dto.CreateFinancialAccountRequest;
import com.api.dto.FinancialAccountResponse;
import com.api.dto.UpdateFinancialAccountRequest;
import com.api.enums.FinancialAccountType;
import com.api.exception.ConflictException;
import com.api.exception.ForbiddenException;
import com.api.exception.InvalidRequestException;
import com.api.exception.NotFoundException;
import com.api.model.FinancialAccount;
import com.api.model.User;
import com.api.repository.FinancialAccountRepository;
import com.api.repository.TransactionRepository;
import com.api.repository.UserRepository;
import com.api.service.interfaces.FinancialAccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
@Service
public class FinancialAccountServiceImpl implements FinancialAccountService {

    private final FinancialAccountRepository financialAccountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public FinancialAccountServiceImpl(
            FinancialAccountRepository financialAccountRepository,
            UserRepository userRepository,
            TransactionRepository transactionRepository
    ) {
        this.financialAccountRepository = financialAccountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    private boolean isAccountNameUniqueViolation(Throwable throwable) {
        Throwable cause = throwable;

        while (cause != null) {
            String message = cause.getMessage();

            if (message != null
                    && message.contains(
                    "idx_financial_accounts_unique_active_user_name"
            )) {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FinancialAccountResponse> getAccounts(UUID currentUserId) {
        return financialAccountRepository.findByUser_IdOrderByNameAsc(currentUserId)
                .stream()
                .map(FinancialAccountResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FinancialAccountResponse getAccount(UUID currentUserId, UUID accountId) {
        FinancialAccount account = financialAccountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Financial account not found"));

        if (!account.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have access to this financial account");
        }

        return FinancialAccountResponse.from(account);
    }

    @Override
    @Transactional
    public FinancialAccountResponse createAccount(UUID currentUserId, CreateFinancialAccountRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Request body is required");
        }

        String normalizedName = normalizeName(request.name());
        if (normalizedName.isBlank()) {
            throw new InvalidRequestException("Financial account name is required");
        }

        if (request.type() == null) {
            throw new InvalidRequestException("Financial account type is required");
        }

        String normalizedCurrency = normalizeCurrency(request.currency());
        if (normalizedCurrency == null) {
            throw new InvalidRequestException("Currency must be a valid 3-character ISO code");
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (financialAccountRepository.existsByUser_IdAndNameIgnoreCase(currentUserId, normalizedName)) {
            throw new ConflictException("Financial account with this name already exists");
        }

        FinancialAccount account = new FinancialAccount();
        account.setUser(user);
        account.setName(normalizedName);
        account.setType(request.type());
        account.setCurrency(normalizedCurrency);

        try {
            FinancialAccount saved = financialAccountRepository.saveAndFlush(account);
            return FinancialAccountResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            if (isAccountNameUniqueViolation(ex)) {
                throw new ConflictException(
                        "Financial account with this name already exists"
                );
            }

            throw ex;
        }
    }

    @Override
    @Transactional
    public FinancialAccountResponse updateAccount(UUID currentUserId, UUID accountId, UpdateFinancialAccountRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Request body is required");
        }

        FinancialAccount account = financialAccountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Financial account not found"));

        if (!account.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have permission to update this financial account");
        }

        String resolvedName = account.getName();
        FinancialAccountType resolvedType = account.getType();
        String resolvedCurrency = account.getCurrency();

        if (request.name() != null) {
            resolvedName = normalizeName(request.name());
            if (resolvedName.isBlank()) {
                throw new InvalidRequestException("Financial account name is required");
            }
        }

        if (request.type() != null) {
            resolvedType = request.type();
        }

        if (request.currency() != null) {
            resolvedCurrency = normalizeCurrency(request.currency());
            if (resolvedCurrency == null) {
                throw new InvalidRequestException("Currency must be a valid 3-character ISO code");
            }
        }

        if (!account.getName().equalsIgnoreCase(resolvedName)
                || !account.getType().equals(resolvedType)
                || !equalsIgnoreCase(account.getCurrency(), resolvedCurrency)) {
            if (financialAccountRepository.existsByUser_IdAndNameIgnoreCaseAndIdNot(currentUserId, resolvedName, accountId)) {
                throw new ConflictException("Financial account with this name already exists");
            }
        }

        account.setName(resolvedName);
        account.setType(resolvedType);
        account.setCurrency(resolvedCurrency);

        try {
            FinancialAccount saved =
                    financialAccountRepository.saveAndFlush(account);

            return FinancialAccountResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            if (isAccountNameUniqueViolation(ex)) {
                throw new ConflictException(
                        "Financial account with this name already exists"
                );
            }

            throw ex;
        }
    }

    @Override
    @Transactional
    public void deleteAccount(UUID currentUserId, UUID accountId) {
        FinancialAccount account = financialAccountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Financial account not found"));

        if (!account.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have permission to delete this financial account");
        }

        boolean hasTransactions = transactionRepository.existsByAccountId(accountId);
        if (hasTransactions) {
            throw new ConflictException("Cannot delete financial account because it is referenced by one or more transactions");
        }

        financialAccountRepository.delete(account);
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }

    private String normalizeCurrency(String currency) {
        if (currency == null) {
            return null;
        }

        String normalized = currency.trim().toUpperCase(Locale.ROOT);

        try {
            java.util.Currency.getInstance(normalized);
            return normalized;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left == null ? right == null : left.equalsIgnoreCase(right);
    }
}
