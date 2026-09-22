package com.api.service.impl;

import com.api.dto.*;
import com.api.enums.CategoryType;
import com.api.enums.ReceiptStatus;
import com.api.exception.ConflictException;
import com.api.exception.ForbiddenException;
import com.api.exception.InvalidRequestException;
import com.api.exception.NotFoundException;
import com.api.model.Category;
import com.api.model.Receipt;
import com.api.model.ReceiptItem;
import com.api.model.User;
import com.api.repository.CategoryRepository;
import com.api.repository.ReceiptItemRepository;
import com.api.repository.ReceiptRepository;
import com.api.repository.TransactionRepository;
import com.api.repository.UserRepository;
import com.api.service.interfaces.FileStorageService;
import com.api.service.interfaces.ReceiptService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class ReceiptServiceImpl implements ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final long maxFileSizeBytes;

    public ReceiptServiceImpl(
            ReceiptRepository receiptRepository,
            ReceiptItemRepository receiptItemRepository,
            CategoryRepository categoryRepository,
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService,
            @Value("${app.storage.max-file-size-bytes:5242880}") long maxFileSizeBytes
    ) {
        this.receiptRepository = receiptRepository;
        this.receiptItemRepository = receiptItemRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @Override
    @Transactional
    public ReceiptUploadResponse uploadReceipt(UUID currentUserId, MultipartFile file, String currency) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Receipt file cannot be empty");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new InvalidRequestException("Receipt file exceeds the allowed size limit");
        }

        validateImageContent(file);

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        String normalizedCurrency = normalizeCurrency(currency);

        Receipt receipt = new Receipt();
        receipt.setUser(user);
        String storagePath = fileStorageService.storeFile(file);
        receipt.setFileUrl(storagePath);
        receipt.setFileName(safeDisplayFileName(file.getOriginalFilename()));
        receipt.setMimeType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
        receipt.setStatus(ReceiptStatus.UPLOADED);
        receipt.setCurrency(normalizedCurrency);
        receipt.setProcessedAt(null);
        receipt.setErrorMessage(null);
        receipt.setReceiptDate(null);
        receipt.setMerchantName(null);
        receipt.setTotalAmount(null);

        try {
            Receipt saved = receiptRepository.save(receipt);
            return ReceiptUploadResponse.from(saved);
        } catch (RuntimeException ex) {
            fileStorageService.deleteFile(storagePath);
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptResponse getReceipt(UUID currentUserId, UUID receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new NotFoundException("Receipt not found"));

        if (!receipt.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have access to this receipt");
        }

        List<ReceiptItem> items = receiptItemRepository.findByReceipt_Id(receiptId);
        return ReceiptResponse.from(receipt, items);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedReceiptResponse getReceipts(
            UUID currentUserId,
            int page,
            int size,
            ReceiptStatus status,
            LocalDate from,
            LocalDate to,
            String sort
    ) {
        if (page < 0) {
            throw new InvalidRequestException("Page must be greater than or equal to 0");
        }
        if (size <= 0 || size > 100) {
            throw new InvalidRequestException("Size must be between 1 and 100");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidRequestException("from date cannot be after to date");
        }

        Specification<Receipt> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), currentUserId));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("receiptDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("receiptDate"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sortSpec = buildSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortSpec);
        Page<Receipt> receiptPage = receiptRepository.findAll(spec, pageable);

        return new PaginatedReceiptResponse(
                receiptPage.getContent().stream().map(ReceiptSummaryResponse::from).toList(),
                receiptPage.getNumber(),
                receiptPage.getSize(),
                receiptPage.getTotalElements(),
                receiptPage.getTotalPages()
        );
    }

    @Override
    @Transactional
    public ReceiptResponse updateReceipt(UUID currentUserId, UUID receiptId, UpdateReceiptRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Request body is required");
        }

        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new NotFoundException("Receipt not found"));

        if (!receipt.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have permission to update this receipt");
        }

        if (receipt.getStatus() != ReceiptStatus.REVIEW_REQUIRED) {
            throw new ConflictException("Receipt can only be updated when status is REVIEW_REQUIRED");
        }

        if (request.receiptDate() != null) {
            receipt.setReceiptDate(request.receiptDate());
        }
        if (request.merchantName() != null) {
            receipt.setMerchantName(request.merchantName().trim());
        }
        if (request.totalAmount() != null) {
            if (request.totalAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidRequestException("Total amount must be greater than 0");
            }
            receipt.setTotalAmount(request.totalAmount());
        }
        if (request.currency() != null) {
            String normalizedCurrency = normalizeCurrency(request.currency());
            if (normalizedCurrency == null) {
                throw new InvalidRequestException("Currency must be a valid 3-character ISO code");
            }
            receipt.setCurrency(normalizedCurrency);
        }

        if (request.items() != null) {
            syncItems(receipt, request.items(), currentUserId);
        }

        Receipt saved = receiptRepository.save(receipt);
        return ReceiptResponse.from(saved);
    }

    @Override
    @Transactional
    public void deleteReceipt(UUID currentUserId, UUID receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new NotFoundException("Receipt not found"));

        if (!receipt.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have permission to delete this receipt");
        }

        if (receipt.getStatus() == ReceiptStatus.CONFIRMED) {
            throw new ConflictException("Confirmed receipts cannot be deleted");
        }

        if (transactionRepository.existsByReceipt_Id(receiptId)) {
            throw new ConflictException("Receipt is referenced by one or more transactions");
        }

        receiptItemRepository.deleteByReceiptId(receiptId);
        receiptRepository.delete(receipt);
        fileStorageService.deleteFile(receipt.getFileUrl());
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptFileDownload getReceiptFile(UUID currentUserId, UUID receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new NotFoundException("Receipt not found"));

        if (!receipt.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have access to this receipt");
        }

        if (!fileStorageService.exists(receipt.getFileUrl())) {
            throw new NotFoundException("Receipt file not found");
        }

        String mimeType = receipt.getMimeType() == null ? "application/octet-stream" : receipt.getMimeType();
        byte[] content = fileStorageService.readFile(receipt.getFileUrl());
        String safeFileName = safeDisplayFileName(receipt.getFileName());
        return new ReceiptFileDownload(content, mimeType, safeFileName);
    }

    private void syncItems(Receipt receipt, List<UpdateReceiptItemRequest> incomingItems, UUID currentUserId) {
        if (incomingItems == null) {
            return;
        }

        List<ReceiptItem> currentItems = new ArrayList<>(receipt.getItems());
        List<UUID> currentIds = currentItems.stream().map(ReceiptItem::getId).filter(Objects::nonNull).toList();

        for (UpdateReceiptItemRequest itemRequest : incomingItems) {
            if (itemRequest == null) {
                throw new InvalidRequestException("Receipt item is required");
            }

            if (itemRequest.id() != null) {
                if (!currentIds.contains(itemRequest.id())) {
                    ReceiptItem existingItem = receiptItemRepository.findById(itemRequest.id()).orElse(null);
                    if (existingItem == null) {
                        throw new InvalidRequestException("Receipt item not found");
                    }
                    if (!existingItem.getReceipt().getId().equals(receipt.getId())) {
                        throw new InvalidRequestException("Receipt item does not belong to this receipt");
                    }
                }
            }
        }

        for (ReceiptItem item : new ArrayList<>(currentItems)) {
            boolean stillPresent = incomingItems.stream().anyMatch(req -> req != null && Objects.equals(req.id(), item.getId()));
            if (!stillPresent) {
                receipt.getItems().remove(item);
                receiptItemRepository.delete(item);
            }
        }

        for (UpdateReceiptItemRequest itemRequest : incomingItems) {
            if (itemRequest == null) {
                continue;
            }

            ReceiptItem item;
            if (itemRequest.id() != null) {
                item = receiptItemRepository.findByIdAndReceipt_Id(itemRequest.id(), receipt.getId())
                        .orElseThrow(() -> new InvalidRequestException("Receipt item not found"));
            } else {
                item = new ReceiptItem();
                item.setReceipt(receipt);
            }

            if (itemRequest.name() != null && itemRequest.name().isBlank()) {
                throw new InvalidRequestException("Receipt item name is required");
            }
            if (itemRequest.name() != null) {
                item.setName(itemRequest.name().trim());
            }
            if (itemRequest.quantity() != null) {
                if (itemRequest.quantity().compareTo(BigDecimal.ZERO) < 0) {
                    throw new InvalidRequestException("Receipt item quantity cannot be negative");
                }
                item.setQuantity(itemRequest.quantity());
            }
            if (itemRequest.unitPrice() != null) {
                if (itemRequest.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
                    throw new InvalidRequestException("Receipt item unit price cannot be negative");
                }
                item.setUnitPrice(itemRequest.unitPrice());
            }
            if (itemRequest.totalPrice() != null) {
                if (itemRequest.totalPrice().compareTo(BigDecimal.ZERO) < 0) {
                    throw new InvalidRequestException("Receipt item total price cannot be negative");
                }
                item.setTotalPrice(itemRequest.totalPrice());
            }

            if (itemRequest.categoryId() != null) {
                Category category = categoryRepository.findById(itemRequest.categoryId())
                        .orElseThrow(() -> new NotFoundException("Category not found"));
                if (category.getUserId() != null && !category.getUserId().equals(currentUserId)) {
                    throw new ForbiddenException("You do not have access to this category");
                }
                item.setCategory(category);
            }

            if (item.getId() == null) {
                receipt.getItems().add(item);
            } else if (!receipt.getItems().contains(item)) {
                receipt.getItems().add(item);
            }
        }
    }

    private Sort buildSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "uploadedAt");
        }

        String[] parts = sort.split(",");
        String property = parts[0].trim();
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        if (property.equals("uploadedAt") || property.equals("receiptDate") || property.equals("merchantName") || property.equals("totalAmount")) {
            return Sort.by(direction, property);
        }

        return Sort.by(Sort.Direction.DESC, "uploadedAt");
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return null;
        }

        String normalized = currency.trim().toUpperCase(Locale.ROOT);
        try {
            java.util.Currency.getInstance(normalized);
            return normalized;
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Currency must be a valid 3-character ISO code");
        }
    }

    private String safeDisplayFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "receipt";
        }

        String safe = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        int slash = safe.lastIndexOf('/') != -1 ? safe.lastIndexOf('/') : safe.lastIndexOf('\\');
        return slash >= 0 ? safe.substring(slash + 1) : safe;
    }

    private void validateImageContent(MultipartFile file) {
        if (file.getContentType() == null) {
            throw new InvalidRequestException("Unsupported receipt file type");
        }

        String contentType = file.getContentType().toLowerCase(Locale.ROOT);
        if (!("image/jpeg".equals(contentType) || "image/png".equals(contentType) || "image/webp".equals(contentType))) {
            throw new InvalidRequestException("Unsupported receipt file type");
        }

        try {
            byte[] data = file.getBytes();
            if (data.length < 4) {
                throw new InvalidRequestException("Unsupported receipt file type");
            }
            if ("image/jpeg".equals(contentType) && !(data[0] == (byte) 0xFF && data[1] == (byte) 0xD8)) {
                throw new InvalidRequestException("Unsupported receipt file type");
            }
            if ("image/png".equals(contentType) && !(data[0] == (byte) 0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47)) {
                throw new InvalidRequestException("Unsupported receipt file type");
            }
            if ("image/webp".equals(contentType) && !(data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F')) {
                throw new InvalidRequestException("Unsupported receipt file type");
            }
        } catch (Exception ex) {
            throw new InvalidRequestException("Unsupported receipt file type");
        }
    }
}
