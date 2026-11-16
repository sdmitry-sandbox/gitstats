--liquibase formatted sql

--changeset susol-da:create-merge-request-comments-table
CREATE TABLE IF NOT EXISTS merge_request_comments (
    id BIGSERIAL PRIMARY KEY,
    gitlab_id BIGINT NOT NULL,
    merge_request_id BIGINT NOT NULL,
    author_id BIGINT,
    message TEXT,
    is_resolved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_merge_request_comments_merge_request_id FOREIGN KEY (merge_request_id) REFERENCES merge_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_merge_request_comments_author_id FOREIGN KEY (author_id) REFERENCES users(id)
);

--changeset susol-da:create-merge-request-comments-indexes
CREATE UNIQUE INDEX IF NOT EXISTS idx_merge_request_comments_mr_id_gitlab_id ON merge_request_comments(merge_request_id, gitlab_id);
CREATE INDEX IF NOT EXISTS idx_merge_request_comments_author_id ON merge_request_comments(author_id);
