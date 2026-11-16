package io.cx.gitstats.domain.model.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMergeRequestDailyViewRow {
    private Long mrId;
    private Long gitlabId;
    private Long projectId;
    private String projectName;
    private Long authorId;
    private String authorUsername;
    private String authorName;
    private Boolean authorIsBot;
    private Long approverId;
    private String approverUsername;
    private String approverName;
    private Boolean approverIsBot;
    private Integer rowsAdded;
    private Integer rowsDeleted;
    private Integer totalCode;
    private Instant createdAt;
    private Instant mergedAt;
    private Instant approvedAt;
    private Instant readyForReviewAt;
    private Instant reviewRequestedAt;
    private Instant reviewFinishedAt;
    private LocalDate contributionDate;
    private LocalDate approvedDate;
}
