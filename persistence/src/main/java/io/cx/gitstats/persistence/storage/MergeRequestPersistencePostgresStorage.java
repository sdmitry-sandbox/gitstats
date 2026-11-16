package io.cx.gitstats.persistence.storage;

import io.cx.gitstats.domain.model.MergeRequest;
import io.cx.gitstats.domain.model.MergeRequestStatus;
import io.cx.gitstats.domain.persistence.MergeRequestPersistenceStorage;
import io.cx.gitstats.persistence.entity.MergeRequestEntity;
import io.cx.gitstats.persistence.mapper.MergeRequestPersistenceMapper;
import io.cx.gitstats.persistence.repository.MergeRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MergeRequestPersistencePostgresStorage implements MergeRequestPersistenceStorage {

    private final MergeRequestRepository mergeRequestRepository;
    private final MergeRequestPersistenceMapper mapper;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public MergeRequest save(MergeRequest mergeRequest) {
        MergeRequestEntity entity = mapper.toEntity(mergeRequest);
        MergeRequestEntity saved = mergeRequestRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void saveAll(List<MergeRequest> mergeRequests) {
        if (mergeRequests == null || mergeRequests.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO merge_requests (
                    gitlab_id, project_id, author_id, approver_id, title, gitlab_state, status,
                    source_branch, target_branch, web_url, rows_added, rows_deleted,
                    approved_at, merged_at, created_at,
                    ai_review_started_at, ai_review_finished_at, ready_for_review_at,
                    review_requested_at, review_finished_at,
                    human_review_duration_seconds, bot_review_duration_seconds
                )
                VALUES (
                    :gitlabId, :projectId, :authorId, :approverId, :title, :gitlabState, :status,
                    :sourceBranch, :targetBranch, :webUrl, :rowsAdded, :rowsDeleted,
                    :approvedAt, :mergedAt, :createdAt,
                    :aiReviewStartedAt, :aiReviewFinishedAt, :readyForReviewAt,
                    :reviewRequestedAt, :reviewFinishedAt,
                    :humanReviewDurationSeconds, :botReviewDurationSeconds
                )
                ON CONFLICT (project_id, gitlab_id) DO NOTHING
                """;

        var batchParams = new ArrayList<SqlParameterSource>();
        for (var mr : mergeRequests) {
            batchParams.add(createParams(mr));
        }

        jdbcTemplate.batchUpdate(sql, batchParams.toArray(new SqlParameterSource[0]));
    }

    @Override
    @Transactional
    public void updateAll(List<MergeRequest> mergeRequests) {
        if (mergeRequests == null || mergeRequests.isEmpty()) {
            return;
        }

        String sql = """
                UPDATE merge_requests
                SET author_id = :authorId, approver_id = :approverId, title = :title,
                    gitlab_state = :gitlabState, status = :status,
                    source_branch = :sourceBranch, target_branch = :targetBranch, web_url = :webUrl,
                    rows_added = :rowsAdded, rows_deleted = :rowsDeleted,
                    approved_at = :approvedAt, merged_at = :mergedAt, created_at = :createdAt,
                    ai_review_started_at = :aiReviewStartedAt,
                    ai_review_finished_at = :aiReviewFinishedAt,
                    ready_for_review_at = :readyForReviewAt,
                    review_requested_at = :reviewRequestedAt,
                    review_finished_at = :reviewFinishedAt,
                    human_review_duration_seconds = :humanReviewDurationSeconds,
                    bot_review_duration_seconds = :botReviewDurationSeconds
                WHERE gitlab_id = :gitlabId AND project_id = :projectId
                """;

        var batchParams = new ArrayList<SqlParameterSource>();
        for (var mr : mergeRequests) {
            batchParams.add(createParams(mr));
        }

        jdbcTemplate.batchUpdate(sql, batchParams.toArray(new SqlParameterSource[0]));
    }

    private MapSqlParameterSource createParams(MergeRequest mr) {
        var createdAt = mr.getCreatedAt() != null ? mr.getCreatedAt() : Instant.now();
        return new MapSqlParameterSource()
                .addValue("gitlabId", mr.getGitlabId())
                .addValue("projectId", mr.getProjectId())
                .addValue("authorId", mr.getAuthorId())
                .addValue("approverId", mr.getApproverId())
                .addValue("title", mr.getTitle())
                .addValue("gitlabState", mr.getGitlabState() != null ? mr.getGitlabState().name() : null)
                .addValue("status", mr.getStatus() != null ? mr.getStatus().name() : null)
                .addValue("sourceBranch", mr.getSourceBranch())
                .addValue("targetBranch", mr.getTargetBranch())
                .addValue("webUrl", mr.getWebUrl())
                .addValue("rowsAdded", mr.getRowsAdded())
                .addValue("rowsDeleted", mr.getRowsDeleted())
                .addValue("approvedAt", toTimestamp(mr.getApprovedAt()))
                .addValue("mergedAt", toTimestamp(mr.getMergedAt()))
                .addValue("createdAt", Timestamp.from(createdAt))
                .addValue("aiReviewStartedAt", toTimestamp(mr.getAiReviewStartedAt()))
                .addValue("aiReviewFinishedAt", toTimestamp(mr.getAiReviewFinishedAt()))
                .addValue("readyForReviewAt", toTimestamp(mr.getReadyForReviewAt()))
                .addValue("reviewRequestedAt", toTimestamp(mr.getReviewRequestedAt()))
                .addValue("reviewFinishedAt", toTimestamp(mr.getReviewFinishedAt()))
                .addValue("humanReviewDurationSeconds", mr.getHumanReviewDurationSeconds())
                .addValue("botReviewDurationSeconds", mr.getBotReviewDurationSeconds());
    }

    private Timestamp toTimestamp(Instant instant) {
        return instant != null ? Timestamp.from(instant) : null;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MergeRequest> findById(Long id) {
        return mergeRequestRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MergeRequest> findByProjectIdAndGitlabId(Long projectId, Long gitlabId) {
        return mergeRequestRepository.findByProjectIdAndGitlabId(projectId, gitlabId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MergeRequest> findByGitlabId(Long gitlabId) {
        return mergeRequestRepository.findByGitlabId(gitlabId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MergeRequest> findAllByProjectId(Long projectId) {
        return mapper.toDomainList(mergeRequestRepository.findAllByProjectId(projectId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MergeRequest> findAll() {
        return mapper.toDomainList(mergeRequestRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public int countByProjectIdAndStatus(Long projectId, MergeRequestStatus status) {
        return mergeRequestRepository.countByProjectIdAndStatus(projectId, status);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        mergeRequestRepository.deleteById(id);
    }
}
