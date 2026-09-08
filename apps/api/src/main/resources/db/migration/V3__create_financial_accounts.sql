CREATE TYPE financial_account_type AS ENUM (
    'CASH',
    'BANK_ACCOUNT',
    'CREDIT_CARD',
    'DEBIT_CARD',
    'DIGITAL_WALLET',
    'OTHER'
);

CREATE TABLE financial_accounts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    type financial_account_type NOT NULL,
    currency VARCHAR(3),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_financial_accounts_user
        FOREIGN KEY (user_id)
            REFERENCES users(id)
);