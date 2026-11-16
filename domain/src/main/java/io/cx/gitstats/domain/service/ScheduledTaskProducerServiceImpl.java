package io.cx.gitstats.domain.service;

import io.cx.gitstats.domain.persistence.ScheduledTaskPersistenceStorage;
import io.cx.gitstats.domain.task.ScheduledTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
class ScheduledTaskProducerServiceImpl implements ScheduledTaskProducerService {
    private final ScheduledTaskPersistenceStorage scheduledTaskPersistenceStorage;

    @Override
    public void produceTask(ScheduledTask task) {
        produceTasks(List.of(task));
    }

    @Override
    public void produceTasks(List<ScheduledTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        var acceptedKeys = scheduledTaskPersistenceStorage.saveAllSkippingDuplicatesAndReturnAcceptedKeys(tasks);
        var notAcceptedKeys = tasks.stream()
                .map(ScheduledTask::getIdempotencyKey)
                .filter(Objects::nonNull)
                .filter(idempotencyKey -> !acceptedKeys.contains(idempotencyKey))
                .distinct()
                .toList();
        if (!notAcceptedKeys.isEmpty()) {
            log.warn("Not accepted keys for scheduled tasks, already exists: " + notAcceptedKeys);
        }
    }

    @Override
    public void produceTasksWithEvenDistribution(List<ScheduledTask> tasks, Instant start, Duration distributionWindow) {
        if (tasks == null || tasks.isEmpty()) {
            log.warn("No tasks provided for scheduled task production");
            return;
        }
        long distributionWindowSeconds = distributionWindow.toSeconds();
        long intervalBetweenTasks = tasks.size() > 1 ? distributionWindowSeconds / (tasks.size() - 1) : 0L;

        for (int i = 0; i < tasks.size(); i++) {
            tasks.get(i).setScheduledAt(start.plusSeconds(intervalBetweenTasks * i));
        }
        produceTasks(tasks);
    }

    @Override
    public void produceTasksWithEvenDistribution(List<ScheduledTask> tasks, Duration distributionWindow) {
        produceTasksWithEvenDistribution(tasks, Instant.now(), distributionWindow);
    }
}
