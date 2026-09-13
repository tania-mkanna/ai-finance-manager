package com.api.dto;

import com.api.enums.CategoryType;
import com.api.model.Category;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        CategoryType type,
        String icon,
        String color,
        boolean isSystem
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getIcon(),
                category.getColor(),
                category.getUserId() == null
        );
    }
}
