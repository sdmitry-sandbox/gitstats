package io.cx.gitstats.domain.model.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PercentileRankBadge {
    private int percentile;
    private double rawPercentile;
    private long userValue;
    private double averageValue;
    private double comparisonWithAveragePercent;
    private String detailsText;
    private Long rowsAdded;
    private Long rowsDeleted;
}
