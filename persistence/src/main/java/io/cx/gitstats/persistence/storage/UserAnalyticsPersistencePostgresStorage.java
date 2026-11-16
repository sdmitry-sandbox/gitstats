package io.cx.gitstats.persistence.storage;

import io.cx.gitstats.domain.model.analytics.UserCommentDailyViewRow;
import io.cx.gitstats.domain.model.analytics.UserMergeRequestDailyViewRow;
import io.cx.gitstats.domain.model.analytics.UserMrHourlyViewRow;
import io.cx.gitstats.domain.persistence.UserAnalyticsPersistenceStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAnalyticsPersistencePostgresStorage implements UserAnalyticsPersistenceStorage {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final String SELECT_MERGE_REQUESTS_SQL = """
            SELECT
                mr_id, gitlab_id, project_id, project_name,
                author_id, author_username, author_name, author_is_bot,
                approver_id, approver_username, approver_name, approver_is_bot,
                rows_added, rows_deleted, total_code,
                created_at, merged_at, approved_at,
                ready_for_review_at, review_requested_at, review_finished_at,
                contribution_date, approved_date
            FROM mv_user_merge_requests_daily
            """;

    private static final String SELECT_COMMENTS_SQL = """
            SELECT
                comment_id, gitlab_id, merge_request_id, project_id, project_name,
                author_id, author_username, author_name, author_is_bot,
                mr_author_id, mr_author_username, mr_author_name, mr_author_is_bot,
                created_at, contribution_date
            FROM mv_user_merge_request_comments_daily
            """;

    private static final String SELECT_HOURLY_SQL = """
            SELECT
                user_id, username, is_bot, contribution_date, day_of_week, hour_of_day, mr_count
            FROM mv_user_mr_hourly
            WHERE user_id = :userId
            """;

    @Override
    @Transactional(readOnly = true)
    public List<UserMergeRequestDailyViewRow> findAllMergeRequests() {
        return jdbcTemplate.query(SELECT_MERGE_REQUESTS_SQL, this::mapMergeRequestRow);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserCommentDailyViewRow> findAllComments() {
        return jdbcTemplate.query(SELECT_COMMENTS_SQL, this::mapCommentRow);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserMrHourlyViewRow> findHourlyData(Long userId) {
        var params = new MapSqlParameterSource("userId", userId);
        return jdbcTemplate.query(SELECT_HOURLY_SQL, params, this::mapHourlyRow);
    }

    private UserMergeRequestDailyViewRow mapMergeRequestRow(ResultSet rs, int rowNum) throws SQLException {
        Date contribDate = rs.getDate("contribution_date");
        Date appDate = rs.getDate("approved_date");
        return UserMergeRequestDailyViewRow.builder()
                .mrId(rs.getObject("mr_id", Long.class))
                .gitlabId(rs.getObject("gitlab_id", Long.class))
                .projectId(rs.getObject("project_id", Long.class))
                .projectName(rs.getString("project_name"))
                .authorId(rs.getObject("author_id", Long.class))
                .authorUsername(rs.getString("author_username"))
                .authorName(rs.getString("author_name"))
                .authorIsBot(rs.getObject("author_is_bot", Boolean.class))
                .approverId(rs.getObject("approver_id", Long.class))
                .approverUsername(rs.getString("approver_username"))
                .approverName(rs.getString("approver_name"))
                .approverIsBot(rs.getObject("approver_is_bot", Boolean.class))
                .rowsAdded(rs.getObject("rows_added", Integer.class))
                .rowsDeleted(rs.getObject("rows_deleted", Integer.class))
                .totalCode(rs.getObject("total_code", Integer.class))
                .createdAt(toInstant(rs.getTimestamp("created_at")))
                .mergedAt(toInstant(rs.getTimestamp("merged_at")))
                .approvedAt(toInstant(rs.getTimestamp("approved_at")))
                .readyForReviewAt(toInstant(rs.getTimestamp("ready_for_review_at")))
                .reviewRequestedAt(toInstant(rs.getTimestamp("review_requested_at")))
                .reviewFinishedAt(toInstant(rs.getTimestamp("review_finished_at")))
                .contributionDate(contribDate != null ? contribDate.toLocalDate() : null)
                .approvedDate(appDate != null ? appDate.toLocalDate() : null)
                .build();
    }

    private UserCommentDailyViewRow mapCommentRow(ResultSet rs, int rowNum) throws SQLException {
        Date contribDate = rs.getDate("contribution_date");
        return UserCommentDailyViewRow.builder()
                .commentId(rs.getObject("comment_id", Long.class))
                .gitlabId(rs.getObject("gitlab_id", Long.class))
                .mergeRequestId(rs.getObject("merge_request_id", Long.class))
                .projectId(rs.getObject("project_id", Long.class))
                .projectName(rs.getString("project_name"))
                .authorId(rs.getObject("author_id", Long.class))
                .authorUsername(rs.getString("author_username"))
                .authorName(rs.getString("author_name"))
                .authorIsBot(rs.getObject("author_is_bot", Boolean.class))
                .mrAuthorId(rs.getObject("mr_author_id", Long.class))
                .mrAuthorUsername(rs.getString("mr_author_username"))
                .mrAuthorName(rs.getString("mr_author_name"))
                .mrAuthorIsBot(rs.getObject("mr_author_is_bot", Boolean.class))
                .createdAt(toInstant(rs.getTimestamp("created_at")))
                .contributionDate(contribDate != null ? contribDate.toLocalDate() : null)
                .build();
    }

    private UserMrHourlyViewRow mapHourlyRow(ResultSet rs, int rowNum) throws SQLException {
        Date contribDate = rs.getDate("contribution_date");
        return UserMrHourlyViewRow.builder()
                .userId(rs.getObject("user_id", Long.class))
                .username(rs.getString("username"))
                .isBot(rs.getObject("is_bot", Boolean.class))
                .contributionDate(contribDate != null ? contribDate.toLocalDate() : null)
                .dayOfWeek(rs.getObject("day_of_week", Integer.class))
                .hourOfDay(rs.getObject("hour_of_day", Integer.class))
                .mrCount(rs.getObject("mr_count", Long.class))
                .build();
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant() : null;
    }
}
