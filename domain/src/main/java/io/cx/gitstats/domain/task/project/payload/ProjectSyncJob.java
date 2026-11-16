package io.cx.gitstats.domain.task.project.payload;

import io.cx.gitstats.domain.connector.GitlabConnector;
import io.cx.gitstats.domain.model.Project;
import io.cx.gitstats.domain.model.ProjectStatus;
import io.cx.gitstats.domain.persistence.ProjectPersistenceStorage;
import io.cx.gitstats.domain.service.ScheduledTaskProducerService;
import io.cx.gitstats.domain.task.ScheduledTask;
import io.cx.gitstats.domain.task.ScheduledTaskState;
import io.cx.gitstats.domain.task.ScheduledTaskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;

import static io.cx.gitstats.domain.utils.SerializationUtils.serializePayload;

@Slf4j
@Component
public class ProjectSyncJob {

    private final ProjectPersistenceStorage projectPersistenceStorage;
    private final ScheduledTaskProducerService scheduledTaskProducerService;
    private final GitlabConnector gitlabConnector;
    private final Duration syncInterval;
    private final Random random = new Random();

    public ProjectSyncJob(
            ProjectPersistenceStorage projectPersistenceStorage,
            ScheduledTaskProducerService scheduledTaskProducerService,
            GitlabConnector gitlabConnector,
            @Value("${project.sync.interval}") Duration syncInterval
    ) {
        this.projectPersistenceStorage = projectPersistenceStorage;
        this.scheduledTaskProducerService = scheduledTaskProducerService;
        this.gitlabConnector = gitlabConnector;
        this.syncInterval = syncInterval;
    }

    @Scheduled(fixedDelayString = "${project.sync.interval}")
    public void run() {
        try {
            syncProjects();
        } catch (Exception e) {
            log.error("Unhandled error in ProjectSyncJob: {}", e.getMessage(), e);
        }
    }

    private void syncProjects() {
        var syncedProjects = projectPersistenceStorage.findAllByStatus(ProjectStatus.SYNCED);
        if (syncedProjects.isEmpty()) {
            return;
        }
        if (cannotConnectToGitlab(syncedProjects)) {
            return;
        }

        var now = Instant.now();
        var tasks = syncedProjects.stream()
                .map(project -> buildProjectSyncTask(project.getGitlabId(), now))
                .toList();
        var distributionWindow = syncInterval.dividedBy(2);
        scheduledTaskProducerService.produceTasksWithEvenDistribution(tasks, now, distributionWindow);
        log.info("Produced {} PROJECT_SYNC_START tasks for synced projects", tasks.size());
    }

    private boolean cannotConnectToGitlab(List<Project> syncedProjects) {
        var randomGitlabId = syncedProjects.get(random.nextInt(syncedProjects.size())).getGitlabId();
        try {
            gitlabConnector.getProject(randomGitlabId);
            return false;
        } catch (Exception e) {
            log.warn("Failed to fetch project from GitLab for gitlabId: {}. Error: {}", randomGitlabId, e.getMessage());
            return true;
        }
    }

    private ScheduledTask buildProjectSyncTask(Long projectGitlabId, Instant now) {
        var payload = new ProjectSyncTaskPayload(projectGitlabId);
        return ScheduledTask.builder()
                .taskType(ScheduledTaskType.PROJECT_SYNC_START)
                .idempotencyKey(ScheduledTaskType.PROJECT_SYNC_START + "_" + projectGitlabId)
                .payload(serializePayload(payload))
                .state(ScheduledTaskState.SCHEDULED)
                .scheduledAt(now)
                .retries(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
