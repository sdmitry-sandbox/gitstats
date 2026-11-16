--liquibase formatted sql

--changeset susol-da:create-scheduled-tasks-table
CREATE TABLE IF NOT EXISTS scheduled_tasks (
    id BIGSERIAL PRIMARY KEY,
    task_type VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    payload TEXT,
    state VARCHAR(50) NOT NULL,
    scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    retries INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

--changeset susol-da:create-scheduled-tasks-indexes
CREATE UNIQUE INDEX IF NOT EXISTS idx_scheduled_tasks_idempotency_key ON scheduled_tasks(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_scheduled_tasks_scheduled_at_state ON scheduled_tasks(scheduled_at, state);
