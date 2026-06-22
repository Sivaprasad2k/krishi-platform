CREATE TABLE krishi.treatments
(
    id UUID PRIMARY KEY,
    disease_incident_id UUID NOT NULL,
    treatment_name VARCHAR(150) NOT NULL,
    description TEXT,
    recommended_dosage VARCHAR(100),
    application_method VARCHAR(100),
    start_date TIMESTAMP,
    completion_date TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT fk_treatment_disease_incident
        FOREIGN KEY (disease_incident_id)
            REFERENCES krishi.disease_incidents(id)
);
