package io.cx.gitstats.domain.model.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadarMetricItem {
    private String name;
    private double userValue;
    private double avgValue;
    private int scorePercent;
    private String formattedUserValue;
    private String formattedAvgValue;
    private String description;
}
