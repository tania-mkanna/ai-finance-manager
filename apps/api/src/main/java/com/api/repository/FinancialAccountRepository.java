package com.api.repository;

import com.api.model.FinancialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinancialAccountRepository extends JpaRepository<FinancialAccount, UUID> {
    List<FinancialAccount> findByUser_IdOrderByNameAsc(UUID userId);

    Optional<FinancialAccount> findByIdAndUser_Id(UUID accountId, UUID userId);

    boolean existsByUser_IdAndNameIgnoreCase(UUID userId, String name);

    boolean existsByUser_IdAndNameIgnoreCaseAndIdNot(UUID userId, String name, UUID accountId);
}