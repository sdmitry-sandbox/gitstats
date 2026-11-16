package io.cx.gitstats.domain.task.project;

import io.cx.gitstats.domain.connector.GitlabConnector;
import io.cx.gitstats.domain.model.*;
import io.cx.gitstats.domain.persistence.MergeRequestPersistenceStorage;
import io.cx.gitstats.domain.persistence.ProjectPersistenceStorage;
import io.cx.gitstats.domain.service.ScheduledTaskProducerService;
import io.cx.gitstats.domain.task.ScheduledTask;
import io.cx.gitstats.domain.task.ScheduledTaskProcessor;
import io.cx.gitstats.domain.task.ScheduledTaskState;
import io.cx.gitstats.domain.task.ScheduledTaskType;
import io.cx.gitstats.domain.task.project.payload.ProjectFinalizeTaskSyncPayload;
import io.cx.gitstats.domain.task.project.payload.ProjectMergeRequestListUpdateTaskPayload;
import io.cx.gitstats.domain.task.project.payload.ProjectMergeRequestUpdateTaskPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.cx.gitstats.domain.utils.SerializationUtils.serializePayload;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectMergeRequestListUpdateTaskProcessor implements ScheduledTaskProcessor<ProjectMergeRequestListUpdateTaskPayload> {

    private final GitlabConnector gitlabConnector;
    private final ProjectPersistenceStorage projectPersistenceStorage;
    private final MergeRequestPersistenceStorage mergeRequestPersistenceStorage;
    private final ScheduledTaskProducerService scheduledTaskProducerService;
    private final TransactionTemplate transactionTemplate;

    @Override
    public ScheduledTaskType getTaskType() {
        return ScheduledTaskType.PROJECT_MR_LIST_UPDATE;
    }

    @Override
    public Class<ProjectMergeRequestListUpdateTaskPayload> getPayloadClass() {
        return ProjectMergeRequestListUpdateTaskPayload.class;
    }

    @Override
    public void process(ProjectMergeRequestListUpdateTaskPayload payload) {
        var projectGitlabId = payload.projectGitlabId();
        log.info("Processing ProjectMergeRequestListUpdateTask for gitlabId: {}", projectGitlabId);

        var project = projectPersistenceStorage.getByGitlabId(projectGitlabId);
        var existingByGitlabId = Optional.ofNullable(project.getMergeRequests())
                .orElseGet(Collections::emptyList)
                .stream()
                .collect(Collectors.toMap(MergeRequest::getGitlabId, Function.identity(), (a, b) -> a));

        var apiMrs = gitlabConnector.getMergeRequests(projectGitlabId);
        var updates = getUpdates(project, existingByGitlabId, apiMrs);

        if (updates.isEmpty()) {
            markProjectSynced(project);
            log.info("No merge request changes for gitlabId: {}. Project status updated to SYNCED", projectGitlabId);
            return;
        }

        persistChanges(project, updates);
        log.info(
                "Updated merge requests for gitlabId: {}. New: {}, updated: {}",
                projectGitlabId, updates.newMrs().size(), updates.updatedMrs().size()
        );
    }

    private MergeRequestChanges getUpdates(Project project,
                                           Map<Long, MergeRequest> existingByGitlabId,
                                           List<MergeRequest> apiMrs) {
        var newMrs = new ArrayList<MergeRequest>();
        var updatedMrs = new ArrayList<MergeRequest>();

        for (var apiMr : apiMrs) {
            var existingMr = existingByGitlabId.get(apiMr.getGitlabId());
            if (existingMr == null) {
                newMrs.add(buildNewMergeRequest(project, apiMr));
            } else if (existingMr.getGitlabState() != apiMr.getGitlabState()) {
                updatedMrs.add(applyUpdates(existingMr, apiMr));
            }
        }
        return new MergeRequestChanges(newMrs, updatedMrs);
    }

    private MergeRequest buildNewMergeRequest(Project project, MergeRequest apiMr) {
        var gitlabState = apiMr.getGitlabState();
        return MergeRequest.builder()
                .gitlabId(apiMr.getGitlabId())
                .projectId(project.getId())
                .title(apiMr.getTitle())
                .gitlabState(gitlabState)
                .status(MergeRequestGitlabState.MERGED == gitlabState ? MergeRequestStatus.SYNCING : MergeRequestStatus.SYNCED)
                .webUrl(apiMr.getWebUrl())
                .createdAt(apiMr.getCreatedAt() != null ? apiMr.getCreatedAt() : Instant.now())
                .build();
    }

    private MergeRequest applyUpdates(MergeRequest existingMr, MergeRequest apiMr) {
        var gitlabState = apiMr.getGitlabState();
        existingMr.setGitlabState(gitlabState);
        existingMr.setTitle(apiMr.getTitle());
        existingMr.setWebUrl(apiMr.getWebUrl());
        existingMr.setStatus(MergeRequestGitlabState.MERGED == gitlabState ? MergeRequestStatus.SYNCING : MergeRequestStatus.SYNCED);
        return existingMr;
    }

    private void markProjectSynced(Project project) {
        var now = Instant.now();
        project.setStatus(ProjectStatus.SYNCED);
        project.setUpdatedAt(now);
        project.setLastSyncedAt(now);
        projectPersistenceStorage.save(project);
    }

    private void persistChanges(Project project, MergeRequestChanges changes) {
        transactionTemplate.executeWithoutResult(status -> {
            if (!changes.newMrs().isEmpty()) {
                mergeRequestPersistenceStorage.saveAll(changes.newMrs());
            }
            if (!changes.updatedMrs().isEmpty()) {
                mergeRequestPersistenceStorage.updateAll(changes.updatedMrs());
            }
            var tasksToProduce = buildMrUpdateTasks(project.getGitlabId(), changes);
            if (!tasksToProduce.isEmpty()) {
                var allTasks = new ArrayList<>(tasksToProduce);
                allTasks.add(buildFinalizeSyncScheduledTask(project.getGitlabId()));
                scheduledTaskProducerService.produceTasks(allTasks);
            } else {
                markProjectSynced(project);
            }
        });
    }

    private ScheduledTask buildFinalizeSyncScheduledTask(Long projectGitlabId) {
        var now = Instant.now();
        var payload = new ProjectFinalizeTaskSyncPayload(projectGitlabId);
        return ScheduledTask.builder()
                .taskType(ScheduledTaskType.FINALIZE_TASK_SYNC)
                .idempotencyKey(ScheduledTaskType.FINALIZE_TASK_SYNC + "_" + projectGitlabId)
                .payload(serializePayload(payload))
                .state(ScheduledTaskState.SCHEDULED)
                .scheduledAt(now.plus(Duration.ofSeconds(30)))
                .retries(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private List<ScheduledTask> buildMrUpdateTasks(Long projectGitlabId, MergeRequestChanges changes) {
        return Stream.concat(changes.newMrs().stream(), changes.updatedMrs().stream())
                .filter(mr -> mr.getGitlabState() == MergeRequestGitlabState.MERGED)
                .map(mr -> buildMrUpdateScheduledTask(projectGitlabId, mr.getGitlabId()))
                .toList();
    }

    private ScheduledTask buildMrUpdateScheduledTask(Long projectGitlabId, Long mrGitlabId) {
        var now = Instant.now();
        var payload = new ProjectMergeRequestUpdateTaskPayload(projectGitlabId, mrGitlabId);
        return ScheduledTask.builder()
                .taskType(ScheduledTaskType.PROJECT_MR_UPDATE)
                .idempotencyKey(ScheduledTaskType.PROJECT_MR_UPDATE + "_" + projectGitlabId + "_" + mrGitlabId)
                .payload(serializePayload(payload))
                .state(ScheduledTaskState.SCHEDULED)
                .scheduledAt(now)
                .retries(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private record MergeRequestChanges(List<MergeRequest> newMrs, List<MergeRequest> updatedMrs) {
        boolean isEmpty() {
            return newMrs.isEmpty() && updatedMrs.isEmpty();
        }
    }
}