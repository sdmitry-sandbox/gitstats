package io.cx.gitstats.domain.task.project;

import io.cx.gitstats.domain.model.MergeRequestStatus;
import io.cx.gitstats.domain.model.ProjectStatus;
import io.cx.gitstats.domain.persistence.MergeRequestPersistenceStorage;
import io.cx.gitstats.domain.persistence.ProjectPersistenceStorage;
import io.cx.gitstats.domain.task.ScheduledTaskProcessor;
import io.cx.gitstats.domain.task.ScheduledTaskType;
import io.cx.gitstats.domain.task.project.payload.ProjectFinalizeTaskSyncPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectFinalizeTaskSyncProcessor implements ScheduledTaskProcessor<ProjectFinalizeTaskSyncPayload> {

    private final ProjectPersistenceStorage projectPersistenceStorage;
    private final MergeRequestPersistenceStorage mergeRequestPersistenceStorage;

    @Override
    public ScheduledTaskType getTaskType() {
        return ScheduledTaskType.FINALIZE_TASK_SYNC;
    }

    @Override
    public Class<ProjectFinalizeTaskSyncPayload> getPayloadClass() {
        return ProjectFinalizeTaskSyncPayload.class;
    }

    @Override
    public void process(ProjectFinalizeTaskSyncPayload payload) {
        var projectGitlabId = payload.projectGitlabId();
        log.info("Processing FinalizeTaskSync for gitlabId: {}", projectGitlabId);

        var project = projectPersistenceStorage.getByGitlabId(projectGitlabId);
        if (project.getStatus() != ProjectStatus.PENDING_MR_SYNC) {
            throw new IllegalStateException("Expected project status PENDING_MR_SYNC, but was: " + project.getStatus());
        }

        var syncingMrsCount = mergeRequestPersistenceStorage.countByProjectIdAndStatus(project.getId(), MergeRequestStatus.SYNCING);
        if (syncingMrsCount == 0) {
            var now = Instant.now();
            project.setStatus(ProjectStatus.SYNCED);
            project.setUpdatedAt(now);
            project.setLastSyncedAt(now);
            projectPersistenceStorage.save(project);
            log.info("Finalized sync for gitlabId: {}. Project status updated to SYNCED", projectGitlabId);
        } else {
            throw new IllegalStateException("Project %s still has %d merge requests in SYNCING status".formatted(projectGitlabId, syncingMrsCount));
        }
    }
}
