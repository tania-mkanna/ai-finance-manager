package com.api.service.impl;

import com.api.dto.CreateTransactionRequest;
import com.api.dto.PaginatedTransactionResponse;
import com.api.dto.TransactionResponse;
import com.api.dto.UpdateTransactionRequest;
import com.api.enums.CategoryType;
import com.api.enums.TransactionSource;
import com.api.enums.TransactionType;
import com.api.exception.ForbiddenException;
import com.api.exception.InvalidRequestException;
import com.api.exception.NotFoundException;
import com.api.model.Category;
import com.api.model.FinancialAccount;
import com.api.model.Transaction;
import com.api.model.User;
import com.api.repository.CategoryRepository;
import com.api.repository.FinancialAccountRepository;
import com.api.repository.TransactionRepository;
import com.api.repository.UserRepository;
import com.api.service.interfaces.TransactionService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final FinancialAccountRepository financialAccountRepository;
    private final UserRepository userRepository;

    public TransactionServiceImpl(
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            FinancialAccountRepository financialAccountRepository,
            UserRepository userRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.financialAccountRepository = financialAccountRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public TransactionResponse createTransaction(UUID currentUserId, CreateTransactionRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Request body is required");
        }

        validateOwnershipAndCategoryCompatibility(currentUserId, request.categoryId(), request.accountId(), request.type(), request.amount(), request.currency(), request.transactionDate());

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new NotFoundException("Category not found"));
        FinancialAccount account = financialAccountRepository.findById(request.accountId())
                .orElseThrow(() -> new NotFoundException("Financial account not found"));

        if (category.getUserId() != null && !category.getUserId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have access to this category");
        }
        if (!account.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have access to this financial account");
        }

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setCategory(category);
        transaction.setAccount(account);
        transaction.setType(request.type());
        transaction.setAmount(request.amount());
        transaction.setCurrency(normalizeCurrency(request.currency()));
        transaction.setTransactionDate(request.transactionDate());
        transaction.setMerchantName(request.merchantName() == null ? null : request.merchantName().trim());
        transaction.setDescription(request.description());
        transaction.setSource(TransactionSource.MANUAL);
        transaction.setReceipt(null);

        Transaction saved = transactionRepository.save(transaction);
        return TransactionResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedTransactionResponse getTransactions(
            UUID currentUserId,
            int page,
            int size,
            TransactionType type,
            UUID categoryId,
            UUID accountId,
            String merchant,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            LocalDateTime from,
            LocalDateTime to,
            TransactionSource source,
            String sort
    ) {
        if (page < 0) {
            throw new InvalidRequestException("Page must be greater than or equal to 0");
        }
        if (size <= 0 || size > 100) {
            throw new InvalidRequestException("Size must be between 1 and 100");
        }
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new InvalidRequestException("minAmount cannot be greater than maxAmount");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidRequestException("from date cannot be after to date");
        }

        Sort sortSpec = buildSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortSpec);

        Specification<Transaction> spec = buildSpecification(
                currentUserId, type, categoryId, accountId, merchant, minAmount, maxAmount, from, to, source
        );

        Page<Transaction> transactionPage = transactionRepository.findAll(spec, pageable);

        return new PaginatedTransactionResponse(
                transactionPage.getContent().stream().map(TransactionResponse::from).toList(),
                transactionPage.getNumber(),
                transactionPage.getSize(),
                transactionPage.getTotalElements(),
                transactionPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(UUID currentUserId, UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        if (!transaction.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have access to this transaction");
        }

        return TransactionResponse.from(transaction);
    }

    @Override
    @Transactional
    public TransactionResponse updateTransaction(UUID currentUserId, UUID transactionId, UpdateTransactionRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Request body is required");
        }

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        if (!transaction.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have permission to update this transaction");
        }

        Category targetCategory = transaction.getCategory();
        if (request.categoryId() != null) {
            targetCategory = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            if (targetCategory.getUserId() != null && !targetCategory.getUserId().equals(currentUserId)) {
                throw new ForbiddenException("You do not have access to this category");
            }
        }

        FinancialAccount targetAccount = transaction.getAccount();
        if (request.accountId() != null) {
            targetAccount = financialAccountRepository.findById(request.accountId())
                    .orElseThrow(() -> new NotFoundException("Financial account not found"));
            if (!targetAccount.getUser().getId().equals(currentUserId)) {
                throw new ForbiddenException("You do not have access to this financial account");
            }
        }

        TransactionType targetType = transaction.getType();
        if (request.type() != null) {
            targetType = request.type();
        }

        BigDecimal targetAmount = transaction.getAmount();
        if (request.amount() != null) {
            if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidRequestException("Amount must be greater than 0");
            }
            targetAmount = request.amount();
        }

        String targetCurrency = transaction.getCurrency();
        if (request.currency() != null) {
            String normalizedCurrency = normalizeCurrency(request.currency());
            if (normalizedCurrency == null) {
                throw new InvalidRequestException("Currency must be a valid 3-character ISO code");
            }
            targetCurrency = normalizedCurrency;
        }

        LocalDateTime targetTransactionDate = transaction.getTransactionDate();
        if (request.transactionDate() != null) {
            targetTransactionDate = request.transactionDate();
        }

        String targetMerchantName = transaction.getMerchantName();
        if (request.merchantName() != null) {
            targetMerchantName = request.merchantName().trim();
        }

        String targetDescription = transaction.getDescription();
        if (request.description() != null) {
            targetDescription = request.description();
        }

        validateCategoryAndAccountForTransaction(currentUserId, targetCategory, targetAccount, targetType);

        transaction.setCategory(targetCategory);
        transaction.setAccount(targetAccount);
        transaction.setType(targetType);
        transaction.setAmount(targetAmount);
        transaction.setCurrency(targetCurrency);
        transaction.setTransactionDate(targetTransactionDate);
        transaction.setMerchantName(targetMerchantName);
        transaction.setDescription(targetDescription);

        Transaction saved = transactionRepository.save(transaction);
        return TransactionResponse.from(saved);
    }

    @Override
    @Transactional
    public void deleteTransaction(UUID currentUserId, UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        if (!transaction.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have permission to delete this transaction");
        }

        transactionRepository.delete(transaction);
    }

    private Specification<Transaction> buildSpecification(
            UUID currentUserId,
            TransactionType type,
            UUID categoryId,
            UUID accountId,
            String merchant,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            LocalDateTime from,
            LocalDateTime to,
            TransactionSource source
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), currentUserId));

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (accountId != null) {
                predicates.add(cb.equal(root.get("account").get("id"), accountId));
            }
            if (merchant != null && !merchant.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("merchantName")), "%" + merchant.toLowerCase(Locale.ROOT).trim() + "%"));
            }
            if (minAmount != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), minAmount));
            }
            if (maxAmount != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), maxAmount));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), to));
            }
            if (source != null) {
                predicates.add(cb.equal(root.get("source"), source));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort buildSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "transactionDate");
        }

        String[] tokens = sort.split(",");
        String field = tokens[0].trim();
        Sort.Direction direction = tokens.length > 1 && tokens[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return switch (field) {
            case "transactionDate" -> Sort.by(direction, "transactionDate");
            case "amount" -> Sort.by(direction, "amount");
            case "merchantName" -> Sort.by(direction, "merchantName");
            case "createdAt" -> Sort.by(direction, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "transactionDate");
        };
    }

    private void validateOwnershipAndCategoryCompatibility(
            UUID currentUserId,
            UUID categoryId,
            UUID accountId,
            TransactionType type,
            BigDecimal amount,
            String currency,
            LocalDateTime transactionDate
    ) {
        if (categoryId == null) {
            throw new InvalidRequestException("Category ID is required");
        }
        if (accountId == null) {
            throw new InvalidRequestException("Account ID is required");
        }
        if (type == null) {
            throw new InvalidRequestException("Transaction type is required");
        }
        if (amount == null) {
            throw new InvalidRequestException("Amount is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("Amount must be greater than 0");
        }
        if (currency == null || currency.isBlank()) {
            throw new InvalidRequestException("Currency is required");
        }
        String normalizedCurrency = normalizeCurrency(currency);
        if (normalizedCurrency == null) {
            throw new InvalidRequestException("Currency must be a valid 3-character ISO code");
        }
        if (transactionDate == null) {
            throw new InvalidRequestException("Transaction date is required");
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        if (category.getUserId() != null && !category.getUserId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have access to this category");
        }

        FinancialAccount account = financialAccountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Financial account not found"));
        if (!account.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have access to this financial account");
        }

        validateCategoryAndAccountForTransaction(currentUserId, category, account, type);
    }

    private void validateCategoryAndAccountForTransaction(
            UUID currentUserId,
            Category category,
            FinancialAccount account,
            TransactionType type
    ) {
        if (category == null) {
            throw new NotFoundException("Category not found");
        }
        if (category.getUserId() != null && !category.getUserId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have access to this category");
        }
        if (account == null) {
            throw new NotFoundException("Financial account not found");
        }
        if (!account.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have access to this financial account");
        }

        if (type == TransactionType.EXPENSE && category.getType() != CategoryType.EXPENSE && category.getType() != CategoryType.BOTH) {
            throw new InvalidRequestException("Category type is not compatible with transaction type");
        }
        if (type == TransactionType.INCOME && category.getType() != CategoryType.INCOME && category.getType() != CategoryType.BOTH) {
            throw new InvalidRequestException("Category type is not compatible with transaction type");
        }
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
}
