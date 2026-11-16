package io.cx.gitstats.persistence.repository;

import io.cx.gitstats.persistence.entity.ScheduledTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ScheduledTaskRepository extends JpaRepository<ScheduledTaskEntity, Long> {

    @Query(value = """
            SELECT * FROM scheduled_tasks
            WHERE state = 'SCHEDULED' AND scheduled_at <= :now
            ORDER BY scheduled_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<ScheduledTaskEntity> findScheduledForUpdate(@Param("now") Instant now, @Param("limit") int limit);

    @Modifying
    @Query("""
            UPDATE ScheduledTaskEntity t
            SET t.state = :state, t.scheduledAt = :scheduledAt, t.retries = :retries, t.updatedAt = :now
            WHERE t.id = :id
            """)
    void updateTaskState(@Param("id") Long id,
                         @Param("state") io.cx.gitstats.domain.task.ScheduledTaskState state,
                         @Param("scheduledAt") Instant scheduledAt,
                         @Param("retries") int retries,
                         @Param("now") Instant now);
}
