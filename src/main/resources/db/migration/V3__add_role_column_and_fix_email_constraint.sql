-- Drop the column-level UNIQUE constraint on email created in V1.
-- Case-insensitive uniqueness is enforced by the LOWER(email) index from V2.
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;

-- Add role column for role-based access control
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';
