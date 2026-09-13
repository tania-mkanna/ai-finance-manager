package com.api.dto;

import com.api.enums.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank(message = "Category name is required")
        @Size(min = 1, max = 255, message = "Category name must be between 1 and 255 characters")
        String name,

        @NotNull(message = "Category type is required")
        CategoryType type,

        String icon,

        String color
) {
}
