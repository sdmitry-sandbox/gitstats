package io.cx.gitstats.persistence.mapper;

import io.cx.gitstats.domain.task.ScheduledTask;
import io.cx.gitstats.persistence.entity.ScheduledTaskEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper
public interface ScheduledTaskPersistenceMapper {
    ScheduledTask toDomain(ScheduledTaskEntity entity);
    ScheduledTaskEntity toEntity(ScheduledTask domain);
    List<ScheduledTask> toDomainList(List<ScheduledTaskEntity> entities);
}
