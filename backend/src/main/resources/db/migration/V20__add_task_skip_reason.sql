-- Migration V20: Add skip_reason column to tasks table for operational traceability
ALTER TABLE krishi.tasks
    ADD COLUMN IF NOT EXISTS skip_reason VARCHAR(255);
