CREATE TABLE krishi.automation_rules
(
    id UUID PRIMARY KEY,
    rule_name VARCHAR(150) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    action_type VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

-- Seed Rule 1: DISEASE_REPORTED -> CREATE_NOTIFICATION
INSERT INTO krishi.automation_rules (id, rule_name, event_type, action_type, enabled, created_at)
VALUES ('a1b2c3d4-e5f6-7a8b-9c0d-e1f2a3b4c5d6', 'Disease Reported Notification', 'DISEASE_REPORTED', 'CREATE_NOTIFICATION', TRUE, CURRENT_TIMESTAMP);

-- Seed Rule 2: TREATMENT_COMPLETED -> CREATE_TASK
INSERT INTO krishi.automation_rules (id, rule_name, event_type, action_type, enabled, created_at)
VALUES ('b2c3d4e5-f67a-8b9c-0d1e-2f3a4b5c6d7e', 'Treatment Completed Follow-up Task', 'TREATMENT_COMPLETED', 'CREATE_TASK', TRUE, CURRENT_TIMESTAMP);

-- Seed Rule 3: TASK_COMPLETED -> CREATE_NOTIFICATION
INSERT INTO krishi.automation_rules (id, rule_name, event_type, action_type, enabled, created_at)
VALUES ('c3d4e5f6-7a8b-9c0d-e1f2-3a4b5c6d7e8f', 'Task Completed Notification', 'TASK_COMPLETED', 'CREATE_NOTIFICATION', TRUE, CURRENT_TIMESTAMP);
