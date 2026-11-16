package io.cx.gitstats.domain.model.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MrDetailsForAnalytics {
    private Long id;
    private Long gitlabId;
    private Long projectId;
    private String projectName;
    private Long authorId;
    private String authorUsername;
    private String authorName;
    private Long approverId;
    private Integer rowsAdded;
    private Integer rowsDeleted;
    private Instant createdAt;
    private Instant mergedAt;
    private Instant approvedAt;
    private Instant readyForReviewAt;
    private Instant reviewRequestedAt;
    private Instant reviewFinishedAt;
    private int commentsCount;
    private int meaningfulCommentsCount;
}
