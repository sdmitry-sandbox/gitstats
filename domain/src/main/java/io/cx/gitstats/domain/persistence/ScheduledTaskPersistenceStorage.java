package io.cx.gitstats.domain.persistence;

import io.cx.gitstats.domain.task.ScheduledTask;
import io.cx.gitstats.domain.task.ScheduledTaskState;

import java.time.Instant;
import java.util.List;

public interface ScheduledTaskPersistenceStorage {
    List<String> saveAllSkippingDuplicatesAndReturnAcceptedKeys(List<ScheduledTask> tasks);
    List<ScheduledTask> fetchScheduledTasksForUpdate(Instant now, int limit);
    ScheduledTask update(ScheduledTask task);
    void updateState(Long id, ScheduledTaskState state, Instant scheduledAt, int retries);
    void deleteById(Long id);
}
