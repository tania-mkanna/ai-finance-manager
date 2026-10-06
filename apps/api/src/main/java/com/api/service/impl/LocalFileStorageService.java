package com.api.service.impl;

import com.api.exception.FileStorageException;
import com.api.exception.InvalidRequestException;
import com.api.service.interfaces.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path baseDirectory;

    public LocalFileStorageService(@Value("${app.storage.base-directory:./storage}") String baseDirectory) {
        this.baseDirectory = Paths.get(baseDirectory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create storage directory", e);
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Receipt file cannot be empty");
        }

        String originalName = sanitizeOriginalFileName(file.getOriginalFilename());
        String extension = resolveExtension(file.getContentType(), originalName);
        String safeName = UUID.randomUUID() + "." + extension;
        String relativePath = "receipts/" + safeName;

        Path target = resolveStoragePath(relativePath);

        try {
            Files.createDirectories(target.getParent());
            Files.write(target, file.getBytes());
            return relativePath;
        } catch (IOException e) {
            throw new InvalidRequestException("Could not save receipt file");
        }
    }

    @Override
    public byte[] readFile(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            throw new InvalidRequestException("Receipt file not found");
        }

        Path target = resolveStoragePath(storedPath);
        if (!Files.exists(target)) {
            throw new InvalidRequestException("Receipt file not found");
        }

        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new InvalidRequestException("Receipt file could not be read");
        }
    }

    @Override
    public void deleteFile(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return;
        }

        Path target = resolveStoragePath(storedPath);

        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            throw new FileStorageException(
                    "Could not delete receipt file",
                    ex
            );
        }
    }

    @Override
    public boolean exists(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return false;
        }

        try {
            return Files.exists(resolveStoragePath(storedPath));
        } catch (InvalidRequestException ex) {
            return false;
        }
    }

    private Path resolveStoragePath(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            throw new InvalidRequestException("Receipt file not found");
        }

        Path normalized = Paths.get(storedPath).normalize();
        Path resolved = baseDirectory.resolve(normalized).normalize();
        if (!resolved.startsWith(baseDirectory)) {
            throw new InvalidRequestException("Invalid receipt storage path");
        }

        return resolved;
    }

    private String sanitizeOriginalFileName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "receipt";
        }

        String sanitized = Paths.get(originalName).getFileName().toString();
        sanitized = sanitized.replaceAll("[\\\\/:*?\"<>|]", "_");
        return sanitized.isBlank() ? "receipt" : sanitized;
    }

    private String resolveExtension(String contentType, String originalName) {
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT).trim();

        if ("image/jpeg".equals(type) || "image/jpg".equals(type)) {
            return "jpg";
        }
        if ("image/png".equals(type)) {
            return "png";
        }
        if ("image/webp".equals(type)) {
            return "webp";
        }

        String lowerName = originalName == null ? "" : originalName.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "jpg";
        }
        if (lowerName.endsWith(".png")) {
            return "png";
        }
        if (lowerName.endsWith(".webp")) {
            return "webp";
        }

        throw new InvalidRequestException("Unsupported receipt file type");
    }
}
