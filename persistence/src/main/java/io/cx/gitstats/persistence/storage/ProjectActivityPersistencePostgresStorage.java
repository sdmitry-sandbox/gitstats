package io.cx.gitstats.persistence.storage;

import io.cx.gitstats.domain.model.activity.UserActivityItem;
import io.cx.gitstats.domain.model.activity.UserActivityType;
import io.cx.gitstats.domain.persistence.ProjectActivityPersistenceStorage;
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
public class ProjectActivityPersistencePostgresStorage implements ProjectActivityPersistenceStorage {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private record RawActivityRow(
            Long id,
            Long gitlabId,
            UserActivityType type,
            Long mergeRequestId,
            Long mrGitlabId,
            Long projectId,
            String projectName,
            Long userId,
            String username,
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
            Long authorId,
            Instant createdAt
    ) {}

    @Override
    @Transactional(readOnly = true)
    public List<UserActivityItem> findProjectActivities(
            Long projectId,
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
        if (limit <= 0) {
            return List.of();
        }

        var rawRows = queryRawActivities(projectId, from, to, includeMr, includeApprovals, includeComments, limit);
        if (rawRows.isEmpty()) {
            return List.of();
        }

        var mrIds = rawRows.stream()
                .map(RawActivityRow::mergeRequestId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        var commentIds = rawRows.stream()
                .filter(r -> r.type() == UserActivityType.COMMENT)
                .map(RawActivityRow::id)
                .collect(Collectors.toSet());

        var mrDetailsMap = fetchMrDetails(mrIds);
        var commentMessages = fetchCommentMessages(commentIds);
        var commentsByMrAndUser = fetchCommentsForMrs(mrIds);

        return buildActivityItems(rawRows, mrDetailsMap, commentMessages, commentsByMrAndUser);
    }

    private List<RawActivityRow> queryRawActivities(
            Long projectId,
            LocalDate from,
            LocalDate to,
            boolean includeMr,
            boolean includeApprovals,
            boolean includeComments,
            int limit
    ) {
        var params = new MapSqlParameterSource()
                .addValue("projectId", projectId)
                .addValue("limit", limit);

        if (from != null) {
            params.addValue("from", from);
        }
        if (to != null) {
            params.addValue("to", to);
        }

        var allRows = new ArrayList<RawActivityRow>();
        if (includeMr) {
            allRows.addAll(jdbcTemplate.query(buildMrSubquery(from != null, to != null), params, this::mapRawRow));
        }
        if (includeApprovals) {
            allRows.addAll(jdbcTemplate.query(buildApprovalsSubquery(from != null, to != null), params, this::mapRawRow));
        }
        if (includeComments) {
            allRows.addAll(jdbcTemplate.query(buildCommentsSubquery(from != null, to != null), params, this::mapRawRow));
        }

        return allRows.stream()
                .sorted((a, b) -> {
                    if (a.activityTime() == null && b.activityTime() == null) return 0;
                    if (a.activityTime() == null) return 1;
                    if (b.activityTime() == null) return -1;
                    return b.activityTime().compareTo(a.activityTime());
                })
                .limit(limit)
                .toList();
    }

    private String buildMrSubquery(boolean hasFrom, boolean hasTo) {
        var sb = new StringBuilder("""
                SELECT
                    'MERGE_REQUEST' AS activity_type,
                    mr_id AS id,
                    gitlab_id AS gitlab_id,
                    mr_id AS merge_request_id,
                    gitlab_id AS mr_gitlab_id,
                    project_id,
                    project_name,
                    author_id AS user_id,
                    author_username AS username,
                    created_at AS activity_time,
                    contribution_date,
                    rows_added,
                    rows_deleted
                FROM mv_user_merge_requests_daily
                WHERE project_id = :projectId
                  AND author_id IS NOT NULL
                  AND author_is_bot = false
                """);
        if (hasFrom) {
            sb.append(" AND contribution_date >= :from");
        }
        if (hasTo) {
            sb.append(" AND contribution_date <= :to");
        }
        sb.append(" ORDER BY created_at DESC LIMIT :limit");
        return sb.toString();
    }

    private String buildApprovalsSubquery(boolean hasFrom, boolean hasTo) {
        var sb = new StringBuilder("""
                SELECT
                    'APPROVAL' AS activity_type,
                    mr_id AS id,
                    gitlab_id AS gitlab_id,
                    mr_id AS merge_request_id,
                    gitlab_id AS mr_gitlab_id,
                    project_id,
                    project_name,
                    approver_id AS user_id,
                    approver_username AS username,
                    COALESCE(approved_at, created_at) AS activity_time,
                    approved_date AS contribution_date,
                    rows_added,
                    rows_deleted
                FROM mv_user_merge_requests_daily
                WHERE project_id = :projectId
                  AND approver_id IS NOT NULL
                  AND approver_is_bot = false
                """);
        if (hasFrom) {
            sb.append(" AND approved_date >= :from");
        }
        if (hasTo) {
            sb.append(" AND approved_date <= :to");
        }
        sb.append(" ORDER BY COALESCE(approved_at, created_at) DESC LIMIT :limit");
        return sb.toString();
    }

    private String buildCommentsSubquery(boolean hasFrom, boolean hasTo) {
        var sb = new StringBuilder("""
                SELECT
                    'COMMENT' AS activity_type,
                    comment_id AS id,
                    gitlab_id AS gitlab_id,
                    merge_request_id,
                    NULL::BIGINT AS mr_gitlab_id,
                    project_id,
                    project_name,
                    author_id AS user_id,
                    author_username AS username,
                    created_at AS activity_time,
                    contribution_date,
                    0 AS rows_added,
                    0 AS rows_deleted
                FROM mv_user_merge_request_comments_daily
                WHERE project_id = :projectId
                  AND author_id IS NOT NULL
                  AND author_is_bot = false
                """);
        if (hasFrom) {
            sb.append(" AND contribution_date >= :from");
        }
        if (hasTo) {
            sb.append(" AND contribution_date <= :to");
        }
        sb.append(" ORDER BY created_at DESC LIMIT :limit");
        return sb.toString();
    }

    private RawActivityRow mapRawRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp actTs = rs.getTimestamp("activity_time");
        java.sql.Date cDate = rs.getDate("contribution_date");
        long mrGitlabIdVal = rs.getLong("mr_gitlab_id");
        Long mrGitlabId = rs.wasNull() ? null : mrGitlabIdVal;
        long userIdVal = rs.getLong("user_id");
        Long userId = rs.wasNull() ? null : userIdVal;
        return new RawActivityRow(
                rs.getLong("id"),
                rs.getLong("gitlab_id"),
                UserActivityType.valueOf(rs.getString("activity_type")),
                rs.getLong("merge_request_id"),
                mrGitlabId,
                rs.getLong("project_id"),
                rs.getString("project_name"),
                userId,
                rs.getString("username"),
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

    private Map<Long, Map<Long, List<CommentRef>>> fetchCommentsForMrs(Set<Long> mrIds) {
        if (mrIds.isEmpty()) {
            return Map.of();
        }
        var sql = """
                SELECT comment_id AS id, merge_request_id, author_id, created_at
                FROM mv_user_merge_request_comments_daily
                WHERE merge_request_id IN (:mrIds)
                  AND author_is_bot = false
                ORDER BY created_at ASC
                """;
        var params = new MapSqlParameterSource("mrIds", mrIds);
        var list = jdbcTemplate.query(sql, params, (rs, rowNum) -> new CommentRef(
                rs.getLong("id"),
                rs.getLong("merge_request_id"),
                rs.getLong("author_id"),
                getInstant(rs, "created_at")
        ));
        return list.stream().collect(
                Collectors.groupingBy(
                        CommentRef::mergeRequestId,
                        Collectors.groupingBy(CommentRef::authorId)
                )
        );
    }

    private List<UserActivityItem> buildActivityItems(
            List<RawActivityRow> rawRows,
            Map<Long, MrDetails> mrDetailsMap,
            Map<Long, String> commentMessages,
            Map<Long, Map<Long, List<CommentRef>>> commentsByMrAndUser
    ) {
        var items = new ArrayList<UserActivityItem>();
        for (var row : rawRows) {
            var mr = mrDetailsMap.get(row.mergeRequestId());
            var userComments = commentsByMrAndUser.getOrDefault(row.mergeRequestId(), Map.of())
                    .getOrDefault(row.userId(), List.of());
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
                    .userId(row.userId())
                    .username(row.username())
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
        return formatHours(hours);
    }

    private String formatHours(double hours) {
        if (hours < 1.0) {
            long mins = Math.round(hours * 60.0);
            return mins <= 0 ? "< 1m" : mins + "m";
        }
        long h = (long) Math.floor(hours);
        long m = Math.round((hours - h) * 60.0);
        return m > 0 ? h + "h " + m + "m" : h + "h";
    }

    private Instant getInstant(ResultSet rs, String col) throws SQLException {
        Timestamp ts = rs.getTimestamp(col);
        return ts != null ? ts.toInstant() : null;
    }
}