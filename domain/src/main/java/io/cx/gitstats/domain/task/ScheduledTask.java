package io.cx.gitstats.domain.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledTask {
    private Long id;
    private ScheduledTaskType taskType;
    private String idempotencyKey;
    private String payload;
    private ScheduledTaskState state;
    private Instant scheduledAt;
    private Integer retries;
    private Instant createdAt;
    private Instant updatedAt;
}
