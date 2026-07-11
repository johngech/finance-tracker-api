-- Add indexes on FK columns for efficient JOINs in TransactionSpecification
CREATE INDEX idx_transactions_type ON transactions(type);
CREATE INDEX idx_transactions_category ON transactions(category);

-- Clean up whitespace-only categories: null out referencing rows first, then delete
UPDATE transactions SET category = NULL WHERE TRIM(category) = '';
DELETE FROM transaction_categories WHERE TRIM(name) = '';
