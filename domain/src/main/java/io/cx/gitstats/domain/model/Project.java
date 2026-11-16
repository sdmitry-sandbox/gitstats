package io.cx.gitstats.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {
    private Long id;
    private Long gitlabId;
    private String name;
    private String webUrl;
    private ProjectStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastSyncedAt;
    private List<MergeRequest> mergeRequests;
}
