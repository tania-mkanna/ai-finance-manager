CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transactions_user_date
    ON transactions (user_id, transaction_date DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transactions_user_category_date
    ON transactions (user_id, category_id, transaction_date DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transactions_user_account_date
    ON transactions (user_id, account_id, transaction_date DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transactions_receipt_id
    ON transactions (receipt_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_budgets_user_category_dates
    ON budgets (user_id, category_id, start_date, end_date);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_categories_user_type
    ON categories (user_id, type);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_financial_accounts_user_type
    ON financial_accounts (user_id, type);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_receipts_user_status_uploaded_at
    ON receipts (user_id, status, uploaded_at DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_receipt_items_receipt_id
    ON receipt_items (receipt_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_receipt_items_category_id
    ON receipt_items (category_id);