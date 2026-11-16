package io.cx.gitstats.domain.model.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOverviewAnalytics {
    private Long userId;
    private String username;
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private GeneralStats generalStats;
    private CodeContributionStats codeContributionStats;
    private CodeReviewStats codeReviewStats;
    private ProjectStats projectStats;
    private ActivityStats activityStats;
}
