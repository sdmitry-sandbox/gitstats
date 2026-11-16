package io.cx.gitstats.domain.model.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectCodeStats {
    private Long projectId;
    private String projectName;
    private long totalCode;
    private long rowsAdded;
    private long rowsDeleted;
    private double percentageOfTotal;
}
