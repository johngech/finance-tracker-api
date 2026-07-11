-- V8: Add active flag to users table for admin suspend/activate workflow.
-- Default true ensures all existing users remain active.
ALTER TABLE users
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
