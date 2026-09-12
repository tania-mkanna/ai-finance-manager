package com.api.service.impl;

import com.api.dto.CategoryResponse;
import com.api.dto.CreateCategoryRequest;
import com.api.dto.UpdateCategoryRequest;
import com.api.enums.CategoryType;
import com.api.exception.ConflictException;
import com.api.exception.ForbiddenException;
import com.api.exception.InvalidRequestException;
import com.api.exception.NotFoundException;
import com.api.model.Category;
import com.api.model.Transaction;
import com.api.model.User;
import com.api.repository.CategoryRepository;
import com.api.repository.TransactionRepository;
import com.api.repository.UserRepository;
import com.api.service.interfaces.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public CategoryServiceImpl(
            CategoryRepository categoryRepository,
            TransactionRepository transactionRepository,
            UserRepository userRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(
            UUID currentUserId,
            CategoryType type
    ) {
        List<Category> categories;

        if (type == null) {
            categories = categoryRepository.findVisibleCategories(currentUserId);
        } else {
            categories = categoryRepository.findVisibleCategoriesByType(
                    currentUserId,
                    type,
                    CategoryType.BOTH
            );
        }

        return categories.stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategory(UUID currentUserId, UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));

        if (category.getUserId() != null && !category.getUserId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have access to this category");
        }

        return CategoryResponse.from(category);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(UUID currentUserId, CreateCategoryRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Request body is required");
        }

        String trimmedName = normalizeName(request.name());
        if (trimmedName.isBlank()) {
            throw new InvalidRequestException("Category name is required");
        }

        if (request.type() == null) {
            throw new InvalidRequestException("Category type is required");
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        boolean exists = categoryRepository.existsByUserIdAndNameIgnoreCaseAndType(currentUserId, trimmedName, request.type());
        if (exists) {
            throw new ConflictException("Category with this name and type already exists");
        }

        Category category = new Category();
        category.setUserId(currentUserId);
        category.setName(trimmedName);
        category.setType(request.type());
        category.setIcon(request.icon());
        category.setColor(request.color());

        Category saved = categoryRepository.save(category);
        return CategoryResponse.from(saved);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(UUID currentUserId, UUID categoryId, UpdateCategoryRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Request body is required");
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));

        if (category.getUserId() == null) {
            throw new ForbiddenException("System categories cannot be modified");
        }

        if (!category.getUserId().equals(currentUserId)) {
            throw new ForbiddenException(
                    "You do not have permission to update this category"
            );
        }

        String resolvedName = category.getName();
        CategoryType resolvedType = category.getType();

        if (request.name() != null) {
            resolvedName = normalizeName(request.name());

            if (resolvedName.isBlank()) {
                throw new InvalidRequestException("Category name is required");
            }
        }

        if (request.type() != null) {
            resolvedType = request.type();
        }

        boolean duplicateExists =
                categoryRepository.existsByUserIdAndNameIgnoreCaseAndTypeAndIdNot(
                        currentUserId,
                        resolvedName,
                        resolvedType,
                        categoryId
                );

        if (duplicateExists) {
            throw new ConflictException(
                    "Category with this name and type already exists"
            );
        }

        category.setName(resolvedName);
        category.setType(resolvedType);

        if (request.icon() != null) {
            category.setIcon(request.icon());
        }

        if (request.color() != null) {
            category.setColor(request.color());
        }

        Category saved = categoryRepository.save(category);

        return CategoryResponse.from(saved);
    }

    @Override
    @Transactional
    public void deleteCategory(UUID currentUserId, UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));

        if (category.getUserId() == null) {
            throw new ForbiddenException("System categories cannot be deleted");
        }

        if (!category.getUserId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have permission to delete this category");
        }

        boolean hasTransactions = transactionRepository.existsByCategoryId(categoryId);
        if (hasTransactions) {
            throw new ConflictException("Cannot delete category because it is referenced by one or more transactions");
        }

        categoryRepository.delete(category);
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }
}
