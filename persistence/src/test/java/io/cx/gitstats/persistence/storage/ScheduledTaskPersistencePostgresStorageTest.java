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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@RequiredArgsConstructor
class ScheduledTaskPersistencePostgresStorageTest extends AbstractPersistenceTest {
    private final ScheduledTaskPersistencePostgresStorage storage;

    @Test
    void saveAllSkippingDuplicates_whenEmptyOrNullList_thenReturnEmptyList() {
        // when
        var resultNull = storage.saveAllSkippingDuplicatesAndReturnAcceptedKeys(null);
        var resultEmpty = storage.saveAllSkippingDuplicatesAndReturnAcceptedKeys(List.of());

        // then
        assertThat(resultNull).isEmpty();
        assertThat(resultEmpty).isEmpty();
    }

    @Test
    void saveAllSkippingDuplicates_whenNewTasks_thenAllInsertedAndKeysReturned() {
        // given
        var task1 = testHelper.buildScheduledTask("key_1");
        var task2 = testHelper.buildScheduledTask("key_2");

        // when
        var insertedKeys = storage.saveAllSkippingDuplicatesAndReturnAcceptedKeys(List.of(task1, task2));

        // then
        assertThat(insertedKeys).containsExactly("key_1", "key_2");
        assertThat(testHelper.findAllScheduledTasks()).hasSize(2);
    }

    @Test
    void saveAllSkippingDuplicates_whenSomeDuplicatesExist_thenInsertOnlyNewAndReturnTheirKeys() {
        // given
        testHelper.createAndSaveScheduledTaskEntity("key_1", ScheduledTaskState.SCHEDULED, Instant.now());
        var task1 = testHelper.buildScheduledTask("key_1");
        var task2 = testHelper.buildScheduledTask("key_2");
        var task3 = testHelper.buildScheduledTask("key_3");

        // when
        var insertedKeys = storage.saveAllSkippingDuplicatesAndReturnAcceptedKeys(List.of(task1, task2, task3));

        // then
        assertThat(insertedKeys).containsExactly("key_2", "key_3");
        assertThat(testHelper.findAllScheduledTasks()).hasSize(3);
    }

    @Test
    void saveAllSkippingDuplicates_whenAllDuplicatesExist_thenReturnEmptyList() {
        // given
        testHelper.createAndSaveScheduledTaskEntity("key_1", ScheduledTaskState.SCHEDULED, Instant.now());
        testHelper.createAndSaveScheduledTaskEntity("key_2", ScheduledTaskState.SCHEDULED, Instant.now());
        var task1 = testHelper.buildScheduledTask("key_1");
        var task2 = testHelper.buildScheduledTask("key_2");

        // when
        var insertedKeys = storage.saveAllSkippingDuplicatesAndReturnAcceptedKeys(List.of(task1, task2));

        // then
        assertThat(insertedKeys).isEmpty();
        assertThat(testHelper.findAllScheduledTasks()).hasSize(2);
    }

    @Test
    void saveAllSkippingDuplicates_whenOptionalFieldsNull_thenDefaultValuesApplied() {
        // given
        var taskWithNulls = ScheduledTask.builder()
                .taskType(ScheduledTaskType.PROJECT_SYNC_START)
                .idempotencyKey("key_null_fields")
                .payload("{\"projectGitlabId\": 456}")
                .state(null)
                .scheduledAt(null)
                .retries(null)
                .createdAt(null)
                .updatedAt(null)
                .build();

        // when
        var insertedKeys = storage.saveAllSkippingDuplicatesAndReturnAcceptedKeys(List.of(taskWithNulls));

        // then
        assertThat(insertedKeys).containsExactly("key_null_fields");
        var tasks = testHelper.findAllScheduledTasks();
        assertThat(tasks).hasSize(1);
        var saved = tasks.get(0);
        assertThat(saved.getState()).isEqualTo(ScheduledTaskState.SCHEDULED);
        assertThat(saved.getRetries()).isEqualTo(0);
        assertThat(saved.getScheduledAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void fetchScheduledTasksForUpdate_whenMatchingTasksExist_thenStateUpdatedToProcessingAndTasksReturned() {
        // given
        var past = Instant.now().minus(60, ChronoUnit.SECONDS);
        var future = Instant.now().plus(60, ChronoUnit.SECONDS);
        testHelper.createAndSaveScheduledTaskEntity("key_ready_1", ScheduledTaskState.SCHEDULED, past);
        testHelper.createAndSaveScheduledTaskEntity("key_ready_2", ScheduledTaskState.SCHEDULED, past.plusSeconds(10));
        testHelper.createAndSaveScheduledTaskEntity("key_future", ScheduledTaskState.SCHEDULED, future);
        testHelper.createAndSaveScheduledTaskEntity("key_processing", ScheduledTaskState.PROCESSING, past);

        // when
        var fetched = storage.fetchScheduledTasksForUpdate(Instant.now(), 10);

        // then
        assertThat(fetched).hasSize(2)
                .extracting(ScheduledTask::getIdempotencyKey)
                .containsExactly("key_ready_1", "key_ready_2");
        assertThat(fetched).allMatch(task -> task.getState() == ScheduledTaskState.PROCESSING);

        var tasksInDb = testHelper.findAllScheduledTasks();
        var task1InDb = tasksInDb.stream()
                .filter(t -> t.getIdempotencyKey().equals("key_ready_1"))
                .findFirst()
                .orElseThrow();
        var task2InDb = tasksInDb.stream()
                .filter(t -> t.getIdempotencyKey().equals("key_ready_2"))
                .findFirst()
                .orElseThrow();

        assertThat(task1InDb.getState()).isEqualTo(ScheduledTaskState.PROCESSING);
        assertThat(task2InDb.getState()).isEqualTo(ScheduledTaskState.PROCESSING);
    }

    @Test
    void fetchScheduledTasksForUpdate_whenLimitApplied_thenOnlyLimitedCountReturned() {
        // given
        var past = Instant.now().minus(60, ChronoUnit.SECONDS);
        testHelper.createAndSaveScheduledTaskEntity("key_1", ScheduledTaskState.SCHEDULED, past);
        testHelper.createAndSaveScheduledTaskEntity("key_2", ScheduledTaskState.SCHEDULED, past.plusSeconds(5));
        testHelper.createAndSaveScheduledTaskEntity("key_3", ScheduledTaskState.SCHEDULED, past.plusSeconds(10));

        // when
        var fetched = storage.fetchScheduledTasksForUpdate(Instant.now(), 2);

        // then
        assertThat(fetched).hasSize(2)
                .extracting(ScheduledTask::getIdempotencyKey)
                .containsExactly("key_1", "key_2");
    }

    @Test
    void fetchScheduledTasksForUpdate_whenNoMatchingTasks_thenReturnEmptyList() {
        // given
        testHelper.createAndSaveScheduledTaskEntity("key_future", ScheduledTaskState.SCHEDULED, Instant.now().plus(120, ChronoUnit.SECONDS));

        // when
        var fetched = storage.fetchScheduledTasksForUpdate(Instant.now(), 10);

        // then
        assertThat(fetched).isEmpty();
    }

    @Test
    void update_whenTaskUpdated_thenChangesPersistedAndReturned() {
        // given
        var entity = testHelper.createAndSaveScheduledTaskEntity("key_to_update", ScheduledTaskState.SCHEDULED, Instant.now());
        var domainTask = ScheduledTask.builder()
                .id(entity.getId())
                .taskType(ScheduledTaskType.PROJECT_SYNC_START)
                .idempotencyKey("key_to_update")
                .payload("{\"updated\": true}")
                .state(ScheduledTaskState.ERROR)
                .scheduledAt(entity.getScheduledAt())
                .retries(3)
                .createdAt(entity.getCreatedAt())
                .updatedAt(Instant.now())
                .build();

        // when
        var updated = storage.update(domainTask);

        // then
        assertThat(updated.getId()).isEqualTo(entity.getId());
        assertThat(updated.getState()).isEqualTo(ScheduledTaskState.ERROR);
        assertThat(updated.getRetries()).isEqualTo(3);
        assertThat(updated.getPayload()).isEqualTo("{\"updated\": true}");

        var entityInDb = testHelper.findScheduledTaskById(entity.getId());
        assertThat(entityInDb).isPresent();
        assertThat(entityInDb.get().getState()).isEqualTo(ScheduledTaskState.ERROR);
        assertThat(entityInDb.get().getRetries()).isEqualTo(3);
    }

    @Test
    void updateState_whenTaskExists_thenStateScheduledAtAndRetriesUpdated() {
        // given
        var entity = testHelper.createAndSaveScheduledTaskEntity("key_state_update", ScheduledTaskState.SCHEDULED, Instant.now());
        var newScheduledAt = Instant.now().plus(300, ChronoUnit.SECONDS).truncatedTo(ChronoUnit.MICROS);

        // when
        storage.updateState(entity.getId(), ScheduledTaskState.ERROR, newScheduledAt, 5);

        // then
        var updatedEntity = testHelper.findScheduledTaskById(entity.getId());
        assertThat(updatedEntity).isPresent();
        assertThat(updatedEntity.get().getState()).isEqualTo(ScheduledTaskState.ERROR);
        assertThat(updatedEntity.get().getRetries()).isEqualTo(5);
        assertThat(updatedEntity.get().getScheduledAt()).isEqualTo(newScheduledAt);
    }

    @Test
    void deleteById_whenTaskExists_thenTaskDeleted() {
        // given
        var entity = testHelper.createAndSaveScheduledTaskEntity("key_delete", ScheduledTaskState.SCHEDULED, Instant.now());

        // when
        storage.deleteById(entity.getId());

        // then
        assertThat(testHelper.findScheduledTaskById(entity.getId())).isEmpty();
    }

    @Test
    void deleteById_whenTaskDoesNotExist_thenNoException() {
        // given
        var nonExistentId = 999999L;

        // when & then
        assertThatNoException().isThrownBy(() -> storage.deleteById(nonExistentId));
    }
}