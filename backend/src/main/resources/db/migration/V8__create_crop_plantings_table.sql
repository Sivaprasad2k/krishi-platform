CREATE TABLE krishi.crop_plantings
(
    id UUID PRIMARY KEY,
    crop_cycle_id UUID NOT NULL,
    crop_name VARCHAR(100) NOT NULL,
    crop_variety VARCHAR(100),
    planting_date TIMESTAMP NOT NULL,
    expected_harvest_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT fk_crop_planting_cycle
        FOREIGN KEY (crop_cycle_id)
            REFERENCES krishi.crop_cycles(id)
);
