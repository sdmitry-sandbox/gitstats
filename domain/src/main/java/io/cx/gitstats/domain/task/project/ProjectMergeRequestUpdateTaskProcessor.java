package io.cx.gitstats.domain.task.project;

import io.cx.gitstats.domain.connector.GitlabConnector;
import io.cx.gitstats.domain.model.MergeRequest;
import io.cx.gitstats.domain.model.MergeRequestStatus;
import io.cx.gitstats.domain.model.Project;
import io.cx.gitstats.domain.model.User;
import io.cx.gitstats.domain.persistence.MergeRequestCommentPersistenceStorage;
import io.cx.gitstats.domain.persistence.MergeRequestPersistenceStorage;
import io.cx.gitstats.domain.persistence.ProjectPersistenceStorage;
import io.cx.gitstats.domain.persistence.UserPersistenceStorage;
import io.cx.gitstats.domain.task.ScheduledTaskProcessor;
import io.cx.gitstats.domain.task.ScheduledTaskType;
import io.cx.gitstats.domain.task.project.payload.ProjectMergeRequestUpdateTaskPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectMergeRequestUpdateTaskProcessor implements ScheduledTaskProcessor<ProjectMergeRequestUpdateTaskPayload> {

    private final GitlabConnector gitlabConnector;
    private final ProjectPersistenceStorage projectPersistenceStorage;
    private final MergeRequestPersistenceStorage mergeRequestPersistenceStorage;
    private final UserPersistenceStorage userPersistenceStorage;
    private final MergeRequestCommentPersistenceStorage mergeRequestCommentPersistenceStorage;
    private final TransactionTemplate transactionTemplate;

    @Override
    public ScheduledTaskType getTaskType() {
        return ScheduledTaskType.PROJECT_MR_UPDATE;
    }

    @Override
    public Class<ProjectMergeRequestUpdateTaskPayload> getPayloadClass() {
        return ProjectMergeRequestUpdateTaskPayload.class;
    }

    @Override
    public void process(ProjectMergeRequestUpdateTaskPayload payload) {
        var projectGitlabId = payload.projectGitlabId();
        var mergeRequestGitlabId = payload.mergeRequestGitlabId();

        log.info("Processing ProjectMergeRequestUpdateTask for projectGitlabId: {}, mrGitlabId: {}", projectGitlabId, mergeRequestGitlabId);

        var project = projectPersistenceStorage.getByGitlabId(projectGitlabId);
        var mergeRequest = mergeRequestPersistenceStorage.findByProjectIdAndGitlabId(project.getId(), mergeRequestGitlabId)
                .orElseThrow(() -> new IllegalStateException("MergeRequest not found for projectId: " + project.getId() + ", mrGitlabId: " + mergeRequestGitlabId));

        if (mergeRequest.getStatus() != MergeRequestStatus.SYNCING) {
            log.warn("MergeRequest id: {}, gitlabId: {} is not in SYNCING status (current: {}). Skipping update.",
                    mergeRequest.getId(), mergeRequest.getGitlabId(), mergeRequest.getStatus());
            return;
        }

        try {
            processUpdate(project, mergeRequest);
        } catch (Exception e) {
            log.warn("Error updating merge request gitlabId: {} for projectGitlabId: {}. Setting status to SYNC_ERROR",
                    mergeRequestGitlabId, projectGitlabId, e);
            mergeRequest.setStatus(MergeRequestStatus.SYNC_ERROR);
            mergeRequestPersistenceStorage.updateAll(List.of(mergeRequest));
        }
    }

    private void processUpdate(Project project, MergeRequest mergeRequest) {
        var projectGitlabId = project.getGitlabId();
        var mergeRequestGitlabId = mergeRequest.getGitlabId();

        gitlabConnector.setMergeRequestChanges(projectGitlabId, mergeRequest);
        gitlabConnector.setMergeRequestCommentsData(projectGitlabId, mergeRequest);

        var userGitlabIds = collectUserGitlabIds(mergeRequest);
        var missingUsers = fetchMissingUsers(userGitlabIds);

        persistData(project, mergeRequest, userGitlabIds, missingUsers);

        log.info("Successfully updated merge request gitlabId: {} for projectGitlabId: {}", mergeRequestGitlabId, projectGitlabId);
    }

    private Set<Long> collectUserGitlabIds(MergeRequest mergeRequest) {
        var userGitlabIds = new HashSet<Long>();
        if (mergeRequest.getAuthorGitlabId() != null) {
            userGitlabIds.add(mergeRequest.getAuthorGitlabId());
        }
        if (mergeRequest.getApproverGitlabId() != null) {
            userGitlabIds.add(mergeRequest.getApproverGitlabId());
        }
        if (mergeRequest.getComments() != null) {
            for (var comment : mergeRequest.getComments()) {
                if (comment.getAuthorGitlabId() != null) {
                    userGitlabIds.add(comment.getAuthorGitlabId());
                }
            }
        }
        return userGitlabIds;
    }

    private List<User> fetchMissingUsers(Set<Long> userGitlabIds) {
        if (userGitlabIds.isEmpty()) {
            return List.of();
        }
        var existingUsers = userPersistenceStorage.findAllByGitlabIdIn(userGitlabIds);
        var existingGitlabIds = existingUsers.stream()
                .map(User::getGitlabId)
                .collect(Collectors.toSet());

        var missingGitlabIds = userGitlabIds.stream()
                .filter(id -> !existingGitlabIds.contains(id))
                .toList();

        var newUsers = new ArrayList<User>();
        for (var gitlabId : missingGitlabIds) {
            gitlabConnector.getUser(gitlabId).ifPresent(newUsers::add);
        }
        return newUsers;
    }

    private void persistData(Project project, MergeRequest mergeRequest, Set<Long> userGitlabIds, List<User> newUsers) {
        transactionTemplate.executeWithoutResult(status -> {
            if (!newUsers.isEmpty()) {
                userPersistenceStorage.saveAll(newUsers);
            }

            var allUsers = userPersistenceStorage.findAllByGitlabIdIn(userGitlabIds);
            var usersByGitlabId = allUsers.stream()
                    .collect(Collectors.toMap(User::getGitlabId, User::getId, (a, b) -> a));

            populateUserForeignKeys(mergeRequest, usersByGitlabId);

            if (mergeRequest.getComments() != null && !mergeRequest.getComments().isEmpty()) {
                mergeRequestCommentPersistenceStorage.saveAll(mergeRequest.getComments());
            }

            mergeRequest.setStatus(MergeRequestStatus.SYNCED);
            mergeRequestPersistenceStorage.updateAll(List.of(mergeRequest));
        });
    }

    private void populateUserForeignKeys(MergeRequest mergeRequest, Map<Long, Long> usersByGitlabId) {
        if (mergeRequest.getAuthorGitlabId() != null) {
            mergeRequest.setAuthorId(usersByGitlabId.get(mergeRequest.getAuthorGitlabId()));
        }
        if (mergeRequest.getApproverGitlabId() != null) {
            mergeRequest.setApproverId(usersByGitlabId.get(mergeRequest.getApproverGitlabId()));
        }
        if (mergeRequest.getComments() != null) {
            for (var comment : mergeRequest.getComments()) {
                comment.setMergeRequestId(mergeRequest.getId());
                if (comment.getAuthorGitlabId() != null) {
                    comment.setAuthorId(usersByGitlabId.get(comment.getAuthorGitlabId()));
                }
            }
        }
    }
}
