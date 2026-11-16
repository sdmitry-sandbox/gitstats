package io.cx.gitstats.persistence.mapper;

import io.cx.gitstats.domain.model.Project;
import io.cx.gitstats.persistence.entity.ProjectEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(uses = {MergeRequestPersistenceMapper.class})
public interface ProjectPersistenceMapper {
    Project toDomain(ProjectEntity entity);

    @Mapping(target = "mergeRequests", ignore = true)
    ProjectEntity toEntity(Project domain);

    List<Project> toDomainList(List<ProjectEntity> entities);
}
