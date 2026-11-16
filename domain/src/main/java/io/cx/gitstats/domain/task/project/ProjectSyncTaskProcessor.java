package io.cx.gitstats.domain.task.project;

import io.cx.gitstats.domain.connector.GitlabConnector;
import io.cx.gitstats.domain.model.Project;
import io.cx.gitstats.domain.model.ProjectStatus;
import io.cx.gitstats.domain.persistence.ProjectPersistenceStorage;
import io.cx.gitstats.domain.service.ScheduledTaskProducerService;
import io.cx.gitstats.domain.task.ScheduledTask;
import io.cx.gitstats.domain.task.ScheduledTaskProcessor;
import io.cx.gitstats.domain.task.ScheduledTaskState;
import io.cx.gitstats.domain.task.ScheduledTaskType;
import io.cx.gitstats.domain.task.project.payload.ProjectMergeRequestListUpdateTaskPayload;
import io.cx.gitstats.domain.task.project.payload.ProjectSyncTaskPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

import static io.cx.gitstats.domain.utils.SerializationUtils.serializePayload;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectSyncTaskProcessor implements ScheduledTaskProcessor<ProjectSyncTaskPayload> {
    private final GitlabConnector gitlabConnector;
    private final ProjectPersistenceStorage projectPersistenceStorage;
    private final TransactionTemplate transactionTemplate;
    private final ScheduledTaskProducerService scheduledTaskProducerService;

    @Override
    public ScheduledTaskType getTaskType() {
        return ScheduledTaskType.PROJECT_SYNC_START;
    }

    @Override
    public Class<ProjectSyncTaskPayload> getPayloadClass() {
        return ProjectSyncTaskPayload.class;
    }

    @Override
    public void process(ProjectSyncTaskPayload payload) {
        var projectGitlabId = payload.projectGitlabId();
        log.info("Processing ProjectSyncTask for gitlabId: {}", projectGitlabId);

        var externalProjectOpt = gitlabConnector.getProject(projectGitlabId);
        if (externalProjectOpt.isEmpty()) {
            throw new IllegalStateException("Gitlab project not found for id: " + projectGitlabId);
        }

        var externalProject = externalProjectOpt.get();
        var existingProjectOpt = projectPersistenceStorage.findByGitlabId(projectGitlabId);

        var projectToSave = existingProjectOpt
                .map(project -> updateAndReturnExistingProject(project, externalProject))
                .orElseGet(() -> buildNewProject(projectGitlabId, externalProject));

        transactionTemplate.executeWithoutResult(status -> {
            projectPersistenceStorage.save(projectToSave);
            scheduledTaskProducerService.produceTask(buildMrListUpdateScheduledTask(projectGitlabId));
        });

        log.info("Successfully synced project gitlabId: {}, name: {} with status PENDING_MR_SYNC", projectGitlabId, projectToSave.getName());
    }

    private Project updateAndReturnExistingProject(Project existingProject, Project externalProject) {
        existingProject.setName(externalProject.getName());
        existingProject.setWebUrl(externalProject.getWebUrl());
        existingProject.setStatus(ProjectStatus.PENDING_MR_SYNC);
        existingProject.setUpdatedAt(Instant.now());
        return existingProject;
    }

    private Project buildNewProject(Long projectGitlabId, Project externalProject) {
        return Project.builder()
                .gitlabId(projectGitlabId)
                .name(externalProject.getName())
                .webUrl(externalProject.getWebUrl())
                .status(ProjectStatus.PENDING_MR_SYNC)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private ScheduledTask buildMrListUpdateScheduledTask(Long projectGitlabId) {
        var payload = new ProjectMergeRequestListUpdateTaskPayload(projectGitlabId);
        return ScheduledTask.builder()
                .taskType(ScheduledTaskType.PROJECT_MR_LIST_UPDATE)
                .idempotencyKey(ScheduledTaskType.PROJECT_MR_LIST_UPDATE + "_" + projectGitlabId)
                .payload(serializePayload(payload))
                .state(ScheduledTaskState.SCHEDULED)
                .scheduledAt(Instant.now())
                .retries(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
