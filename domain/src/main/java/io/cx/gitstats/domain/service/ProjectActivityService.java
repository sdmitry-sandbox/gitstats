package io.cx.gitstats.domain.service;

import io.cx.gitstats.domain.model.activity.UserActivityItem;

import java.time.LocalDate;
import java.util.List;

public interface ProjectActivityService {
    List<UserActivityItem> getProjectActivities(
            Long projectId,
            LocalDate from,
            LocalDate to,
            boolean includeMr,
            boolean includeApprovals,
            boolean includeComments
    );

    List<UserActivityItem> getProjectActivities(
            Long projectId,
            LocalDate from,
            LocalDate to,
            boolean includeMr,
            boolean includeApprovals,
            boolean includeComments,
            int limit
    );
}
