package io.cx.gitstats.persistence.mapper;

import io.cx.gitstats.domain.model.MergeRequest;
import io.cx.gitstats.persistence.entity.MergeRequestEntity;
import io.cx.gitstats.persistence.entity.ProjectEntity;
import io.cx.gitstats.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper
public interface MergeRequestPersistenceMapper {
    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "authorId", source = "author.id")
    @Mapping(target = "approverId", source = "approver.id")
    @Mapping(target = "authorGitlabId", ignore = true)
    @Mapping(target = "approverGitlabId", ignore = true)
    @Mapping(target = "comments", ignore = true)
    MergeRequest toDomain(MergeRequestEntity entity);

    @Mapping(target = "project", source = "projectId", qualifiedByName = "idToProjectEntity")
    @Mapping(target = "author", source = "authorId", qualifiedByName = "idToUserEntity")
    @Mapping(target = "approver", source = "approverId", qualifiedByName = "idToUserEntity")
    MergeRequestEntity toEntity(MergeRequest domain);

    List<MergeRequest> toDomainList(List<MergeRequestEntity> entities);

    @Named("idToUserEntity")
    default UserEntity idToUserEntity(Long id) {
        return id == null ? null : UserEntity.builder().id(id).build();
    }

    @Named("idToProjectEntity")
    default ProjectEntity idToProjectEntity(Long id) {
        return id == null ? null : ProjectEntity.builder().id(id).build();
    }
}
