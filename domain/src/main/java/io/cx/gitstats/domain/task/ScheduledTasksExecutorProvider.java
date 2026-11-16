package io.cx.gitstats.domain.task;

import java.util.concurrent.Executor;

public interface ScheduledTasksExecutorProvider {
    Executor getExecutor();
    int getAvailableThreads();
    int getCorePoolSize();
}
