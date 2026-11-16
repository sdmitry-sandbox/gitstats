package io.cx.gitstats.domain.service;

import io.cx.gitstats.domain.task.ScheduledTask;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public interface ScheduledTaskProducerService {
    void produceTasks(List<ScheduledTask> tasks);
    void produceTask(ScheduledTask task);
    void produceTasksWithEvenDistribution(List<ScheduledTask> tasks, Instant start, Duration distributionWindow);
    void produceTasksWithEvenDistribution(List<ScheduledTask> tasks, Duration distributionWindow);
}
