package io.cx.gitstats.domain.persistence;

import io.cx.gitstats.domain.model.MergeRequest;
import io.cx.gitstats.domain.model.MergeRequestStatus;

import java.util.List;
import java.util.Optional;

public interface MergeRequestPersistenceStorage {
    MergeRequest save(MergeRequest mergeRequest);
    void saveAll(List<MergeRequest> mergeRequests);
    void updateAll(List<MergeRequest> mergeRequests);
    Optional<MergeRequest> findById(Long id);
    Optional<MergeRequest> findByProjectIdAndGitlabId(Long projectId, Long gitlabId);
    Optional<MergeRequest> findByGitlabId(Long gitlabId);
    List<MergeRequest> findAllByProjectId(Long projectId);
    List<MergeRequest> findAll();
    int countByProjectIdAndStatus(Long projectId, MergeRequestStatus status);
    void deleteById(Long id);
}
