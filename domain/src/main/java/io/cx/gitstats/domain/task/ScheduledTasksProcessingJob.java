package io.cx.gitstats.domain.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasksProcessingJob {
    private final ScheduledTasksProcessingLogic processingLogic;

    @Scheduled(fixedDelayString = "${scheduled.tasks.polling.interval:1000}")
    public void run() {
        try {
            processingLogic.processScheduledTasks();
        } catch (Exception e) {
            log.error("Unhandled error in ScheduledTasksProcessingJob: {}", e.getMessage(), e);
        }
    }
}
