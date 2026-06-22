CREATE TABLE IF NOT EXISTS krishi.domain_events
(
    id UUID PRIMARY KEY,

    aggregate_type VARCHAR(100) NOT NULL,

    aggregate_id UUID NOT NULL,

    event_type VARCHAR(100) NOT NULL,

    event_data TEXT,

    occurred_at TIMESTAMP NOT NULL,

    created_by UUID
    );