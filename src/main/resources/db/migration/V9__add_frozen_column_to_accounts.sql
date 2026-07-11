-- V9: Add frozen flag to accounts table for admin freeze/unfreeze workflow.
-- Default false ensures all existing accounts remain unfrozen.
ALTER TABLE accounts
    ADD COLUMN frozen BOOLEAN NOT NULL DEFAULT FALSE;
