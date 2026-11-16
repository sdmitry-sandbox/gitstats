package io.cx.gitstats.domain.model.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryData {
    private Long userId;
    private String username;
    private Boolean isBot;
    private int activeWorkingDaysCount;
    private long totalCode;
    private long rowsAdded;
    private long rowsDeleted;
    private long mergedMrsCount;
    private long totalApprovals;
    private long totalComments;
}
