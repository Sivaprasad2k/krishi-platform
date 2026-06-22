CREATE TABLE krishi.crop_cycles
(
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    crop_name VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    farm_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT fk_crop_cycle_farm
        FOREIGN KEY (farm_id)
            REFERENCES krishi.farms(id)
);
