package com.api.repository;

import com.api.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    boolean existsByCategoryId(UUID categoryId);

    boolean existsByAccountId(UUID accountId);
}