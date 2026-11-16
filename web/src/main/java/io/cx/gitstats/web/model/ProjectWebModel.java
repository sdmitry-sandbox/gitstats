package io.cx.gitstats.web.model;

import io.cx.gitstats.domain.model.ProjectStatus;
import lombok.Builder;

import java.time.Instant;

@Builder
public record ProjectWebModel(
        Long id,
        Long gitlabId,
        String name,
        String webUrl,
        ProjectStatus status,
        Instant updatedAt,
        Instant lastSyncedAt
) {
}
