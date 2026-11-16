package io.cx.gitstats.domain.task;

public interface ScheduledTaskProcessor<T extends ScheduledTaskPayload> {
    ScheduledTaskType getTaskType();
    Class<T> getPayloadClass();
    void process(T payload);
}
