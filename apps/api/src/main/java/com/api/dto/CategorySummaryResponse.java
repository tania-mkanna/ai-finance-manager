package com.api.dto;

import com.api.model.Category;

import java.util.UUID;

public record CategorySummaryResponse(
        UUID id,
        String name
) {
    public static CategorySummaryResponse from(Category category) {
        return new CategorySummaryResponse(
                category.getId(),
                category.getName()
        );
    }
}
