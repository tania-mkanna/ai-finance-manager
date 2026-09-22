package com.api.service.interfaces;

import com.api.dto.PaginatedReceiptResponse;
import com.api.dto.ReceiptFileDownload;
import com.api.dto.ReceiptResponse;
import com.api.dto.ReceiptUploadResponse;
import com.api.dto.UpdateReceiptRequest;
import com.api.enums.ReceiptStatus;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

public interface ReceiptService {
    ReceiptUploadResponse uploadReceipt(UUID currentUserId, MultipartFile file, String currency);

    ReceiptResponse getReceipt(UUID currentUserId, UUID receiptId);

    PaginatedReceiptResponse getReceipts(
            UUID currentUserId,
            int page,
            int size,
            ReceiptStatus status,
            LocalDate from,
            LocalDate to,
            String sort
    );

    ReceiptResponse updateReceipt(UUID currentUserId, UUID receiptId, UpdateReceiptRequest request);

    void deleteReceipt(UUID currentUserId, UUID receiptId);

    ReceiptFileDownload getReceiptFile(UUID currentUserId, UUID receiptId);
}
