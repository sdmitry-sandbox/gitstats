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
public class WeeklyCoveragePoint {
    private String weekLabel;
    private LocalDate weekStartDate;
    private long totalMrs;
    private long mrsWithComments;
    private double coveragePercent;
}
