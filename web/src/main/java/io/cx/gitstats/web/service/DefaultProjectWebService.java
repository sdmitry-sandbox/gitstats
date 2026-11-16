package io.cx.gitstats.web.service;

import io.cx.gitstats.domain.model.Project;
import io.cx.gitstats.domain.model.ProjectStatus;
import io.cx.gitstats.domain.persistence.ProjectPersistenceStorage;
import io.cx.gitstats.domain.service.ScheduledTaskProducerService;
import io.cx.gitstats.domain.task.ScheduledTask;
import io.cx.gitstats.domain.task.ScheduledTaskState;
import io.cx.gitstats.domain.task.ScheduledTaskType;
import io.cx.gitstats.domain.task.project.payload.ProjectSyncTaskPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static io.cx.gitstats.domain.utils.SerializationUtils.serializePayload;

@Slf4j
@Service
public class DefaultProjectWebService {

    public static final Set<Long> DEFAULT_GITLAB_IDS = Set.of(
            1262L, 1733L, 2331L, 2332L, 1439L,
            1267L, 2739L, 1263L, 2171L, 1260L,
            1877L, 1426L, 1432L, 1256L, 1840L,
            1257L, 1609L, 1265L, 1264L, 2030L
    );

    private final ProjectPersistenceStorage projectPersistenceStorage;
    private final ScheduledTaskProducerService scheduledTaskProducerService;
    private final Duration syncInterval;

    public DefaultProjectWebService(
            ProjectPersistenceStorage projectPersistenceStorage,
            ScheduledTaskProducerService scheduledTaskProducerService,
            @Value("${project.sync.interval}") Duration syncInterval
    ) {
        this.projectPersistenceStorage = projectPersistenceStorage;
        this.scheduledTaskProducerService = scheduledTaskProducerService;
        this.syncInterval = syncInterval;
    }

    @Transactional
    public void addDefaultProjects() {
        var existingGitlabIds = projectPersistenceStorage.findExistingGitlabIds(DEFAULT_GITLAB_IDS);
        var missingGitlabIds = DEFAULT_GITLAB_IDS.stream()
                .filter(gitlabId -> !existingGitlabIds.contains(gitlabId))
                .toList();

        if (missingGitlabIds.isEmpty()) {
            log.info("All default projects already exist, skipping creation");
            return;
        }

        var now = Instant.now();
        var projectsToSave = buildDefaultProjects(missingGitlabIds, now);
        projectPersistenceStorage.saveAll(projectsToSave);

        var tasksToSchedule = buildDefaultTasks(missingGitlabIds, now);
        var distributionWindow = syncInterval.dividedBy(2);
        scheduledTaskProducerService.produceTasksWithEvenDistribution(tasksToSchedule, now, distributionWindow);

        log.info("Created {} default projects and scheduled sync", projectsToSave.size());
    }

    private List<Project> buildDefaultProjects(List<Long> gitlabIds, Instant now) {
        return gitlabIds.stream()
                .map(gitlabId -> Project.builder()
                        .gitlabId(gitlabId)
                        .status(ProjectStatus.CREATING)
                        .createdAt(now)
                        .updatedAt(now)
                        .build())
                .toList();
    }

    private List<ScheduledTask> buildDefaultTasks(List<Long> gitlabIds, Instant now) {
        return gitlabIds.stream()
                .map(gitlabId -> ScheduledTask.builder()
                        .taskType(ScheduledTaskType.PROJECT_SYNC_START)
                        .idempotencyKey(ScheduledTaskType.PROJECT_SYNC_START + "_" + gitlabId)
                        .payload(serializePayload(new ProjectSyncTaskPayload(gitlabId)))
                        .state(ScheduledTaskState.SCHEDULED)
                        .scheduledAt(now)
                        .retries(0)
                        .createdAt(now)
                        .updatedAt(now)
                        .build())
                .toList();
    }
}
