package io.cx.gitstats.domain.persistence;

import io.cx.gitstats.domain.model.activity.UserActivityItem;

import java.time.LocalDate;
import java.util.List;

public interface ProjectActivityPersistenceStorage {
    List<UserActivityItem> findProjectActivities(
            Long projectId,
            LocalDate from,
            LocalDate to,
            boolean includeMr,
            boolean includeApprovals,
            boolean includeComments,
            int limit
    );
}
