package com.api.dto;

public record ReceiptFileDownload(byte[] content, String contentType, String fileName) {
}
