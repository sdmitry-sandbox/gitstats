--liquibase formatted sql

--changeset susol-da:create-merge-requests-table
CREATE TABLE IF NOT EXISTS merge_requests (
    id BIGSERIAL PRIMARY KEY,
    gitlab_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    author_id BIGINT,
    approver_id BIGINT,
    title VARCHAR(500),
    gitlab_state VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    source_branch VARCHAR(255),
    target_branch VARCHAR(255),
    web_url VARCHAR(500),
    rows_added INTEGER,
    rows_deleted INTEGER,
    ai_review_started_at TIMESTAMP WITH TIME ZONE,
    ai_review_finished_at TIMESTAMP WITH TIME ZONE,
    ready_for_review_at TIMESTAMP WITH TIME ZONE,
    review_requested_at TIMESTAMP WITH TIME ZONE,
    review_finished_at TIMESTAMP WITH TIME ZONE,
    human_review_duration_seconds BIGINT,
    bot_review_duration_seconds BIGINT,
    approved_at TIMESTAMP WITH TIME ZONE,
    merged_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_merge_requests_project_id FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_merge_requests_author_id FOREIGN KEY (author_id) REFERENCES users(id),
    CONSTRAINT fk_merge_requests_approver_id FOREIGN KEY (approver_id) REFERENCES users(id)
);

--changeset susol-da:create-merge-requests-indexes
CREATE INDEX IF NOT EXISTS idx_merge_requests_gitlab_id ON merge_requests(gitlab_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_merge_requests_project_id_gitlab_id ON merge_requests(project_id, gitlab_id);
CREATE INDEX IF NOT EXISTS idx_merge_requests_author_id ON merge_requests(author_id);
CREATE INDEX IF NOT EXISTS idx_merge_requests_approver_id ON merge_requests(approver_id);
