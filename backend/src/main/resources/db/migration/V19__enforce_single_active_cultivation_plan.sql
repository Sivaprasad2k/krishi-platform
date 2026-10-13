-- Migration V19: Enforce unique active cultivation plan per crop planting
-- Prevents concurrent generation from creating multiple active plans for the same crop planting.

CREATE UNIQUE INDEX IF NOT EXISTS uq_active_cultivation_plan_per_planting
    ON krishi.cultivation_plans (crop_planting_id)
    WHERE status = 'ACTIVE';
