package io.cx.gitstats.domain.model.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoxPlotPoint {
    private String category;
    private double min;
    private double q1;
    private double median;
    private double q3;
    private double max;
}
