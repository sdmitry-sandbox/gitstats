package io.cx.gitstats.domain.model.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MrCommentsBySizePoint {
    private String bucketName;
    private double commentsPer1kLines;
    private double ratioPercentage;
}
