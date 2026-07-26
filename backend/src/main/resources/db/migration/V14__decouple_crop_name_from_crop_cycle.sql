-- V14__decouple_crop_name_from_crop_cycle.sql
-- Decouples Crop names from the Season (crop_cycles) table

ALTER TABLE krishi.crop_cycles DROP COLUMN crop_name;

ALTER TABLE krishi.crop_cycles ADD COLUMN season_type VARCHAR(50);
ALTER TABLE krishi.crop_cycles ADD COLUMN year INT;
ALTER TABLE krishi.crop_cycles ADD COLUMN notes TEXT;

-- Make name optional to support fallback dynamically generated names based on season type + year
ALTER TABLE krishi.crop_cycles ALTER COLUMN name DROP NOT NULL;
