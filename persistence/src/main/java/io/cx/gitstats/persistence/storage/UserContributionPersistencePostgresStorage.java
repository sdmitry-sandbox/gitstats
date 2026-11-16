package io.cx.gitstats.persistence.storage;

import io.cx.gitstats.domain.model.DailyContribution;
import io.cx.gitstats.domain.persistence.UserContributionPersistenceStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserContributionPersistencePostgresStorage implements UserContributionPersistenceStorage {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final String SELECT_BASE = """
            WITH user_daily_mrs AS (
                SELECT
                    author_id AS user_id,
                    author_username AS username,
                    author_is_bot AS is_bot,
                    contribution_date,
                    COUNT(*) AS mr_count,
                    0::BIGINT AS approval_count
                FROM mv_user_merge_requests_daily
                WHERE author_id IS NOT NULL
                GROUP BY author_id, author_username, author_is_bot, contribution_date
                UNION ALL
                SELECT
                    approver_id AS user_id,
                    approver_username AS username,
                    approver_is_bot AS is_bot,
                    approved_date AS contribution_date,
                    0::BIGINT AS mr_count,
                    COUNT(*) AS approval_count
                FROM mv_user_merge_requests_daily
                WHERE approver_id IS NOT NULL
                GROUP BY approver_id, approver_username, approver_is_bot, approved_date
            ),
            user_mrs_aggregated AS (
                SELECT
                    user_id,
                    username,
                    is_bot,
                    contribution_date,
                    SUM(mr_count)::BIGINT AS mr_count,
                    SUM(approval_count)::BIGINT AS approval_count
                FROM user_daily_mrs
                GROUP BY user_id, username, is_bot, contribution_date
            ),
            user_comments_aggregated AS (
                SELECT
                    author_id AS user_id,
                    author_username AS username,
                    author_is_bot AS is_bot,
                    contribution_date,
                    COUNT(*)::BIGINT AS comments_count
                FROM mv_user_merge_request_comments_daily
                WHERE author_id IS NOT NULL
                GROUP BY author_id, author_username, author_is_bot, contribution_date
            )
            SELECT
                COALESCE(mr.user_id, c.user_id) AS user_id,
                COALESCE(mr.username, c.username) AS username,
                COALESCE(mr.is_bot, c.is_bot) AS is_bot,
                COALESCE(mr.contribution_date, c.contribution_date) AS contribution_date,
                COALESCE(mr.mr_count, 0) AS mr_count,
                COALESCE(mr.approval_count, 0) AS approval_count,
                COALESCE(c.comments_count, 0) AS comments_count
            FROM user_mrs_aggregated mr
            FULL OUTER JOIN user_comments_aggregated c
                ON mr.user_id = c.user_id AND mr.contribution_date = c.contribution_date
            """;

    private final RowMapper<DailyContribution> rowMapper = (rs, rowNum) -> {
        Date sqlDate = rs.getDate("contribution_date");
        return DailyContribution.builder()
                .userId(rs.getLong("user_id"))
                .username(rs.getString("username"))
                .isBot(rs.getBoolean("is_bot"))
                .date(sqlDate != null ? sqlDate.toLocalDate() : null)
                .mrCount(rs.getInt("mr_count"))
                .approvalCount(rs.getInt("approval_count"))
                .commentCount(rs.getInt("comments_count"))
                .build();
    };

    @Override
    @Transactional(readOnly = true)
    public List<DailyContribution> findDailyContributionsByUserId(Long userId) {
        String sql = SELECT_BASE + " WHERE COALESCE(mr.user_id, c.user_id) = :userId ORDER BY contribution_date ASC";
        var params = new MapSqlParameterSource("userId", userId);
        return jdbcTemplate.query(sql, params, rowMapper);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyContribution> findDailyContributionsByUsername(String username) {
        String sql = SELECT_BASE + " WHERE COALESCE(mr.username, c.username) = :username ORDER BY contribution_date ASC";
        var params = new MapSqlParameterSource("username", username);
        return jdbcTemplate.query(sql, params, rowMapper);
    }

    @Override
    @Transactional(readOnly = true)
    public int findMaxDailyContribution(boolean includeMr, boolean includeApprovals, boolean includeComments, boolean isBot, Long userId) {
        String filter = isBot
                ? "WHERE t.user_id = :userId"
                : "WHERE t.is_bot = false";

        String sql = """
                SELECT COALESCE(MAX(
                    (CASE WHEN :includeMr THEN COALESCE(t.mr_count, 0) ELSE 0 END) +
                    (CASE WHEN :includeApprovals THEN COALESCE(t.approval_count, 0) ELSE 0 END) +
                    (CASE WHEN :includeComments THEN COALESCE(t.comments_count, 0) ELSE 0 END)
                ), 0) AS max_daily
                FROM (
                """ + SELECT_BASE + """
                ) t
                """ + " " + filter;

        var params = new MapSqlParameterSource()
                .addValue("includeMr", includeMr)
                .addValue("includeApprovals", includeApprovals)
                .addValue("includeComments", includeComments)
                .addValue("userId", userId);

        Integer maxVal = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return maxVal != null ? maxVal : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> findDailyContributionCounts(boolean includeMr, boolean includeApprovals, boolean includeComments, boolean isBot, Long userId) {
        String filter = isBot
                ? "WHERE t.user_id = :userId"
                : "WHERE t.is_bot = false";

        String sql = """
                SELECT (
                    (CASE WHEN :includeMr THEN COALESCE(t.mr_count, 0) ELSE 0 END) +
                    (CASE WHEN :includeApprovals THEN COALESCE(t.approval_count, 0) ELSE 0 END) +
                    (CASE WHEN :includeComments THEN COALESCE(t.comments_count, 0) ELSE 0 END)
                ) AS daily_count
                FROM (
                """ + SELECT_BASE + """
                ) t
                """ + " " + filter;

        var params = new MapSqlParameterSource()
                .addValue("includeMr", includeMr)
                .addValue("includeApprovals", includeApprovals)
                .addValue("includeComments", includeComments)
                .addValue("userId", userId);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> rs.getInt("daily_count"));
    }
}
