package io.cx.gitstats.persistence.storage;

import io.cx.gitstats.domain.model.DailyContribution;
import io.cx.gitstats.domain.persistence.ProjectContributionPersistenceStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectContributionPersistencePostgresStorage implements ProjectContributionPersistenceStorage {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final String SELECT_AUTHOR_MRS = """
            SELECT
                contribution_date,
                COUNT(*) AS mr_count
            FROM mv_user_merge_requests_daily
            WHERE project_id = :projectId
              AND author_id IS NOT NULL
              AND author_is_bot = false
            GROUP BY contribution_date
            """;

    private static final String SELECT_APPROVER_MRS = """
            SELECT
                approved_date AS contribution_date,
                COUNT(*) AS approval_count
            FROM mv_user_merge_requests_daily
            WHERE project_id = :projectId
              AND approver_id IS NOT NULL
              AND approver_is_bot = false
            GROUP BY approved_date
            """;

    private static final String SELECT_COMMENTS = """
            SELECT
                contribution_date,
                COUNT(*) AS comments_count
            FROM mv_user_merge_request_comments_daily
            WHERE project_id = :projectId
              AND author_id IS NOT NULL
              AND author_is_bot = false
            GROUP BY contribution_date
            """;

    private static class ContributionAccumulator {
        int mrCount;
        int approvalCount;
        int commentCount;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyContribution> findDailyContributionsByProjectId(Long projectId) {
        var params = new MapSqlParameterSource("projectId", projectId);
        var map = new TreeMap<LocalDate, ContributionAccumulator>();

        fetchMrCounts(params, map);
        fetchApprovalCounts(params, map);
        fetchCommentCounts(params, map);

        return buildResultList(map);
    }

    private void fetchMrCounts(MapSqlParameterSource params, Map<LocalDate, ContributionAccumulator> map) {
        jdbcTemplate.query(SELECT_AUTHOR_MRS, params, rs -> {
            Date sqlDate = rs.getDate("contribution_date");
            if (sqlDate != null) {
                var date = sqlDate.toLocalDate();
                var acc = map.computeIfAbsent(date, d -> new ContributionAccumulator());
                acc.mrCount += rs.getInt("mr_count");
            }
        });
    }

    private void fetchApprovalCounts(MapSqlParameterSource params, Map<LocalDate, ContributionAccumulator> map) {
        jdbcTemplate.query(SELECT_APPROVER_MRS, params, rs -> {
            Date sqlDate = rs.getDate("contribution_date");
            if (sqlDate != null) {
                var date = sqlDate.toLocalDate();
                var acc = map.computeIfAbsent(date, d -> new ContributionAccumulator());
                acc.approvalCount += rs.getInt("approval_count");
            }
        });
    }

    private void fetchCommentCounts(MapSqlParameterSource params, Map<LocalDate, ContributionAccumulator> map) {
        jdbcTemplate.query(SELECT_COMMENTS, params, rs -> {
            Date sqlDate = rs.getDate("contribution_date");
            if (sqlDate != null) {
                var date = sqlDate.toLocalDate();
                var acc = map.computeIfAbsent(date, d -> new ContributionAccumulator());
                acc.commentCount += rs.getInt("comments_count");
            }
        });
    }

    private List<DailyContribution> buildResultList(Map<LocalDate, ContributionAccumulator> map) {
        var result = new ArrayList<DailyContribution>(map.size());
        for (var entry : map.entrySet()) {
            var acc = entry.getValue();
            result.add(DailyContribution.builder()
                    .date(entry.getKey())
                    .mrCount(acc.mrCount)
                    .approvalCount(acc.approvalCount)
                    .commentCount(acc.commentCount)
                    .build());
        }
        return result;
    }
}
