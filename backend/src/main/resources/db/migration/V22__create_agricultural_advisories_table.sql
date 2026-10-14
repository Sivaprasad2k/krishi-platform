-- Migration V22: Agricultural Advisory Rules Engine and Contextual Recommendations

CREATE TABLE krishi.agricultural_advisories (
    id UUID PRIMARY KEY,
    advisory_fingerprint VARCHAR(160) NOT NULL,
    farm_id UUID NOT NULL REFERENCES krishi.farms(id) ON DELETE CASCADE,
    crop_planting_id UUID NOT NULL REFERENCES krishi.crop_plantings(id) ON DELETE CASCADE,
    task_id UUID REFERENCES krishi.tasks(id) ON DELETE SET NULL,
    rule_code VARCHAR(64) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    recommended_action TEXT NOT NULL,
    target_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    provenance VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_evaluated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX uq_active_agri_advisory ON krishi.agricultural_advisories (advisory_fingerprint) WHERE status = 'ACTIVE';
CREATE INDEX idx_agri_advisories_planting_status ON krishi.agricultural_advisories(crop_planting_id, status);
CREATE INDEX idx_agri_advisories_farm_status ON krishi.agricultural_advisories(farm_id, status);
CREATE INDEX idx_agri_advisories_task ON krishi.agricultural_advisories(task_id) WHERE task_id IS NOT NULL;
CREATE INDEX idx_agri_advisories_target_date ON krishi.agricultural_advisories(target_date);
