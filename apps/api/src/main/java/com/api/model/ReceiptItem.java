package com.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "receipt_items",
        indexes = {
                @Index(name = "idx_receipt_items_receipt_id", columnList = "receipt_id"),
                @Index(name = "idx_receipt_items_category_id", columnList = "category_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ReceiptItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "receipt_id", nullable = false)
    private Receipt receipt;

    private String name;

    private BigDecimal quantity;

    @Column(name = "unit_price", precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "total_price", precision = 19, scale = 4)
    private BigDecimal totalPrice;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "confidence_score")
    private BigDecimal confidenceScore;
}