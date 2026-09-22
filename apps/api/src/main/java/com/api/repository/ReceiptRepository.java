package com.api.repository;

import com.api.model.Receipt;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ReceiptRepository extends JpaRepository<Receipt, UUID>, JpaSpecificationExecutor<Receipt> {
    @EntityGraph(attributePaths = {"items", "items.category"})
    Optional<Receipt> findById(UUID receiptId);

    Optional<Receipt> findByIdAndUser_Id(UUID receiptId, UUID userId);
}