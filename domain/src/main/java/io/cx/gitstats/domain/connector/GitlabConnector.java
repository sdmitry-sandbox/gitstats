package io.cx.gitstats.domain.connector;

import io.cx.gitstats.domain.model.MergeRequest;
import io.cx.gitstats.domain.model.Project;
import io.cx.gitstats.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface GitlabConnector {
    Optional<Project> getProject(Long gitlabId);
    List<MergeRequest> getMergeRequests(Long projectGitlabId);
    Optional<User> getUser(Long gitlabId);
    void setMergeRequestChanges(Long projectGitlabId, MergeRequest mergeRequest);
    void setMergeRequestCommentsData(Long projectGitlabId, MergeRequest mergeRequest);
}
