package io.cx.gitstats.domain.model.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectUserCountStats {
    private Long userId;
    private String username;
    private String name;
    private long count;
    private double percentageOfTotal;
}
