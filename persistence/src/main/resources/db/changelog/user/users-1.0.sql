--liquibase formatted sql

--changeset susol-da:create-users-table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    gitlab_id BIGINT NOT NULL,
    username VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    gitlab_state VARCHAR(50) NOT NULL,
    is_bot BOOLEAN NOT NULL DEFAULT FALSE,
    avatar_url VARCHAR(500),
    web_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

--changeset susol-da:create-users-indexes
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_gitlab_id ON users(gitlab_id);
