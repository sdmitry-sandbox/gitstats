package io.cx.gitstats.domain.service;

import io.cx.gitstats.domain.model.activity.UserActivityItem;
import io.cx.gitstats.domain.persistence.ProjectActivityPersistenceStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectActivityServiceImpl implements ProjectActivityService {

    private static final int DEFAULT_LIMIT = 100;

    private final ProjectActivityPersistenceStorage projectActivityPersistenceStorage;

    @Override
    public List<UserActivityItem> getProjectActivities(
            Long projectId,
            LocalDate from,
            LocalDate to,
            boolean includeMr,
            boolean includeApprovals,
            boolean includeComments
    ) {
        return getProjectActivities(projectId, from, to, includeMr, includeApprovals, includeComments, DEFAULT_LIMIT);
    }

    @Override
    public List<UserActivityItem> getProjectActivities(
            Long projectId,
            LocalDate from,
            LocalDate to,
            boolean includeMr,
            boolean includeApprovals,
            boolean includeComments,
            int limit
    ) {
        if (projectId == null) {
            return List.of();
        }
        return projectActivityPersistenceStorage.findProjectActivities(
                projectId, from, to, includeMr, includeApprovals, includeComments, limit
        );
    }
}
