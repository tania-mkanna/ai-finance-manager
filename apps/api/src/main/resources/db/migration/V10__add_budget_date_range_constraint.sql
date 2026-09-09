ALTER TABLE budgets
    ADD CONSTRAINT ck_budgets_date_range
        CHECK (end_date >= start_date);