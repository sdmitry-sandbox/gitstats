package io.cx.gitstats.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeRequestComment {
    private Long id;
    private Long gitlabId;
    private Long mergeRequestId;
    private Long authorId;
    private Long authorGitlabId;
    private String message;
    private Boolean isResolved;
    private Instant resolvedAt;
    private Instant createdAt;
}
