package io.cx.gitstats.web.ui.components.analytics;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.cx.gitstats.domain.model.analytics.ProjectOverviewAnalytics;

public class ProjectOverviewAnalyticsComponent extends VerticalLayout {

    public ProjectOverviewAnalyticsComponent(ProjectOverviewAnalytics analytics) {
        setWidthFull();
        setPadding(false);
        setSpacing(true);

        if (analytics == null) {
            return;
        }

        if (analytics.getUserStats() != null) {
            add(new ProjectUserStatsComponent(analytics.getUserStats()));
        }
    }
}
