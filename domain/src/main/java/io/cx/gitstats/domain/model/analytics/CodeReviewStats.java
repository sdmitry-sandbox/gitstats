package io.cx.gitstats.domain.model.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeReviewStats {
    private long totalApprovals;
    private long totalComments;
    private double averageCommentsPer1000Lines;
    private Double averageReactionTimeHours;
    private List<TtfrBySizePoint> ttfrByMrSize;
    private List<MrCommentsBySizePoint> producedCommentsBySize;
    private List<ReviewedAuthorPoint> reviewedAuthors;
    private List<TimeReviewActivityPoint> reviewActivityOverTime;
}
