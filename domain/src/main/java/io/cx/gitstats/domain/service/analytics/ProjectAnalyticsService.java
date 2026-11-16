package io.cx.gitstats.domain.service.analytics;

import io.cx.gitstats.domain.model.analytics.ProjectOverviewAnalytics;

import java.time.LocalDate;

public interface ProjectAnalyticsService {
    ProjectOverviewAnalytics getProjectOverviewAnalytics(Long projectId, LocalDate from, LocalDate to);
}
