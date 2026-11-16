package io.cx.gitstats.web.mapper;

import io.cx.gitstats.domain.model.Project;
import io.cx.gitstats.domain.model.ProjectStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class WebMapperTest {

    private final WebMapper webMapper = new WebMapperImpl();

    @Test
    void toWebModel_whenAllFieldsPresent_thenAllTargetFieldsMapped() {
        // given
        var project = Project.builder()
                .id(1L)
                .gitlabId(12345L)
                .name("test-project")
                .webUrl("https://gitlab.com/group/test-project")
                .status(ProjectStatus.SYNCED)
                .createdAt(Instant.parse("2026-11-16T10:00:00Z"))
                .updatedAt(Instant.parse("2026-11-16T10:30:00Z"))
                .lastSyncedAt(Instant.parse("2026-11-16T10:30:00Z"))
                .build();

        // when
        var webModel = webMapper.toWebModel(project);

        // then
        assertThat(webModel).isNotNull();
        assertThat(webModel.id()).isEqualTo(1L);
        assertThat(webModel.gitlabId()).isEqualTo(12345L);
        assertThat(webModel.name()).isEqualTo("test-project");
        assertThat(webModel.webUrl()).isEqualTo("https://gitlab.com/group/test-project");
        assertThat(webModel.status()).isEqualTo(ProjectStatus.SYNCED);
        assertThat(webModel.updatedAt()).isEqualTo(Instant.parse("2026-11-16T10:30:00Z"));
        assertThat(webModel.lastSyncedAt()).isEqualTo(Instant.parse("2026-11-16T10:30:00Z"));
    }

    @Test
    void toWebModel_whenDomainIsNull_thenReturnsNull() {
        // given
        Project project = null;

        // when
        var webModel = webMapper.toWebModel(project);

        // then
        assertThat(webModel).isNull();
    }
}
