package com.api.service.interfaces;

import com.api.dto.CategoryResponse;
import com.api.dto.CreateCategoryRequest;
import com.api.dto.UpdateCategoryRequest;
import com.api.enums.CategoryType;

import java.util.List;
import java.util.UUID;

public interface CategoryService {
    List<CategoryResponse> getCategories(UUID currentUserId, CategoryType type);
    CategoryResponse getCategory(UUID currentUserId, UUID categoryId);
    CategoryResponse createCategory(UUID currentUserId, CreateCategoryRequest request);
    CategoryResponse updateCategory(UUID currentUserId, UUID categoryId, UpdateCategoryRequest request);
    void deleteCategory(UUID currentUserId, UUID categoryId);
}
