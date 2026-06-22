CREATE TABLE krishi.tasks
(
    id UUID PRIMARY KEY,
    treatment_id UUID NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    due_date TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT fk_task_treatment
        FOREIGN KEY (treatment_id)
            REFERENCES krishi.treatments(id)
);
