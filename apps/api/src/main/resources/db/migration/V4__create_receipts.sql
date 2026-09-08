CREATE TYPE receipt_status AS ENUM (
    'UPLOADED',
    'PROCESSING',
    'REVIEW_REQUIRED',
    'CONFIRMED',
    'FAILED'
);

CREATE TABLE receipts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    file_url TEXT NOT NULL,
    file_name VARCHAR(255),
    mime_type VARCHAR(255),
    status receipt_status NOT NULL,
    receipt_date DATE,
    merchant_name VARCHAR(255),
    total_amount DECIMAL(19,4),
    currency VARCHAR(3),
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_receipts_user
        FOREIGN KEY (user_id)
            REFERENCES users(id)
);