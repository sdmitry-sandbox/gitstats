package io.cx.gitstats.persistence.storage;

import io.cx.gitstats.domain.model.activity.UserActivityItem;
import io.cx.gitstats.domain.model.activity.UserActivityType;
import io.cx.gitstats.domain.persistence.UserActivityPersistenceStorage;
import io.cx.gitstats.domain.service.analytics.WorkingDaysCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityPersistencePostgresStorage implements UserActivityPersistenceStorage {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private record RawActivityRow(
            Long id,
            Long gitlabId,
            UserActivityType type,
            Long mergeRequestId,
            Long mrGitlabId,
            Long projectId,
            String projectName,
            Instant activityTime,
            LocalDate contributionDate,
            Integer rowsAdded,
            Integer rowsDeleted
    ) {}

    private record MrDetails(
            Long id,
            Long gitlabId,
            String title,
            String webUrl,
            Instant readyForReviewAt,
            Instant reviewRequestedAt,
            Instant createdAt,
            Instant approvedAt
    ) {}

    private record CommentRef(
            Long id,
            Long mergeRequestId,
            Instant createdAt
    ) {}

    @Override
    @Transactional(readOnly = true)
    public List<UserActivityItem> findUserActivities(
            Long userId,
            LocalDate from,
            LocalDate to,
            boolean includeMr,
            boolean includeApprovals,
            boolean includeComments,
            int limit
    ) {
        if (!includeMr && !includeApprovals && !includeComments) {
            return List.of();
        }

        var rawRows = queryRawActivities(userId, from, to, includeMr, includeApprovals, includeComments, limit);
        if (rawRows.isEmpty()) {
            return List.of();
        }

        var mrIds = rawRows.stream().map(RawActivityRow::mergeRequestId).filter(Objects::nonNull).collect(Collectors.toSet());
        var commentIds = rawRows.stream().filter(r -> r.type() == UserActivityType.COMMENT).map(RawActivityRow::id).collect(Collectors.toSet());

        var mrDetailsMap = fetchMrDetails(mrIds);
        var commentMessages = fetchCommentMessages(commentIds);
        var userCommentsByMr = fetchUserCommentsForMrs(userId, mrIds);

        return buildActivityItems(rawRows, mrDetailsMap, commentMessages, userCommentsByMr);
    }

    private List<RawActivityRow> queryRawActivities(
            Long userId,
            LocalDate from,
            LocalDate to,
            boolean includeMr,
            boolean includeApprovals,
            boolean includeComments,
            int limit
    ) {
        var parts = new ArrayList<String>();
        if (includeMr) {
            parts.add(buildMrSubquery());
        }
        if (includeApprovals) {
            parts.add(buildApprovalsSubquery());
        }
        if (includeComments) {
            parts.add(buildCommentsSubquery());
        }

        var sql = "SELECT * FROM (" + String.join(" UNION ALL ", parts) + ") sub ORDER BY activity_time DESC LIMIT :limit";
        var params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("from", from)
                .addValue("to", to)
                .addValue("limit", limit);

        return jdbcTemplate.query(sql, params, this::mapRawRow);
    }

    private String buildMrSubquery() {
        return """
                SELECT
                    'MERGE_REQUEST' AS activity_type,
                    mr_id AS id,
                    gitlab_id AS gitlab_id,
                    mr_id AS merge_request_id,
                    gitlab_id AS mr_gitlab_id,
                    project_id,
                    project_name,
                    created_at AS activity_time,
                    contribution_date,
                    rows_added,
                    rows_deleted
                FROM mv_user_merge_requests_daily
                WHERE author_id = :userId
                  AND (:from::date IS NULL OR contribution_date >= :from)
                  AND (:to::date IS NULL OR contribution_date <= :to)
                """;
    }

    private String buildApprovalsSubquery() {
        return """
                SELECT
                    'APPROVAL' AS activity_type,
                    mr_id AS id,
                    gitlab_id AS gitlab_id,
                    mr_id AS merge_request_id,
                    gitlab_id AS mr_gitlab_id,
                    project_id,
                    project_name,
                    COALESCE(approved_at, created_at) AS activity_time,
                    approved_date AS contribution_date,
                    rows_added,
                    rows_deleted
                FROM mv_user_merge_requests_daily
                WHERE approver_id = :userId
                  AND (:from::date IS NULL OR approved_date >= :from)
                  AND (:to::date IS NULL OR approved_date <= :to)
                """;
    }

    private String buildCommentsSubquery() {
        return """
                SELECT
                    'COMMENT' AS activity_type,
                    comment_id AS id,
                    gitlab_id AS gitlab_id,
                    merge_request_id,
                    NULL::BIGINT AS mr_gitlab_id,
                    project_id,
                    project_name,
                    created_at AS activity_time,
                    contribution_date,
                    0 AS rows_added,
                    0 AS rows_deleted
                FROM mv_user_merge_request_comments_daily
                WHERE author_id = :userId
                  AND (:from::date IS NULL OR contribution_date >= :from)
                  AND (:to::date IS NULL OR contribution_date <= :to)
                """;
    }

    private RawActivityRow mapRawRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp actTs = rs.getTimestamp("activity_time");
        java.sql.Date cDate = rs.getDate("contribution_date");
        long mrGitlabIdVal = rs.getLong("mr_gitlab_id");
        Long mrGitlabId = rs.wasNull() ? null : mrGitlabIdVal;
        return new RawActivityRow(
                rs.getLong("id"),
                rs.getLong("gitlab_id"),
                UserActivityType.valueOf(rs.getString("activity_type")),
                rs.getLong("merge_request_id"),
                mrGitlabId,
                rs.getLong("project_id"),
                rs.getString("project_name"),
                actTs != null ? actTs.toInstant() : null,
                cDate != null ? cDate.toLocalDate() : null,
                rs.getInt("rows_added"),
                rs.getInt("rows_deleted")
        );
    }

    private Map<Long, MrDetails> fetchMrDetails(Set<Long> mrIds) {
        if (mrIds.isEmpty()) {
            return Map.of();
        }
        var sql = """
                SELECT id, gitlab_id, title, web_url, ready_for_review_at, review_requested_at, created_at, approved_at
                FROM merge_requests
                WHERE id IN (:mrIds)
                """;
        var params = new MapSqlParameterSource("mrIds", mrIds);
        var list = jdbcTemplate.query(sql, params, (rs, rowNum) -> new MrDetails(
                rs.getLong("id"),
                rs.getLong("gitlab_id"),
                rs.getString("title"),
                rs.getString("web_url"),
                getInstant(rs, "ready_for_review_at"),
                getInstant(rs, "review_requested_at"),
                getInstant(rs, "created_at"),
                getInstant(rs, "approved_at")
        ));
        var map = new HashMap<Long, MrDetails>();
        list.forEach(m -> map.put(m.id(), m));
        return map;
    }

    private Map<Long, String> fetchCommentMessages(Set<Long> commentIds) {
        if (commentIds.isEmpty()) {
            return Map.of();
        }
        var sql = "SELECT id, message FROM merge_request_comments WHERE id IN (:commentIds)";
        var params = new MapSqlParameterSource("commentIds", commentIds);
        var map = new HashMap<Long, String>();
        jdbcTemplate.query(sql, params, rs -> {
            map.put(rs.getLong("id"), rs.getString("message"));
        });
        return map;
    }

    private Map<Long, List<CommentRef>> fetchUserCommentsForMrs(Long userId, Set<Long> mrIds) {
        if (mrIds.isEmpty()) {
            return Map.of();
        }
        var sql = """
                SELECT id, merge_request_id, created_at
                FROM merge_request_comments
                WHERE author_id = :userId AND merge_request_id IN (:mrIds)
                ORDER BY created_at ASC
                """;
        var params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("mrIds", mrIds);

        var list = jdbcTemplate.query(sql, params, (rs, rowNum) -> new CommentRef(
                rs.getLong("id"),
                rs.getLong("merge_request_id"),
                getInstant(rs, "created_at")
        ));
        return list.stream().collect(Collectors.groupingBy(CommentRef::mergeRequestId));
    }

    private List<UserActivityItem> buildActivityItems(
            List<RawActivityRow> rawRows,
            Map<Long, MrDetails> mrDetailsMap,
            Map<Long, String> commentMessages,
            Map<Long, List<CommentRef>> userCommentsByMr
    ) {
        var items = new ArrayList<UserActivityItem>();
        for (var row : rawRows) {
            var mr = mrDetailsMap.get(row.mergeRequestId());
            var userComments = userCommentsByMr.getOrDefault(row.mergeRequestId(), List.of());
            var reactionTime = calculateReactionTime(row, mr, userComments);

            items.add(UserActivityItem.builder()
                    .id(row.id())
                    .gitlabId(row.gitlabId())
                    .type(row.type())
                    .mergeRequestId(row.mergeRequestId())
                    .mrGitlabId(mr != null ? mr.gitlabId() : row.mrGitlabId())
                    .mrTitle(mr != null ? mr.title() : null)
                    .mrWebUrl(mr != null ? mr.webUrl() : null)
                    .projectId(row.projectId())
                    .projectName(row.projectName())
                    .activityTime(row.activityTime())
                    .contributionDate(row.contributionDate())
                    .rowsAdded(row.rowsAdded())
                    .rowsDeleted(row.rowsDeleted())
                    .commentMessage(commentMessages.get(row.id()))
                    .reactionTime(reactionTime)
                    .build());
        }
        return items;
    }

    private String calculateReactionTime(RawActivityRow row, MrDetails mr, List<CommentRef> userComments) {
        if (mr == null) {
            return null;
        }
        Instant start = mr.readyForReviewAt() != null ? mr.readyForReviewAt()
                : (mr.reviewRequestedAt() != null ? mr.reviewRequestedAt() : mr.createdAt());
        if (start == null) {
            return null;
        }

        if (row.type() == UserActivityType.APPROVAL) {
            if (!userComments.isEmpty()) {
                return null;
            }
            Instant end = mr.approvedAt() != null ? mr.approvedAt() : row.activityTime();
            return computeTtfr(start, end);
        } else if (row.type() == UserActivityType.COMMENT) {
            if (userComments.isEmpty() || !Objects.equals(userComments.get(0).id(), row.id())) {
                return null;
            }
            return computeTtfr(start, row.activityTime());
        }
        return null;
    }

    private String computeTtfr(Instant start, Instant end) {
        if (end == null || !end.isAfter(start)) {
            return null;
        }
        double hours = WorkingDaysCalculator.calculateBusinessWorkingHours(start, end);
        if (hours < 0.0) {
            return null;
        }
        long totalMinutes = Math.round(hours * 60.0);
        long hh = totalMinutes / 60;
        long mm = totalMinutes % 60;
        return String.format("%02d:%02d", hh, mm);
    }

    private Instant getInstant(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts != null ? ts.toInstant() : null;
    }
}
