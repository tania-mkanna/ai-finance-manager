package com.api.dto;

import com.api.enums.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCategoryRequest(
        @NotBlank(message = "Category name is required")
        String name,

        @NotNull(message = "Category type is required")
        CategoryType type,

        String icon,

        String color
) {
}
