package io.cx.gitstats.persistence.repository;

import io.cx.gitstats.persistence.entity.MergeRequestCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MergeRequestCommentRepository extends JpaRepository<MergeRequestCommentEntity, Long> {
    List<MergeRequestCommentEntity> findAllByMergeRequestId(Long mergeRequestId);
}
