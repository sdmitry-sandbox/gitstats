package io.cx.gitstats.domain.model.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewedAuthorPoint {
    private Long authorId;
    private String authorUsername;
    private String authorName;
    private double medianComments;
    private long totalComments;
    private double commentsPer1kLines;
}
