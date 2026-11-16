package io.cx.gitstats.connector.mapper;

import io.cx.gitstats.connector.gitlab.model.MergeRequestResponse;
import io.cx.gitstats.connector.gitlab.model.ProjectResponse;
import io.cx.gitstats.connector.gitlab.model.UserResponse;
import io.cx.gitstats.domain.model.MergeRequest;
import io.cx.gitstats.domain.model.MergeRequestGitlabState;
import io.cx.gitstats.domain.model.Project;
import io.cx.gitstats.domain.model.User;
import io.cx.gitstats.domain.model.UserGitlabState;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

@Mapper
public interface ConnectorMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "gitlabId", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "webUrl", source = "webUrl")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "mergeRequests", ignore = true)
    Project toDomain(ProjectResponse response);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "gitlabId", source = "iid")
    @Mapping(target = "projectId", ignore = true)
    @Mapping(target = "authorId", ignore = true)
    @Mapping(target = "approverId", ignore = true)
    @Mapping(target = "authorGitlabId", ignore = true)
    @Mapping(target = "approverGitlabId", ignore = true)
    @Mapping(target = "title", source = "title")
    @Mapping(target = "gitlabState", source = "state", qualifiedByName = "mapGitlabState")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "sourceBranch", ignore = true)
    @Mapping(target = "targetBranch", ignore = true)
    @Mapping(target = "webUrl", source = "webUrl")
    @Mapping(target = "rowsAdded", ignore = true)
    @Mapping(target = "rowsDeleted", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "mergedAt", ignore = true)
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "comments", ignore = true)
    MergeRequest toDomain(MergeRequestResponse response);

    List<MergeRequest> toDomainMergeRequestList(List<MergeRequestResponse> response);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "gitlabId", source = "id")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "gitlabState", source = "state", qualifiedByName = "mapUserGitlabState")
    @Mapping(target = "isBot", source = "bot")
    @Mapping(target = "avatarUrl", source = "avatarUrl")
    @Mapping(target = "webUrl", source = "webUrl")
    @Mapping(target = "createdAt", source = "createdAt")
    User toDomain(UserResponse response);

    default Instant map(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? offsetDateTime.toInstant() : null;
    }

    @Named("mapGitlabState")
    default MergeRequestGitlabState mapGitlabState(String state) {
        if (state == null) {
            return null;
        }
        return MergeRequestGitlabState.fromValue(state);
    }

    @Named("mapUserGitlabState")
    default UserGitlabState mapUserGitlabState(String state) {
        if (state == null) {
            return UserGitlabState.UNKNOWN;
        }
        return UserGitlabState.fromValue(state);
    }
}
