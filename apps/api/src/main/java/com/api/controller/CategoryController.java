package com.api.controller;

import com.api.dto.CategoryResponse;
import com.api.dto.CreateCategoryRequest;
import com.api.dto.UpdateCategoryRequest;
import com.api.enums.CategoryType;
import com.api.exception.NotFoundException;
import com.api.model.User;
import com.api.repository.UserRepository;
import com.api.service.interfaces.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final UserRepository userRepository;

    public CategoryController(CategoryService categoryService, UserRepository userRepository) {
        this.categoryService = categoryService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategories(
            Authentication authentication,
            @RequestParam(required = false) CategoryType type
    ) {
        UUID currentUserId = resolveCurrentUserId(authentication);
        return ResponseEntity.ok(categoryService.getCategories(currentUserId, type));
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> getCategory(
            Authentication authentication,
            @PathVariable UUID categoryId
    ) {
        UUID currentUserId = resolveCurrentUserId(authentication);
        return ResponseEntity.ok(categoryService.getCategory(currentUserId, categoryId));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            Authentication authentication,
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        UUID currentUserId = resolveCurrentUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.createCategory(currentUserId, request));
    }

    @PatchMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> updateCategory(
            Authentication authentication,
            @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        UUID currentUserId = resolveCurrentUserId(authentication);
        return ResponseEntity.ok(categoryService.updateCategory(currentUserId, categoryId, request));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(
            Authentication authentication,
            @PathVariable UUID categoryId
    ) {
        UUID currentUserId = resolveCurrentUserId(authentication);
        categoryService.deleteCategory(currentUserId, categoryId);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveCurrentUserId(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return user.getId();
    }
}
