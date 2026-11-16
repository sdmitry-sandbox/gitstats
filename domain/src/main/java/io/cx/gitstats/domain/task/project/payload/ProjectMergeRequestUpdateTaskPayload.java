package io.cx.gitstats.domain.task.project.payload;

import io.cx.gitstats.domain.task.ScheduledTaskPayload;
import lombok.Builder;

@Builder
public record ProjectMergeRequestUpdateTaskPayload(
        Long projectGitlabId,
        Long mergeRequestGitlabId
) implements ScheduledTaskPayload {
}
