package io.cx.gitstats.domain.config;

import io.cx.gitstats.domain.task.ScheduledTasksExecutorProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ExecutorsConfig {

    @Bean
    public ScheduledTasksExecutorProvider scheduledTasksExecutorProvider(@Value("${scheduled-tasks.threads}") int threads,
                                                                         @Value("${scheduled-tasks.queue-capacity}") int queueCapacity) {
        var threadPoolExecutor = new ThreadPoolExecutor(
                threads,
                threads,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                r -> {
                    Thread thread = new Thread(r);
                    thread.setName("scheduled-task-worker-" + thread.getId());
                    thread.setDaemon(true);
                    return thread;
                }
        );

        return new ScheduledTasksExecutorProvider() {
            @Override
            public Executor getExecutor() {
                return threadPoolExecutor;
            }

            @Override
            public int getAvailableThreads() {
                return threadPoolExecutor.getCorePoolSize() - threadPoolExecutor.getActiveCount();
            }

            @Override
            public int getCorePoolSize() {
                return threadPoolExecutor.getCorePoolSize();
            }
        };
    }
}
