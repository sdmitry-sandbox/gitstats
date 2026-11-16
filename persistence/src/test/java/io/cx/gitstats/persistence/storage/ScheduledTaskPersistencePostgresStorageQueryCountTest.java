package io.cx.gitstats.persistence.storage;

import io.cx.gitstats.domain.task.ScheduledTask;
import io.cx.gitstats.domain.task.ScheduledTaskState;
import io.cx.gitstats.domain.task.ScheduledTaskType;
import io.cx.gitstats.persistence.AbstractPersistenceTest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static io.cx.gitstats.persistence.utils.QueryCountAssertion.assertQueryCountAtMost;
import static io.cx.gitstats.persistence.utils.QueryCountAssertion.assertQueryCountEqual;

@RequiredArgsConstructor
class ScheduledTaskPersistencePostgresStorageQueryCountTest extends AbstractPersistenceTest {
    private final ScheduledTaskPersistencePostgresStorage storage;

    @Test
    void saveAllSkippingDuplicates_whenMultipleNewTasks_thenSingleDbQuery() {
        // given
        var task1 = testHelper.buildScheduledTask("qc_key_1");
        var task2 = testHelper.buildScheduledTask("qc_key_2");
        var task3 = testHelper.buildScheduledTask("qc_key_3");

        // when & then
        assertQueryCountEqual(1, () -> storage.saveAllSkippingDuplicatesAndReturnAcceptedKeys(List.of(task1, task2, task3)));
    }

    @Test
    void saveAllSkippingDuplicates_whenOneTaskAlreadyExists_thenSingleDbQuery() {
        // given
        testHelper.createAndSaveScheduledTaskEntity("qc_key_1", ScheduledTaskState.SCHEDULED, Instant.now());
        var task1 = testHelper.buildScheduledTask("qc_key_1");
        var task2 = testHelper.buildScheduledTask("qc_key_2");
        var task3 = testHelper.buildScheduledTask("qc_key_3");

        // when & then
        assertQueryCountEqual(1, () -> storage.saveAllSkippingDuplicatesAndReturnAcceptedKeys(List.of(task1, task2, task3)));
    }

    @Test
    void saveAllSkippingDuplicates_whenAllTasksAlreadyExist_thenSingleDbQuery() {
        // given
        testHelper.createAndSaveScheduledTaskEntity("qc_key_1", ScheduledTaskState.SCHEDULED, Instant.now());
        testHelper.createAndSaveScheduledTaskEntity("qc_key_2", ScheduledTaskState.SCHEDULED, Instant.now());
        testHelper.createAndSaveScheduledTaskEntity("qc_key_3", ScheduledTaskState.SCHEDULED, Instant.now());
        var task1 = testHelper.buildScheduledTask("qc_key_1");
        var task2 = testHelper.buildScheduledTask("qc_key_2");
        var task3 = testHelper.buildScheduledTask("qc_key_3");

        // when & then
        assertQueryCountEqual(1, () -> storage.saveAllSkippingDuplicatesAndReturnAcceptedKeys(List.of(task1, task2, task3)));
    }

    @Test
    void fetchScheduledTasksForUpdate_whenMatchingTasksExist_thenAtMostThreeDbQueries() {
        // given
        var past = Instant.now().minus(60, ChronoUnit.SECONDS);
        testHelper.createAndSaveScheduledTaskEntity("qc_ready_1", ScheduledTaskState.SCHEDULED, past);
        testHelper.createAndSaveScheduledTaskEntity("qc_ready_2", ScheduledTaskState.SCHEDULED, past.plusSeconds(10));

        // when & then
        assertQueryCountAtMost(3, () -> storage.fetchScheduledTasksForUpdate(Instant.now(), 10));
    }

    @Test
    void fetchScheduledTasksForUpdate_whenNoMatchingTasks_thenSingleDbQuery() {
        // given
        testHelper.createAndSaveScheduledTaskEntity("qc_future", ScheduledTaskState.SCHEDULED, Instant.now().plus(120, ChronoUnit.SECONDS));

        // when & then
        assertQueryCountEqual(1, () -> storage.fetchScheduledTasksForUpdate(Instant.now(), 10));
    }

    @Test
    void update_whenTaskExists_thenAtMostTwoDbQueries() {
        // given
        var entity = testHelper.createAndSaveScheduledTaskEntity("qc_update", ScheduledTaskState.SCHEDULED, Instant.now());
        var task = ScheduledTask.builder()
                .id(entity.getId())
                .taskType(ScheduledTaskType.PROJECT_SYNC_START)
                .idempotencyKey("qc_update")
                .payload("{\"updated\": true}")
                .state(ScheduledTaskState.ERROR)
                .scheduledAt(entity.getScheduledAt())
                .retries(2)
                .createdAt(entity.getCreatedAt())
                .updatedAt(Instant.now())
                .build();

        // when & then
        assertQueryCountAtMost(2, () -> storage.update(task));
    }

    @Test
    void updateState_whenTaskExists_thenSingleDbQuery() {
        // given
        var entity = testHelper.createAndSaveScheduledTaskEntity("qc_update_state", ScheduledTaskState.SCHEDULED, Instant.now());

        // when & then
        assertQueryCountEqual(1, () -> storage.updateState(entity.getId(), ScheduledTaskState.ERROR, Instant.now().plusSeconds(60), 1));
    }

    @Test
    void deleteById_whenTaskExists_thenAtMostTwoDbQueries() {
        // given
        var entity = testHelper.createAndSaveScheduledTaskEntity("qc_delete", ScheduledTaskState.SCHEDULED, Instant.now());

        // when & then
        assertQueryCountAtMost(2, () -> storage.deleteById(entity.getId()));
    }
}