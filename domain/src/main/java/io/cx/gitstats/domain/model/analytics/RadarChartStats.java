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
public class RadarChartStats {
    private List<RadarMetricItem> contributionMetrics;
    private List<RadarMetricItem> reviewMetrics;
    private List<RadarMetricItem> allMetrics;
}
