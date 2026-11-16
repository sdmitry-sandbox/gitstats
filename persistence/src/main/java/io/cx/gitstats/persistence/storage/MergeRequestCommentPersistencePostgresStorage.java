package io.cx.gitstats.persistence.storage;

import io.cx.gitstats.domain.model.MergeRequestComment;
import io.cx.gitstats.domain.persistence.MergeRequestCommentPersistenceStorage;
import io.cx.gitstats.persistence.entity.MergeRequestCommentEntity;
import io.cx.gitstats.persistence.mapper.MergeRequestCommentPersistenceMapper;
import io.cx.gitstats.persistence.repository.MergeRequestCommentRepository;
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
public class MergeRequestCommentPersistencePostgresStorage implements MergeRequestCommentPersistenceStorage {

    private final MergeRequestCommentRepository mergeRequestCommentRepository;
    private final MergeRequestCommentPersistenceMapper mapper;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public MergeRequestComment save(MergeRequestComment comment) {
        MergeRequestCommentEntity entity = mapper.toEntity(comment);
        if (entity.getIsResolved() == null) {
            entity.setIsResolved(false);
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        MergeRequestCommentEntity saved = mergeRequestCommentRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void saveAll(List<MergeRequestComment> comments) {
        if (comments == null || comments.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO merge_request_comments (gitlab_id, merge_request_id, author_id, message, is_resolved, resolved_at, created_at)
                VALUES (:gitlabId, :mergeRequestId, :authorId, :message, :isResolved, :resolvedAt, :createdAt)
                ON CONFLICT (merge_request_id, gitlab_id) DO NOTHING
                """;

        var batchParams = new ArrayList<SqlParameterSource>();
        for (var comment : comments) {
            var createdAt = comment.getCreatedAt() != null ? comment.getCreatedAt() : Instant.now();
            var params = new MapSqlParameterSource()
                    .addValue("gitlabId", comment.getGitlabId())
                    .addValue("mergeRequestId", comment.getMergeRequestId())
                    .addValue("authorId", comment.getAuthorId())
                    .addValue("message", comment.getMessage())
                    .addValue("isResolved", comment.getIsResolved() != null ? comment.getIsResolved() : false)
                    .addValue("resolvedAt", comment.getResolvedAt() != null ? Timestamp.from(comment.getResolvedAt()) : null)
                    .addValue("createdAt", Timestamp.from(createdAt));
            batchParams.add(params);
        }

        jdbcTemplate.batchUpdate(sql, batchParams.toArray(new SqlParameterSource[0]));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MergeRequestComment> findById(Long id) {
        return mergeRequestCommentRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MergeRequestComment> findAllByMergeRequestId(Long mergeRequestId) {
        return mapper.toDomainList(mergeRequestCommentRepository.findAllByMergeRequestId(mergeRequestId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MergeRequestComment> findAll() {
        return mapper.toDomainList(mergeRequestCommentRepository.findAll());
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        mergeRequestCommentRepository.deleteById(id);
    }
}
