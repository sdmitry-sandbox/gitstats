--liquibase formatted sql

--changeset susol-da:create-projects-table
CREATE TABLE IF NOT EXISTS projects (
    id BIGSERIAL PRIMARY KEY,
    gitlab_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(255),
    web_url VARCHAR(500),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_synced_at TIMESTAMP WITH TIME ZONE
);

--changeset susol-da:create-projects-indexes
CREATE INDEX IF NOT EXISTS idx_projects_gitlab_id ON projects(gitlab_id);
