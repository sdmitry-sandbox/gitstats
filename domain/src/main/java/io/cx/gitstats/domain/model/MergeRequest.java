package io.cx.gitstats.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeRequest {
    private Long id;
    private Long gitlabId;
    private Long projectId;
    private Long authorId;
    private Long approverId;
    private Long authorGitlabId;
    private Long approverGitlabId;
    private String title;
    private MergeRequestGitlabState gitlabState;
    private MergeRequestStatus status;
    private String sourceBranch;
    private String targetBranch;
    private String webUrl;
    private Integer rowsAdded;
    private Integer rowsDeleted;
    private Instant approvedAt;
    private Instant mergedAt;
    private Instant createdAt;
    private Instant aiReviewStartedAt;
    private Instant aiReviewFinishedAt;
    private Instant readyForReviewAt;
    private Instant reviewRequestedAt;
    private Instant reviewFinishedAt;
    private Long humanReviewDurationSeconds;
    private Long botReviewDurationSeconds;
    private List<MergeRequestComment> comments;
}
