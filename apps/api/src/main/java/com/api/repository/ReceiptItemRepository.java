package com.api.repository;

import com.api.model.ReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReceiptItemRepository extends JpaRepository<ReceiptItem, UUID> {
    List<ReceiptItem> findByReceipt_Id(UUID receiptId);

    Optional<ReceiptItem> findByIdAndReceipt_Id(UUID itemId, UUID receiptId);

    @Modifying
    @Query("DELETE FROM ReceiptItem ri WHERE ri.receipt.id = :receiptId")
    void deleteByReceiptId(@Param("receiptId") UUID receiptId);
}