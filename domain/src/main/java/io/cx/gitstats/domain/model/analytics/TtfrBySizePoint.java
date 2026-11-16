package io.cx.gitstats.domain.model.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TtfrBySizePoint {
    private String bucketName;
    private Double averageHours;
    private String formattedTime;
    private double barPercentage;
}
