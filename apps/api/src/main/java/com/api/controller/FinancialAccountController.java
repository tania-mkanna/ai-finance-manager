package com.api.controller;

import com.api.dto.CreateFinancialAccountRequest;
import com.api.dto.FinancialAccountResponse;
import com.api.dto.UpdateFinancialAccountRequest;
import com.api.model.User;
import com.api.repository.UserRepository;
import com.api.service.interfaces.FinancialAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class FinancialAccountController {

    private final FinancialAccountService financialAccountService;
    private final UserRepository userRepository;

    public FinancialAccountController(FinancialAccountService financialAccountService, UserRepository userRepository) {
        this.financialAccountService = financialAccountService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<FinancialAccountResponse>> getAccounts(Authentication authentication) {
        UUID currentUserId = resolveCurrentUserId(authentication);
        return ResponseEntity.ok(financialAccountService.getAccounts(currentUserId));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<FinancialAccountResponse> getAccount(Authentication authentication, @PathVariable UUID accountId) {
        UUID currentUserId = resolveCurrentUserId(authentication);
        return ResponseEntity.ok(financialAccountService.getAccount(currentUserId, accountId));
    }

    @PostMapping
    public ResponseEntity<FinancialAccountResponse> createAccount(
            Authentication authentication,
            @Valid @RequestBody CreateFinancialAccountRequest request
    ) {
        UUID currentUserId = resolveCurrentUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(financialAccountService.createAccount(currentUserId, request));
    }

    @PatchMapping("/{accountId}")
    public ResponseEntity<FinancialAccountResponse> updateAccount(
            Authentication authentication,
            @PathVariable UUID accountId,
            @Valid @RequestBody UpdateFinancialAccountRequest request
    ) {
        UUID currentUserId = resolveCurrentUserId(authentication);
        return ResponseEntity.ok(financialAccountService.updateAccount(currentUserId, accountId, request));
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> deleteAccount(Authentication authentication, @PathVariable UUID accountId) {
        UUID currentUserId = resolveCurrentUserId(authentication);
        financialAccountService.deleteAccount(currentUserId, accountId);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveCurrentUserId(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new com.api.exception.NotFoundException("User not found"));
        return user.getId();
    }
}
