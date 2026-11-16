package io.cx.gitstats.persistence.repository;

import io.cx.gitstats.domain.model.MergeRequestStatus;
import io.cx.gitstats.persistence.entity.MergeRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MergeRequestRepository extends JpaRepository<MergeRequestEntity, Long> {
    List<MergeRequestEntity> findAllByProjectId(Long projectId);
    Optional<MergeRequestEntity> findByProjectIdAndGitlabId(Long projectId, Long gitlabId);
    Optional<MergeRequestEntity> findByGitlabId(Long gitlabId);
    int countByProjectIdAndStatus(Long projectId, MergeRequestStatus status);
}
