-- Migration V16: Add performance composite indexes for high-frequency workspace and notification queries

CREATE INDEX IF NOT EXISTS idx_tasks_planting_status ON krishi.tasks(crop_planting_id, status);
CREATE INDEX IF NOT EXISTS idx_disease_incidents_planting_status ON krishi.disease_incidents(crop_planting_id, status);
CREATE INDEX IF NOT EXISTS idx_notifications_user_read ON krishi.notifications(user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_crop_cycles_farm_status ON krishi.crop_cycles(farm_id, status);
