package com.api.model;

import com.api.enums.CategoryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

import java.util.UUID;

@Entity
@Table(
        name = "categories",
        indexes = {
                @Index(name = "idx_categories_user_type", columnList = "user_id, type")
        }
)
@SoftDelete(strategy = SoftDeleteType.ACTIVE, columnName = "active")
@Getter
@Setter
@NoArgsConstructor
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryType type;

    private String icon;

    private String color;
}
