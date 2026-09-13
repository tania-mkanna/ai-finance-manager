CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS idx_categories_unique_active_user_name_type
    ON categories (user_id, LOWER(name), type)
    WHERE active = true
    AND user_id IS NOT NULL;