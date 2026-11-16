--liquibase formatted sql

--changeset susol-da:create-user-merge-requests-daily-mv runInTransaction:true
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT count(*) FROM pg_matviews WHERE schemaname = current_schema() AND matviewname = 'mv_user_merge_requests_daily'
DROP MATERIALIZED VIEW IF EXISTS mv_user_merge_requests_daily CASCADE;
CREATE MATERIALIZED VIEW mv_user_merge_requests_daily AS
SELECT
    mr.id AS mr_id,
    mr.gitlab_id AS gitlab_id,
    mr.project_id AS project_id,
    p.name AS project_name,
    mr.author_id AS author_id,
    au.username AS author_username,
    au.name AS author_name,
    au.is_bot AS author_is_bot,
    mr.approver_id AS approver_id,
    appu.username AS approver_username,
    appu.name AS approver_name,
    appu.is_bot AS approver_is_bot,
    COALESCE(mr.rows_added, 0) AS rows_added,
    COALESCE(mr.rows_deleted, 0) AS rows_deleted,
    COALESCE(mr.rows_added, 0) + COALESCE(mr.rows_deleted, 0) AS total_code,
    mr.created_at AS created_at,
    mr.merged_at AS merged_at,
    mr.approved_at AS approved_at,
    mr.ready_for_review_at AS ready_for_review_at,
    mr.review_requested_at AS review_requested_at,
    mr.review_finished_at AS review_finished_at,
    ((mr.created_at AT TIME ZONE 'Europe/Moscow')::date) AS contribution_date,
    ((COALESCE(mr.approved_at, mr.created_at) AT TIME ZONE 'Europe/Moscow')::date) AS approved_date
FROM merge_requests mr
    LEFT JOIN projects p ON mr.project_id = p.id
    LEFT JOIN users au ON mr.author_id = au.id
    LEFT JOIN users appu ON mr.approver_id = appu.id
WHERE mr.gitlab_state = 'MERGED'
  AND mr.target_branch NOT LIKE 'RC-%'
  AND mr.target_branch NOT LIKE 'V%';

--changeset susol-da:create-user-merge-requests-daily-mv-index runInTransaction:true
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:1 SELECT count(*) FROM pg_matviews WHERE schemaname = current_schema() AND matviewname = 'mv_user_merge_requests_daily'
CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_user_mr_daily_id
    ON mv_user_merge_requests_daily(mr_id);
CREATE INDEX IF NOT EXISTS idx_mv_user_mr_daily_author_date
    ON mv_user_merge_requests_daily(author_id, contribution_date);
CREATE INDEX IF NOT EXISTS idx_mv_user_mr_daily_approver_date
    ON mv_user_merge_requests_daily(approver_id, approved_date);
CREATE INDEX IF NOT EXISTS idx_mv_user_mr_daily_project_author_date
    ON mv_user_merge_requests_daily(project_id, contribution_date);
CREATE INDEX IF NOT EXISTS idx_mv_user_mr_daily_project_approver_date
    ON mv_user_merge_requests_daily(project_id, approved_date);
CREATE INDEX IF NOT EXISTS idx_mv_user_mr_daily_project_created
    ON mv_user_merge_requests_daily(project_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_mv_user_mr_daily_project_approved
    ON mv_user_merge_requests_daily(project_id, approved_at DESC);

--changeset susol-da:create-user-mr-comments-daily-mv runInTransaction:true
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT count(*) FROM pg_matviews WHERE schemaname = current_schema() AND matviewname = 'mv_user_merge_request_comments_daily'
DROP MATERIALIZED VIEW IF EXISTS mv_user_merge_request_comments_daily CASCADE;
CREATE MATERIALIZED VIEW mv_user_merge_request_comments_daily AS
SELECT
    c.id AS comment_id,
    c.gitlab_id AS gitlab_id,
    c.merge_request_id AS merge_request_id,
    mr.project_id AS project_id,
    p.name AS project_name,
    c.author_id AS author_id,
    cu.username AS author_username,
    cu.name AS author_name,
    cu.is_bot AS author_is_bot,
    mr.author_id AS mr_author_id,
    mru.username AS mr_author_username,
    mru.name AS mr_author_name,
    mru.is_bot AS mr_author_is_bot,
    c.created_at AS created_at,
    ((COALESCE(c.created_at, mr.created_at) AT TIME ZONE 'Europe/Moscow')::date) AS contribution_date
FROM merge_request_comments c
    JOIN merge_requests mr ON c.merge_request_id = mr.id
    LEFT JOIN projects p ON mr.project_id = p.id
    LEFT JOIN users cu ON c.author_id = cu.id
    LEFT JOIN users mru ON mr.author_id = mru.id
WHERE c.author_id IS NOT NULL
  AND mr.gitlab_state = 'MERGED'
  AND mr.target_branch NOT LIKE 'RC-%'
  AND mr.target_branch NOT LIKE 'V%';

--changeset susol-da:create-user-mr-comments-daily-mv-index runInTransaction:true
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:1 SELECT count(*) FROM pg_matviews WHERE schemaname = current_schema() AND matviewname = 'mv_user_merge_request_comments_daily'
CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_user_mr_comments_daily_id
    ON mv_user_merge_request_comments_daily(comment_id);
CREATE INDEX IF NOT EXISTS idx_mv_user_mr_comments_daily_author_date
    ON mv_user_merge_request_comments_daily(author_id, contribution_date);
CREATE INDEX IF NOT EXISTS idx_mv_user_mr_comments_daily_mr_id
    ON mv_user_merge_request_comments_daily(merge_request_id);
CREATE INDEX IF NOT EXISTS idx_mv_user_mr_comments_daily_mr_author_date
    ON mv_user_merge_request_comments_daily(mr_author_id, contribution_date);
CREATE INDEX IF NOT EXISTS idx_mv_user_mr_comments_daily_project_date
    ON mv_user_merge_request_comments_daily(project_id, contribution_date);
CREATE INDEX idx_mv_user_mr_comments_daily_project_created
    ON mv_user_merge_request_comments_daily(project_id, created_at DESC);

--changeset susol-da:create-user-mr-hourly-mv runInTransaction:true
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT count(*) FROM pg_matviews WHERE schemaname = current_schema() AND matviewname = 'mv_user_mr_hourly'
DROP MATERIALIZED VIEW IF EXISTS mv_user_mr_hourly CASCADE;
CREATE MATERIALIZED VIEW mv_user_mr_hourly AS
SELECT
    u.id AS user_id,
    u.username AS username,
    u.is_bot AS is_bot,
    t.contribution_date AS contribution_date,
    t.day_of_week AS day_of_week,
    t.hour_of_day AS hour_of_day,
    COUNT(*) AS mr_count
FROM users u
         JOIN (
    SELECT
        author_id AS user_id,
        ((ready_for_review_at AT TIME ZONE 'Europe/Moscow')::date) AS contribution_date,
    EXTRACT(ISODOW FROM (ready_for_review_at AT TIME ZONE 'Europe/Moscow'))::INTEGER AS day_of_week,
        EXTRACT(HOUR FROM (ready_for_review_at AT TIME ZONE 'Europe/Moscow'))::INTEGER AS hour_of_day
FROM merge_requests
WHERE author_id IS NOT NULL
  AND ready_for_review_at IS NOT NULL
  AND gitlab_state = 'MERGED'
  AND merge_requests.target_branch NOT LIKE 'RC-%'
  AND merge_requests.target_branch NOT LIKE 'V%'
UNION ALL
SELECT
    author_id AS user_id,
    ((merged_at AT TIME ZONE 'Europe/Moscow')::date) AS contribution_date,
        EXTRACT(ISODOW FROM (merged_at AT TIME ZONE 'Europe/Moscow'))::INTEGER AS day_of_week,
        EXTRACT(HOUR FROM (merged_at AT TIME ZONE 'Europe/Moscow'))::INTEGER AS hour_of_day
FROM merge_requests
WHERE author_id IS NOT NULL
  AND merged_at IS NOT NULL
  AND gitlab_state = 'MERGED'
  AND merge_requests.target_branch NOT LIKE 'RC-%'
  AND merge_requests.target_branch NOT LIKE 'V%'
UNION ALL
SELECT
    approver_id AS user_id,
    ((COALESCE(approved_at, created_at) AT TIME ZONE 'Europe/Moscow')::date) AS contribution_date,
        EXTRACT(ISODOW FROM (COALESCE(approved_at, created_at) AT TIME ZONE 'Europe/Moscow'))::INTEGER AS day_of_week,
        EXTRACT(HOUR FROM (COALESCE(approved_at, created_at) AT TIME ZONE 'Europe/Moscow'))::INTEGER AS hour_of_day
FROM merge_requests
WHERE approver_id IS NOT NULL
  AND gitlab_state = 'MERGED'
  AND merge_requests.target_branch NOT LIKE 'RC-%'
  AND merge_requests.target_branch NOT LIKE 'V%'
UNION ALL
SELECT
    c.author_id AS user_id,
    ((COALESCE(c.created_at, mr.created_at) AT TIME ZONE 'Europe/Moscow')::date) AS contribution_date,
        EXTRACT(ISODOW FROM (COALESCE(c.created_at, mr.created_at) AT TIME ZONE 'Europe/Moscow'))::INTEGER AS day_of_week,
        EXTRACT(HOUR FROM (COALESCE(c.created_at, mr.created_at) AT TIME ZONE 'Europe/Moscow'))::INTEGER AS hour_of_day
FROM merge_request_comments c
    JOIN merge_requests mr ON c.merge_request_id = mr.id
WHERE c.author_id IS NOT NULL
  AND mr.gitlab_state = 'MERGED'
  AND mr.target_branch NOT LIKE 'RC-%'
  AND mr.target_branch NOT LIKE 'V%'
    ) t ON u.id = t.user_id
GROUP BY u.id, u.username, u.is_bot,
    t.contribution_date,
    t.day_of_week,
    t.hour_of_day;

--changeset susol-da:create-user-mr-hourly-mv-index runInTransaction:true
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:1 SELECT count(*) FROM pg_matviews WHERE schemaname = current_schema() AND matviewname = 'mv_user_mr_hourly'
CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_user_mr_hourly
    ON mv_user_mr_hourly(user_id, contribution_date, hour_of_day);