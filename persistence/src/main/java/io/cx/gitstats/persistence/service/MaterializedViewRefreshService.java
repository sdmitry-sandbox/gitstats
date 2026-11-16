package io.cx.gitstats.persistence.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaterializedViewRefreshService {

    private final JdbcTemplate jdbcTemplate;

    @Scheduled(fixedRate = 60000, initialDelay = 60000)
    public void refreshScheduled() {
        refreshAllViews();
    }

    public void refreshAllViews() {
        try {
            refreshView("mv_user_merge_requests_daily");
            refreshView("mv_user_merge_request_comments_daily");
            refreshView("mv_user_mr_hourly");
            log.info("Materialized views refreshed successfully");
        } catch (Exception e) {
            log.error("Failed to refresh materialized views: {}", e.getMessage(), e);
        }
    }

    private void refreshView(String viewName) {
        try {
            jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY " + viewName);
        } catch (Exception e) {
            jdbcTemplate.execute("REFRESH MATERIALIZED VIEW " + viewName);
        }
    }
}
