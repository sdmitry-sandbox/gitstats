package io.cx.gitstats.domain.task;

import io.cx.gitstats.domain.persistence.ScheduledTaskPersistenceStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.cx.gitstats.domain.utils.SerializationUtils.readPayloadValue;

@Slf4j
@Component
public class ScheduledTasksProcessingLogic {
    private static final int MAX_RETRIES = 5;

    private final ScheduledTaskPersistenceStorage scheduledTaskPersistenceStorage;
    private final ScheduledTasksExecutorProvider executorProvider;
    private final Map<ScheduledTaskType, ScheduledTaskProcessor<?>> processors = new HashMap<>();

    public ScheduledTasksProcessingLogic(ScheduledTaskPersistenceStorage scheduledTaskPersistenceStorage,
                                         ScheduledTasksExecutorProvider executorProvider,
                                         List<ScheduledTaskProcessor<?>> processorList) {
        this.scheduledTaskPersistenceStorage = scheduledTaskPersistenceStorage;
        this.executorProvider = executorProvider;
        if (processorList != null) {
            for (ScheduledTaskProcessor<?> processor : processorList) {
                this.processors.put(processor.getTaskType(), processor);
            }
        }
    }

    public void processScheduledTasks() {
        int availableThreads = executorProvider.getAvailableThreads();
        if (availableThreads <= 0) {
            log.info("No available threads in scheduledTasksExecutor, skipping polling");
            return;
        }

        int limit = Math.min(availableThreads, executorProvider.getCorePoolSize());
        var now = Instant.now();

        var tasks = scheduledTaskPersistenceStorage.fetchScheduledTasksForUpdate(now, limit);
        if (tasks.isEmpty()) {
            return;
        }

        log.info("Fetched {} scheduled tasks for processing", tasks.size());

        for (ScheduledTask task : tasks) {
            executorProvider.getExecutor().execute(() -> executeTask(task));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void executeTask(ScheduledTask task) {
        ScheduledTaskProcessor processor = processors.get(task.getTaskType());
        if (processor == null) {
            log.error("No processor found for taskType: {}, taskId: {}", task.getTaskType(), task.getId());
            scheduledTaskPersistenceStorage.updateState(task.getId(), ScheduledTaskState.ERROR, task.getScheduledAt(), task.getRetries());
            return;
        }

        try {
            processor.process(readPayloadValue(task.getPayload(), processor.getPayloadClass()));
            scheduledTaskPersistenceStorage.deleteById(task.getId());
            log.info("Successfully processed and deleted task id: {}, type: {}, key: {}", task.getId(), task.getTaskType(), task.getIdempotencyKey());
        } catch (Exception e) {
            log.error("Error processing task id: {}, type: {}, key: {}. Error: {}", task.getId(), task.getTaskType(), task.getIdempotencyKey(), e.getMessage(), e);
            handleFailure(task);
        }
    }

    private void handleFailure(ScheduledTask task) {
        int nextRetries = (task.getRetries() != null ? task.getRetries() : 0) + 1;
        if (nextRetries >= MAX_RETRIES) {
            log.warn("Task id: {} reached max retries ({}), deleting task from DB", task.getId(), MAX_RETRIES);
            scheduledTaskPersistenceStorage.deleteById(task.getId());
        } else {
            long delaySeconds = (long) Math.pow(2, nextRetries) * 5; // Exponential backoff: 10s, 20s, 40s, 80s...
            var nextScheduledAt = Instant.now().plus(Duration.ofSeconds(delaySeconds));
            log.info("Task id: {} failed. Scheduling retry {}/{} at {}", task.getId(), nextRetries, MAX_RETRIES, nextScheduledAt);
            scheduledTaskPersistenceStorage.updateState(task.getId(), ScheduledTaskState.SCHEDULED, nextScheduledAt, nextRetries);
        }
    }
}
