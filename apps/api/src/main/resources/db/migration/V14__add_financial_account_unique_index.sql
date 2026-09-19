CREATE UNIQUE INDEX CONCURRENTLY idx_financial_accounts_unique_active_user_name
    ON financial_accounts (user_id, LOWER(name))
    WHERE active = true;