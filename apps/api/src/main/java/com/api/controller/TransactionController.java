package com.api.controller;

import com.api.dto.CreateTransactionRequest;
import com.api.dto.PaginatedTransactionResponse;
import com.api.dto.TransactionResponse;
import com.api.dto.UpdateTransactionRequest;
import com.api.enums.TransactionSource;
import com.api.enums.TransactionType;
import com.api.model.User;
import com.api.repository.UserRepository;
import com.api.service.interfaces.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserRepository userRepository;

    public TransactionController(TransactionService transactionService, UserRepository userRepository) {
        this.transactionService = transactionService;
        this.userRepository = userRepository;
    }

    @PostMapping("/transactions")
    public ResponseEntity<TransactionResponse> createTransaction(
            Authentication authentication,
            @Valid @RequestBody CreateTransactionRequest request
    ) {
        UUID currentUserId = resolveCurrentUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.createTransaction(currentUserId, request));
    }

    @GetMapping("/transactions")
    public ResponseEntity<PaginatedTransactionResponse> getTransactions(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) String merchant,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            @RequestParam(required = false) TransactionSource source,
            @RequestParam(required = false, defaultValue = "transactionDate,desc") String sort
    ) {
        UUID currentUserId = resolveCurrentUserId(authentication);
        return ResponseEntity.ok(transactionService.getTransactions(
                currentUserId,
                page,
                size,
                type,
                categoryId,
                accountId,
                merchant,
                minAmount,
                maxAmount,
                from,
                to,
                source,
                sort
        ));
    }

    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(
            Authentication authentication,
            @PathVariable UUID transactionId
    ) {
        UUID currentUserId = resolveCurrentUserId(authentication);
        return ResponseEntity.ok(transactionService.getTransaction(currentUserId, transactionId));
    }

    @PatchMapping("/transactions/{transactionId}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            Authentication authentication,
            @PathVariable UUID transactionId,
            @Valid @RequestBody UpdateTransactionRequest request
    ) {
        UUID currentUserId = resolveCurrentUserId(authentication);
        return ResponseEntity.ok(transactionService.updateTransaction(currentUserId, transactionId, request));
    }

    @DeleteMapping("/transactions/{transactionId}")
    public ResponseEntity<Void> deleteTransaction(
            Authentication authentication,
            @PathVariable UUID transactionId
    ) {
        UUID currentUserId = resolveCurrentUserId(authentication);
        transactionService.deleteTransaction(currentUserId, transactionId);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveCurrentUserId(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new com.api.exception.NotFoundException("User not found"));
        return user.getId();
    }
}
