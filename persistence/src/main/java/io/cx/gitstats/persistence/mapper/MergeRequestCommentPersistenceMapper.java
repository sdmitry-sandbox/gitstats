package io.cx.gitstats.persistence.mapper;

import io.cx.gitstats.domain.model.MergeRequestComment;
import io.cx.gitstats.persistence.entity.MergeRequestCommentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper
public interface MergeRequestCommentPersistenceMapper {
    @Mapping(target = "mergeRequestId", source = "mergeRequest.id")
    @Mapping(target = "authorId", source = "author.id")
    @Mapping(target = "authorGitlabId", ignore = true)
    MergeRequestComment toDomain(MergeRequestCommentEntity entity);

    @Mapping(target = "mergeRequest.id", source = "mergeRequestId")
    @Mapping(target = "author.id", source = "authorId")
    MergeRequestCommentEntity toEntity(MergeRequestComment domain);

    List<MergeRequestComment> toDomainList(List<MergeRequestCommentEntity> entities);
}
