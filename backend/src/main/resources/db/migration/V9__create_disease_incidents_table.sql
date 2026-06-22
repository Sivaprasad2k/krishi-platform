CREATE TABLE krishi.disease_incidents
(
    id UUID PRIMARY KEY,
    crop_planting_id UUID NOT NULL,
    disease_name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    reported_at TIMESTAMP NOT NULL,
    reviewed_at TIMESTAMP,
    treated_at TIMESTAMP,
    closed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT fk_disease_incident_planting
        FOREIGN KEY (crop_planting_id)
            REFERENCES krishi.crop_plantings(id)
);
