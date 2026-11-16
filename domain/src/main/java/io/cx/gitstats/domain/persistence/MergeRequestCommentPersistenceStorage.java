package io.cx.gitstats.domain.persistence;

import io.cx.gitstats.domain.model.MergeRequestComment;

import java.util.List;
import java.util.Optional;

public interface MergeRequestCommentPersistenceStorage {
    MergeRequestComment save(MergeRequestComment comment);
    void saveAll(List<MergeRequestComment> comments);
    Optional<MergeRequestComment> findById(Long id);
    List<MergeRequestComment> findAllByMergeRequestId(Long mergeRequestId);
    List<MergeRequestComment> findAll();
    void deleteById(Long id);
}
