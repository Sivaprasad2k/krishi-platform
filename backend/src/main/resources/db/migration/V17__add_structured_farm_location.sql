-- Migration V17: Add structured farm location fields

ALTER TABLE krishi.farms
    ADD COLUMN state VARCHAR(100),
    ADD COLUMN district VARCHAR(100),
    ADD COLUMN latitude DOUBLE PRECISION,
    ADD COLUMN longitude DOUBLE PRECISION;

CREATE INDEX idx_farms_state_district ON krishi.farms(state, district);
