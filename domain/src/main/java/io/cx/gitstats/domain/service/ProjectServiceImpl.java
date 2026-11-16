package io.cx.gitstats.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cx.gitstats.domain.model.Project;
import io.cx.gitstats.domain.model.ProjectStatus;
import io.cx.gitstats.domain.persistence.ProjectPersistenceStorage;
import io.cx.gitstats.domain.task.ScheduledTask;
import io.cx.gitstats.domain.task.ScheduledTaskState;
import io.cx.gitstats.domain.task.ScheduledTaskType;
import io.cx.gitstats.domain.task.project.payload.ProjectSyncTaskPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
class ProjectServiceImpl implements ProjectService {
    private final ProjectPersistenceStorage projectPersistenceStorage;
    private final ScheduledTaskProducerService scheduledTaskProducerService;
    private final ObjectMapper objectMapper;

    @Override
    public Project createAndScheduleSync(Long gitlabId) {
        if (gitlabId == null) {
            throw new IllegalArgumentException("gitlabId must not be null");
        }
        var existingOpt = projectPersistenceStorage.findByGitlabId(gitlabId);
        if (existingOpt.isPresent()) {
            log.info("Project with id {} already exists, skipping creation", gitlabId);
            return existingOpt.get();
        }
        var projectCreateRequest = Project.builder()
                .gitlabId(gitlabId)
                .status(ProjectStatus.CREATING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        var project = projectPersistenceStorage.save(projectCreateRequest);

        var payload = new ProjectSyncTaskPayload(gitlabId);
        var syncTask = ScheduledTask.builder()
                .taskType(ScheduledTaskType.PROJECT_SYNC_START)
                .idempotencyKey(ScheduledTaskType.PROJECT_SYNC_START + "_" + gitlabId)
                .payload(serializePayload(payload))
                .state(ScheduledTaskState.SCHEDULED)
                .scheduledAt(Instant.now())
                .retries(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        scheduledTaskProducerService.produceTask(syncTask);
        log.info("Created project and scheduled sync for gitlabId: {}", gitlabId);

        return project;
    }

    private String serializePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize task payload: " + payload, e);
        }
    }

    @Override
    public Optional<Project> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return projectPersistenceStorage.findById(id);
    }

    @Override
    public Optional<Project> findByGitlabId(Long gitlabId) {
        return projectPersistenceStorage.findByGitlabId(gitlabId);
    }

    @Override
    public List<Project> findAll() {
        return projectPersistenceStorage.findAll();
    }

    @Override
    public List<Project> searchByName(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return projectPersistenceStorage.searchByName(query);
    }

    @Override
    public Project update(Project project) {
        return projectPersistenceStorage.save(project);
    }

    @Override
    public void deleteById(Long id) {
        projectPersistenceStorage.deleteById(id);
    }
}
