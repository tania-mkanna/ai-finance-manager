package com.api.dto;

import com.api.enums.CategoryType;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @Size(min = 1, max = 255, message = "Category name must be between 1 and 255 characters")
        String name,

        CategoryType type,

        String icon,

        String color
) {
}
