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
public class ProjectStats {
    private List<ProjectCodeStats> codeProjects;
    private List<ProjectCountStats> approvedProjects;
    private List<ProjectCountStats> commentedProjects;
}
