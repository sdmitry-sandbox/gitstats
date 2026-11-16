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
public class CommentDetailsForAnalytics {
    private Long id;
    private Long gitlabId;
    private Long mergeRequestId;
    private Long mrAuthorId;
    private String mrAuthorUsername;
    private String mrAuthorName;
    private Long authorId;
    private Instant createdAt;
}
