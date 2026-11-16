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
public class ProjectUserStats {
    private List<ProjectUserCodeStats> codeUsers;
    private List<ProjectUserCountStats> approvedUsers;
    private List<ProjectUserCountStats> commentedUsers;
}
