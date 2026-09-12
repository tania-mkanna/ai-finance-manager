package com.api.model;

import com.api.enums.FinancialAccountType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

import java.util.UUID;

@Entity
@Table(
        name = "financial_accounts",
        indexes = {
                @Index(name = "idx_financial_accounts_user_type", columnList = "user_id, type")
        }
)
@SoftDelete(strategy = SoftDeleteType.ACTIVE, columnName = "active")
@Getter
@Setter
@NoArgsConstructor
public class FinancialAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(
            name = "type",
            nullable = false,
            columnDefinition = "financial_account_type"
    )
    private FinancialAccountType type;

    @Column(length = 3)
    private String currency;
}