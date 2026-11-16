package io.cx.gitstats.domain.model.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeContributionStats {
    private long totalAdded;
    private long totalDeleted;
    private long totalCode;
    private double averageCodePerWorkingDay;
    private LocalDate adjustedPeriodStart;
    private double averageCodePerMergeRequest;
    private double commentsReceivedPer1kLines;
    private List<TimeCodeChangesPoint> stackedCodeChanges;
    private List<MrQuantityBySizePoint> mrQuantityBySize;
    private List<MrCommentsBySizePoint> receivedCommentsBySize;
    private List<ReviewedAuthorPoint> topReviewers;
}
