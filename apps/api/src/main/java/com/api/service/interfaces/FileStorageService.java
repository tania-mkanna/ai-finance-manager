package com.api.service.interfaces;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeFile(MultipartFile file);

    byte[] readFile(String storedPath);

    void deleteFile(String storedPath);

    boolean exists(String storedPath);
}
