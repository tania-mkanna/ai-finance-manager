CREATE TYPE category_type AS ENUM (
    'INCOME',
    'EXPENSE',
    'BOTH'
);

CREATE TABLE categories (
                            id UUID PRIMARY KEY,
                            user_id UUID,
                            name VARCHAR(255) NOT NULL,
                            type category_type NOT NULL,
                            icon VARCHAR(255),
                            color VARCHAR(255),
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            CONSTRAINT fk_categories_user
                                FOREIGN KEY (user_id)
                                    REFERENCES users(id)
);