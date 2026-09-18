CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS idx_financial_accounts_unique_active_user_name
    ON financial_accounts (user_id, LOWER(name))
    WHERE active = true;