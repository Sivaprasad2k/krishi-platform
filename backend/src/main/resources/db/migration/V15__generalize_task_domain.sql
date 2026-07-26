-- Migration V15: Generalize Task domain with crop_planting_id and optional treatment_id

-- 1. Add crop_planting_id column (initially nullable)
ALTER TABLE krishi.tasks 
    ADD COLUMN crop_planting_id UUID;

-- 2. Backfill crop_planting_id for existing treatment-linked tasks
UPDATE krishi.tasks t
SET crop_planting_id = di.crop_planting_id
FROM krishi.treatments tr
JOIN krishi.disease_incidents di ON tr.disease_incident_id = di.id
WHERE t.treatment_id = tr.id;

-- 3. Apply NOT NULL constraint after backfill
ALTER TABLE krishi.tasks 
    ALTER COLUMN crop_planting_id SET NOT NULL;

-- 4. Add FK constraint referencing krishi.crop_plantings
ALTER TABLE krishi.tasks
    ADD CONSTRAINT fk_task_crop_planting
        FOREIGN KEY (crop_planting_id)
            REFERENCES krishi.crop_plantings(id);

-- 5. Relax treatment_id to allow NULL
ALTER TABLE krishi.tasks 
    ALTER COLUMN treatment_id DROP NOT NULL;

-- 6. Add task_type and source columns with defaults
ALTER TABLE krishi.tasks
    ADD COLUMN task_type VARCHAR(50) NOT NULL DEFAULT 'DISEASE_TREATMENT',
    ADD COLUMN source VARCHAR(50) NOT NULL DEFAULT 'TREATMENT';

-- 7. Intelligent backfill for auto-generated tasks
UPDATE krishi.tasks
SET source = 'AUTOMATION', task_type = 'CROP_INSPECTION'
WHERE title = 'INSPECT_CROP_RECOVERY';

-- 8. Add useful indexes for queries
CREATE INDEX idx_tasks_crop_planting_id ON krishi.tasks(crop_planting_id);
CREATE INDEX idx_tasks_treatment_id ON krishi.tasks(treatment_id);
CREATE INDEX idx_tasks_status ON krishi.tasks(status);
CREATE INDEX idx_tasks_due_date ON krishi.tasks(due_date);
