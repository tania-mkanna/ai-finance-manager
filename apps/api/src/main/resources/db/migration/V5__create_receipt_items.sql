CREATE TABLE receipt_items (
    id UUID PRIMARY KEY,
    receipt_id UUID NOT NULL,
    name VARCHAR(255),
    quantity DECIMAL,
    unit_price DECIMAL(19,4),
    total_price DECIMAL(19,4),
    category_id UUID,
    confidence_score DECIMAL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_receipt_items_receipt
        FOREIGN KEY (receipt_id)
            REFERENCES receipts(id),
    CONSTRAINT fk_receipt_items_category
        FOREIGN KEY (category_id)
            REFERENCES categories(id)
);