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
public class UserCommentDailyViewRow {
    private Long commentId;
    private Long gitlabId;
    private Long mergeRequestId;
    private Long projectId;
    private String projectName;
    private Long authorId;
    private String authorUsername;
    private String authorName;
    private Boolean authorIsBot;
    private Long mrAuthorId;
    private String mrAuthorUsername;
    private String mrAuthorName;
    private Boolean mrAuthorIsBot;
    private Instant createdAt;
    private LocalDate contributionDate;
}
