package com.api.repository;

import com.api.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {
    boolean existsByCategoryId(UUID categoryId);

    boolean existsByAccountId(UUID accountId);

    boolean existsByReceipt_Id(UUID receiptId);

    @EntityGraph(attributePaths = {"category", "account"})
    Optional<Transaction> findById(UUID transactionId);

    @EntityGraph(attributePaths = {"category", "account"})
    Page<Transaction> findAll(Specification<Transaction> spec, Pageable pageable);

    Optional<Transaction> findByIdAndUser_Id(UUID transactionId, UUID userId);
}