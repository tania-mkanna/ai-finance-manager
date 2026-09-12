package com.api.repository;

import com.api.enums.CategoryType;
import com.api.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    @Query("""
            SELECT c FROM Category c
            WHERE (c.userId IS NULL OR c.userId = :currentUserId)
            ORDER BY CASE WHEN c.userId IS NULL THEN 0 ELSE 1 END, c.name ASC
            """)
    List<Category> findVisibleCategories(
            @Param("currentUserId") UUID currentUserId
    );

    @Query("""
            SELECT c FROM Category c
            WHERE (c.userId IS NULL OR c.userId = :currentUserId)
              AND (c.type = :type OR c.type = :bothType)
            ORDER BY CASE WHEN c.userId IS NULL THEN 0 ELSE 1 END, c.name ASC
            """)
    List<Category> findVisibleCategoriesByType(
            @Param("currentUserId") UUID currentUserId,
            @Param("type") CategoryType type,
            @Param("bothType") CategoryType bothType
    );

    Optional<Category> findByIdAndUserId(UUID categoryId, UUID userId);

    boolean existsByUserIdAndNameIgnoreCaseAndType(
            UUID userId,
            String name,
            CategoryType type
    );

    boolean existsByUserIdAndNameIgnoreCaseAndTypeAndIdNot(
            UUID userId,
            String name,
            CategoryType type,
            UUID id
    );
}