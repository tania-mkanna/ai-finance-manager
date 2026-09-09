CREATE TYPE transaction_type AS ENUM (
    'INCOME',
    'EXPENSE'
);

CREATE TYPE transaction_source AS ENUM (
    'MANUAL',
    'RECEIPT'
);

CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    category_id UUID NOT NULL,
    account_id UUID NOT NULL,
    receipt_id UUID,
    type transaction_type NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    transaction_date TIMESTAMP NOT NULL,
    merchant_name VARCHAR(255),
    description TEXT,
    source transaction_source NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_transactions_user
        FOREIGN KEY (user_id)
            REFERENCES users(id),
    CONSTRAINT fk_transactions_category
        FOREIGN KEY (category_id)
            REFERENCES categories(id),
    CONSTRAINT fk_transactions_account
        FOREIGN KEY (account_id)
            REFERENCES financial_accounts(id),
    CONSTRAINT fk_transactions_receipt
        FOREIGN KEY (receipt_id)
            REFERENCES receipts(id)
);