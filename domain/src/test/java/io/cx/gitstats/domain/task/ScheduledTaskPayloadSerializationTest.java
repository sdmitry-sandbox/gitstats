package io.cx.gitstats.domain.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cx.gitstats.domain.task.project.payload.ProjectFinalizeTaskSyncPayload;
import io.cx.gitstats.domain.task.project.payload.ProjectMergeRequestListUpdateTaskPayload;
import io.cx.gitstats.domain.task.project.payload.ProjectMergeRequestUpdateTaskPayload;
import io.cx.gitstats.domain.task.project.payload.ProjectSyncTaskPayload;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduledTaskPayloadSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializeAndDeserialize_whenProjectSyncTaskPayload_thenJsonMatchesAndObjectRestored() throws JsonProcessingException {
        // given
        var payload = ProjectSyncTaskPayload.builder()
                .projectGitlabId(12345L)
                .build();

        // when
        var json = objectMapper.writeValueAsString(payload);
        var deserialized = objectMapper.readValue(json, ProjectSyncTaskPayload.class);

        // then
        assertThat(json).contains("\"projectGitlabId\":12345");
        assertThat(deserialized).isEqualTo(payload);
        assertThat(deserialized.projectGitlabId()).isEqualTo(12345L);
    }

    @Test
    void serializeAndDeserialize_whenProjectMergeRequestListUpdateTaskPayload_thenJsonMatchesAndObjectRestored() throws JsonProcessingException {
        // given
        var payload = ProjectMergeRequestListUpdateTaskPayload.builder()
                .projectGitlabId(67890L)
                .build();

        // when
        var json = objectMapper.writeValueAsString(payload);
        var deserialized = objectMapper.readValue(json, ProjectMergeRequestListUpdateTaskPayload.class);

        // then
        assertThat(json).contains("\"projectGitlabId\":67890");
        assertThat(deserialized).isEqualTo(payload);
        assertThat(deserialized.projectGitlabId()).isEqualTo(67890L);
    }

    @Test
    void serializeAndDeserialize_whenProjectMergeRequestUpdateTaskPayload_thenJsonMatchesAndObjectRestored() throws JsonProcessingException {
        // given
        var payload = ProjectMergeRequestUpdateTaskPayload.builder()
                .projectGitlabId(111L)
                .mergeRequestGitlabId(222L)
                .build();

        // when
        var json = objectMapper.writeValueAsString(payload);
        var deserialized = objectMapper.readValue(json, ProjectMergeRequestUpdateTaskPayload.class);

        // then
        assertThat(json).contains("\"projectGitlabId\":111");
        assertThat(json).contains("\"mergeRequestGitlabId\":222");
        assertThat(deserialized).isEqualTo(payload);
        assertThat(deserialized.projectGitlabId()).isEqualTo(111L);
        assertThat(deserialized.mergeRequestGitlabId()).isEqualTo(222L);
    }

    @Test
    void serializeAndDeserialize_whenProjectFinalizeTaskSyncPayload_thenJsonMatchesAndObjectRestored() throws JsonProcessingException {
        // given
        var payload = ProjectFinalizeTaskSyncPayload.builder()
                .projectGitlabId(333L)
                .build();

        // when
        var json = objectMapper.writeValueAsString(payload);
        var deserialized = objectMapper.readValue(json, ProjectFinalizeTaskSyncPayload.class);

        // then
        assertThat(json).contains("\"projectGitlabId\":333");
        assertThat(deserialized).isEqualTo(payload);
        assertThat(deserialized.projectGitlabId()).isEqualTo(333L);
    }
}
