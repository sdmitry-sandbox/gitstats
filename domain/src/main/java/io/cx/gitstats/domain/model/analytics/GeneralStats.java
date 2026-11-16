package io.cx.gitstats.domain.model.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralStats {
    private PercentileRankBadge codeContributionRank;
    private PercentileRankBadge mergedMrsRank;
    private PercentileRankBadge reviewActivityRank;
    private RadarChartStats radarChartStats;
}
