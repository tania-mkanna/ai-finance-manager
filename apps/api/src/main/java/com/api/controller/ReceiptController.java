package com.api.controller;

import com.api.dto.PaginatedReceiptResponse;
import com.api.dto.ReceiptFileDownload;
import com.api.dto.ReceiptResponse;
import com.api.dto.ReceiptUploadResponse;
import com.api.dto.UpdateReceiptRequest;
import com.api.enums.ReceiptStatus;
import com.api.model.User;
import com.api.repository.UserRepository;
import com.api.service.interfaces.ReceiptService;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ReceiptController {

    private final ReceiptService receiptService;
    private final UserRepository userRepository;

    public ReceiptController(ReceiptService receiptService, UserRepository userRepository) {
        this.receiptService = receiptService;
        this.userRepository = userRepository;
    }

    @PostMapping(value = "/receipts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReceiptUploadResponse> uploadReceipt(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String currency
    ) {
        UUID currentUserId = resolveCurrentUserId(authentication);
        ReceiptUploadResponse response = receiptService.uploadReceipt(currentUserId, file, currency);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/receipts/{receiptId}")
    public ResponseEntity<ReceiptResponse> getReceipt(Authentication authentication, @PathVariable UUID receiptId) {
        UUID currentUserId = resolveCurrentUserId(authentication);
        return ResponseEntity.ok(receiptService.getReceipt(currentUserId, receiptId));
    }

    @GetMapping("/receipts")
    public ResponseEntity<PaginatedReceiptResponse> getReceipts(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ReceiptStatus status,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false, defaultValue = "uploadedAt,desc") String sort
    ) {
        UUID currentUserId = resolveCurrentUserId(authentication);
        return ResponseEntity.ok(receiptService.getReceipts(currentUserId, page, size, status, from, to, sort));
    }

    @GetMapping("/receipts/{receiptId}/file")
    public ResponseEntity<Resource> getReceiptFile(Authentication authentication, @PathVariable UUID receiptId) {
        UUID currentUserId = resolveCurrentUserId(authentication);
        ReceiptFileDownload file = receiptService.getReceiptFile(currentUserId, receiptId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(file.contentType()));
        headers.setContentDispositionFormData("attachment", file.fileName());
        headers.setContentLength(file.content().length);

        return ResponseEntity.ok().headers(headers).body(new ByteArrayResource(file.content()));
    }

    @PatchMapping("/receipts/{receiptId}")
    public ResponseEntity<ReceiptResponse> updateReceipt(
            Authentication authentication,
            @PathVariable UUID receiptId,
            @Valid @RequestBody UpdateReceiptRequest request
    ) {
        UUID currentUserId = resolveCurrentUserId(authentication);
        return ResponseEntity.ok(receiptService.updateReceipt(currentUserId, receiptId, request));
    }

    @DeleteMapping("/receipts/{receiptId}")
    public ResponseEntity<Void> deleteReceipt(Authentication authentication, @PathVariable UUID receiptId) {
        UUID currentUserId = resolveCurrentUserId(authentication);
        receiptService.deleteReceipt(currentUserId, receiptId);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveCurrentUserId(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new com.api.exception.NotFoundException("User not found"));
        return user.getId();
    }
}
