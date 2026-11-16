package io.cx.gitstats.web.ui.components.analytics;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.cx.gitstats.domain.model.analytics.UserOverviewAnalytics;

public class UserOverviewAnalyticsComponent extends VerticalLayout {

    public UserOverviewAnalyticsComponent(UserOverviewAnalytics analytics) {
        this(analytics, false, null);
    }

    public UserOverviewAnalyticsComponent(
            UserOverviewAnalytics analytics,
            boolean includeAiReview,
            GeneralStatsComponent.AiReviewChangeListener aiReviewChangeListener
    ) {
        setWidthFull();
        setPadding(false);
        setSpacing(true);

        if (analytics == null) {
            return;
        }

        // 2.1 General Statistics
        add(new GeneralStatsComponent(analytics.getGeneralStats(), includeAiReview, aiReviewChangeListener));

        // 2.2 Code Contribution Statistics
        add(new CodeContributionStatsComponent(analytics.getCodeContributionStats()));

        // 2.3 Code Review Statistics
        add(new CodeReviewStatsComponent(analytics.getCodeReviewStats()));

        // 2.4 Project Statistics
        add(new ProjectStatsComponent(analytics.getProjectStats()));

        // 2.5 Activity Statistics
        add(new ActivityStatsComponent(analytics.getActivityStats()));
    }
}
