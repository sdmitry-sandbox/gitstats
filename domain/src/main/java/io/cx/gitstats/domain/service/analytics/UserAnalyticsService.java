package io.cx.gitstats.domain.service.analytics;

import io.cx.gitstats.domain.model.analytics.UserOverviewAnalytics;

import java.time.LocalDate;

public interface UserAnalyticsService {
    UserOverviewAnalytics getUserOverviewAnalytics(Long userId, LocalDate from, LocalDate to);

    UserOverviewAnalytics getUserOverviewAnalytics(Long userId, LocalDate from, LocalDate to, boolean includeAiReview);

    UserOverviewAnalytics getUserOverviewAnalytics(String username, LocalDate from, LocalDate to);

    UserOverviewAnalytics getUserOverviewAnalytics(String username, LocalDate from, LocalDate to, boolean includeAiReview);
}
