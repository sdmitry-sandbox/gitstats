package io.cx.gitstats.persistence.utils;

import io.cx.gitstats.domain.model.MergeRequest;
import io.cx.gitstats.domain.model.MergeRequestComment;
import io.cx.gitstats.domain.model.MergeRequestGitlabState;
import io.cx.gitstats.domain.model.MergeRequestStatus;
import io.cx.gitstats.domain.model.Project;
import io.cx.gitstats.domain.model.ProjectStatus;
import io.cx.gitstats.domain.model.User;
import io.cx.gitstats.domain.model.UserGitlabState;
import io.cx.gitstats.domain.task.ScheduledTask;
import io.cx.gitstats.domain.task.ScheduledTaskState;
import io.cx.gitstats.domain.task.ScheduledTaskType;
import io.cx.gitstats.persistence.entity.MergeRequestCommentEntity;
import io.cx.gitstats.persistence.entity.MergeRequestEntity;
import io.cx.gitstats.persistence.entity.ProjectEntity;
import io.cx.gitstats.persistence.entity.ScheduledTaskEntity;
import io.cx.gitstats.persistence.entity.UserEntity;
import io.cx.gitstats.persistence.repository.MergeRequestCommentRepository;
import io.cx.gitstats.persistence.repository.MergeRequestRepository;
import io.cx.gitstats.persistence.repository.ProjectRepository;
import io.cx.gitstats.persistence.repository.ScheduledTaskRepository;
import io.cx.gitstats.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PersistenceTestHelper {
    private final ProjectRepository projectRepository;
    private final ScheduledTaskRepository scheduledTaskRepository;
    private final MergeRequestRepository mergeRequestRepository;
    private final UserRepository userRepository;
    private final MergeRequestCommentRepository mergeRequestCommentRepository;

    @Transactional
    public void cleanUp() {
        scheduledTaskRepository.deleteAll();
        mergeRequestCommentRepository.deleteAll();
        mergeRequestRepository.deleteAll();
        userRepository.deleteAll();
        projectRepository.deleteAll();
    }

    @Transactional
    public ProjectEntity createAndSaveProjectEntity(Long gitlabId, String name, ProjectStatus status) {
        var now = Instant.now();
        var entity = ProjectEntity.builder()
                .gitlabId(gitlabId)
                .name(name)
                .webUrl("https://gitlab.example.com/project-" + gitlabId)
                .status(status)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return projectRepository.save(entity);
    }

    @Transactional
    public Project buildProject(Long gitlabId, String name, ProjectStatus status) {
        var now = Instant.now();
        return Project.builder()
                .gitlabId(gitlabId)
                .name(name)
                .webUrl("https://gitlab.example.com/project-" + gitlabId)
                .status(status)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Transactional
    public UserEntity createAndSaveUserEntity(Long gitlabId, String username, String name, UserGitlabState gitlabState) {
        return createAndSaveUserEntity(gitlabId, username, name, gitlabState, false);
    }

    @Transactional
    public UserEntity createAndSaveUserEntity(Long gitlabId, String username, String name, UserGitlabState gitlabState, boolean isBot) {
        var now = Instant.now();
        var entity = UserEntity.builder()
                .gitlabId(gitlabId)
                .username(username)
                .name(name)
                .gitlabState(gitlabState)
                .isBot(isBot)
                .avatarUrl("https://gitlab.example.com/avatar/" + gitlabId)
                .webUrl("https://gitlab.example.com/user/" + username)
                .createdAt(now)
                .build();
        return userRepository.save(entity);
    }

    public User buildUser(Long gitlabId, String username, String name, UserGitlabState gitlabState) {
        var now = Instant.now();
        return User.builder()
                .gitlabId(gitlabId)
                .username(username)
                .name(name)
                .gitlabState(gitlabState)
                .isBot(false)
                .avatarUrl("https://gitlab.example.com/avatar/" + gitlabId)
                .webUrl("https://gitlab.example.com/user/" + username)
                .createdAt(now)
                .build();
    }

    public Optional<UserEntity> findUserById(Long id) {
        return userRepository.findById(id);
    }

    public List<UserEntity> findAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public MergeRequestEntity createAndSaveMergeRequestEntity(Long gitlabId, Long projectId, String title, MergeRequestGitlabState gitlabState, MergeRequestStatus status) {
        return createAndSaveMergeRequestEntity(gitlabId, projectId, title, gitlabState, status, null);
    }

    @Transactional
    public MergeRequestEntity createAndSaveMergeRequestEntity(Long gitlabId, Long projectId, String title, MergeRequestGitlabState gitlabState, MergeRequestStatus status, Long authorId) {
        return createAndSaveMergeRequestEntity(gitlabId, projectId, title, gitlabState, status, authorId, null, null, null);
    }

    @Transactional
    public MergeRequestEntity createAndSaveMergeRequestEntity(Long gitlabId, Long projectId, String title, MergeRequestGitlabState gitlabState, MergeRequestStatus status, Long authorId, Integer rowsAdded, Integer rowsDeleted, Instant mergedAt) {
        return createAndSaveMergeRequestEntity(gitlabId, projectId, title, gitlabState, status, authorId, rowsAdded, rowsDeleted, mergedAt, null, null);
    }

    @Transactional
    public MergeRequestEntity createAndSaveMergeRequestEntity(Long gitlabId, Long projectId, String title, MergeRequestGitlabState gitlabState, MergeRequestStatus status, Long authorId, Integer rowsAdded, Integer rowsDeleted, Instant mergedAt, Instant approvedAt, Long approverId) {
        var now = Instant.now();
        var project = projectRepository.findById(projectId).orElseThrow();
        var author = authorId != null ? userRepository.findById(authorId).orElse(null) : null;
        var approver = approverId != null ? userRepository.findById(approverId).orElse(null) : null;
        var entity = MergeRequestEntity.builder()
                .gitlabId(gitlabId)
                .project(project)
                .author(author)
                .approver(approver)
                .title(title)
                .gitlabState(gitlabState)
                .status(status)
                .sourceBranch("feature/test-" + gitlabId)
                .targetBranch("main")
                .webUrl("https://gitlab.example.com/mr-" + gitlabId)
                .rowsAdded(rowsAdded)
                .rowsDeleted(rowsDeleted)
                .mergedAt(mergedAt)
                .approvedAt(approvedAt)
                .createdAt(now)
                .build();
        return mergeRequestRepository.save(entity);
    }

    public MergeRequest buildMergeRequest(Long gitlabId, Long projectId, String title, MergeRequestGitlabState gitlabState, MergeRequestStatus status) {
        var now = Instant.now();
        return MergeRequest.builder()
                .gitlabId(gitlabId)
                .projectId(projectId)
                .title(title)
                .gitlabState(gitlabState)
                .status(status)
                .sourceBranch("feature/test-" + gitlabId)
                .targetBranch("main")
                .webUrl("https://gitlab.example.com/mr-" + gitlabId)
                .createdAt(now)
                .build();
    }

    public Optional<MergeRequestEntity> findMergeRequestById(Long id) {
        return mergeRequestRepository.findById(id);
    }

    public List<MergeRequestEntity> findAllMergeRequests() {
        return mergeRequestRepository.findAll();
    }

    @Transactional
    public MergeRequestCommentEntity createAndSaveMergeRequestCommentEntity(Long gitlabId, Long mergeRequestId, Long authorId, String message) {
        var now = Instant.now();
        var mr = mergeRequestRepository.findById(mergeRequestId).orElseThrow();
        var author = authorId != null ? userRepository.findById(authorId).orElse(null) : null;
        var entity = MergeRequestCommentEntity.builder()
                .gitlabId(gitlabId)
                .mergeRequest(mr)
                .author(author)
                .message(message)
                .isResolved(false)
                .resolvedAt(null)
                .createdAt(now)
                .build();
        return mergeRequestCommentRepository.save(entity);
    }

    public MergeRequestComment buildMergeRequestComment(Long gitlabId, Long mergeRequestId, Long authorId, String message) {
        return MergeRequestComment.builder()
                .gitlabId(gitlabId)
                .mergeRequestId(mergeRequestId)
                .authorId(authorId)
                .message(message)
                .isResolved(false)
                .resolvedAt(null)
                .build();
    }

    public Optional<MergeRequestCommentEntity> findMergeRequestCommentById(Long id) {
        return mergeRequestCommentRepository.findById(id);
    }

    public List<MergeRequestCommentEntity> findAllMergeRequestComments() {
        return mergeRequestCommentRepository.findAll();
    }

    @Transactional
    public ScheduledTaskEntity createAndSaveScheduledTaskEntity(String idempotencyKey, ScheduledTaskState state, Instant scheduledAt) {
        var now = Instant.now();
        var entity = ScheduledTaskEntity.builder()
                .taskType(ScheduledTaskType.PROJECT_SYNC_START.name())
                .idempotencyKey(idempotencyKey)
                .payload("{\"projectGitlabId\": 123}")
                .state(state)
                .scheduledAt(scheduledAt != null ? scheduledAt : now)
                .retries(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return scheduledTaskRepository.save(entity);
    }

    public ScheduledTask buildScheduledTask(String idempotencyKey) {
        return buildScheduledTask(idempotencyKey, ScheduledTaskState.SCHEDULED, Instant.now());
    }

    public ScheduledTask buildScheduledTask(String idempotencyKey, ScheduledTaskState state, Instant scheduledAt) {
        var now = Instant.now();
        return ScheduledTask.builder()
                .taskType(ScheduledTaskType.PROJECT_SYNC_START)
                .idempotencyKey(idempotencyKey)
                .payload("{\"projectGitlabId\": 123}")
                .state(state)
                .scheduledAt(scheduledAt != null ? scheduledAt : now)
                .retries(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public Optional<ProjectEntity> findProjectById(Long id) {
        return projectRepository.findById(id);
    }

    public Optional<ScheduledTaskEntity> findScheduledTaskById(Long id) {
        return scheduledTaskRepository.findById(id);
    }

    public List<ScheduledTaskEntity> findAllScheduledTasks() {
        return scheduledTaskRepository.findAll();
    }
}
