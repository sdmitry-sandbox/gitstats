package io.cx.gitstats.persistence.storage;

import io.cx.gitstats.domain.persistence.ScheduledTaskPersistenceStorage;
import io.cx.gitstats.domain.task.ScheduledTask;
import io.cx.gitstats.domain.task.ScheduledTaskState;
import io.cx.gitstats.persistence.entity.ScheduledTaskEntity;
import io.cx.gitstats.persistence.mapper.ScheduledTaskPersistenceMapper;
import io.cx.gitstats.persistence.repository.ScheduledTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduledTaskPersistencePostgresStorage implements ScheduledTaskPersistenceStorage {

    private final ScheduledTaskRepository scheduledTaskRepository;
    private final ScheduledTaskPersistenceMapper mapper;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public List<String> saveAllSkippingDuplicatesAndReturnAcceptedKeys(List<ScheduledTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }

        String sql = """
                INSERT INTO scheduled_tasks (task_type, idempotency_key, payload, state, scheduled_at, retries, created_at, updated_at)
                VALUES (:taskType, :idempotencyKey, :payload, :state, :scheduledAt, :retries, :createdAt, :updatedAt)
                ON CONFLICT (idempotency_key) DO NOTHING
                """;

        var batchParams = new ArrayList<SqlParameterSource>();
        var idempotencyKeys = new ArrayList<String>();

        for (ScheduledTask task : tasks) {
            var now = Instant.now();
            var scheduledAt = task.getScheduledAt() != null ? task.getScheduledAt() : now;
            var createdAt = task.getCreatedAt() != null ? task.getCreatedAt() : now;
            var updatedAt = task.getUpdatedAt() != null ? task.getUpdatedAt() : now;
            var state = task.getState() != null ? task.getState() : ScheduledTaskState.SCHEDULED;
            int retries = task.getRetries() != null ? task.getRetries() : 0;

            var params = new MapSqlParameterSource()
                    .addValue("taskType", task.getTaskType().name())
                    .addValue("idempotencyKey", task.getIdempotencyKey())
                    .addValue("payload", task.getPayload())
                    .addValue("state", state.name())
                    .addValue("scheduledAt", Timestamp.from(scheduledAt))
                    .addValue("retries", retries)
                    .addValue("createdAt", Timestamp.from(createdAt))
                    .addValue("updatedAt", Timestamp.from(updatedAt));

            batchParams.add(params);
            idempotencyKeys.add(task.getIdempotencyKey());
        }

        int[] affected = jdbcTemplate.batchUpdate(sql, batchParams.toArray(new SqlParameterSource[0]));
        var acceptedKeys = new ArrayList<String>();
        for (int i = 0; i < affected.length; i++) {
            if (affected[i] == 1) { // 1 = вставлено, 0 = пропущено (конфликт)
                acceptedKeys.add(idempotencyKeys.get(i));
            }
        }
        return acceptedKeys;
    }

    @Override
    @Transactional
    public List<ScheduledTask> fetchScheduledTasksForUpdate(Instant now, int limit) {
        List<ScheduledTaskEntity> entities = scheduledTaskRepository.findScheduledForUpdate(now, limit);
        for (ScheduledTaskEntity entity : entities) {
            entity.setState(ScheduledTaskState.PROCESSING);
            entity.setUpdatedAt(Instant.now());
        }
        scheduledTaskRepository.saveAll(entities);
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional
    public ScheduledTask update(ScheduledTask task) {
        ScheduledTaskEntity entity = mapper.toEntity(task);
        ScheduledTaskEntity saved = scheduledTaskRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void updateState(Long id, ScheduledTaskState state, Instant scheduledAt, int retries) {
        scheduledTaskRepository.updateTaskState(id, state, scheduledAt, retries, Instant.now());
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        scheduledTaskRepository.deleteById(id);
    }
}
