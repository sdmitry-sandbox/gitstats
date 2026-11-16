package io.cx.gitstats.it.tests;

import io.cx.gitstats.Application;
import io.cx.gitstats.domain.model.Project;
import io.cx.gitstats.domain.model.ProjectStatus;
import io.cx.gitstats.domain.service.ProjectService;
import io.cx.gitstats.domain.task.ScheduledTasksProcessingJob;
import io.cx.gitstats.it.mock.GitlabMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Application.class)
public class ProjectSyncIntegrationTest {

    private static GitlabMockServer mockServer;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ScheduledTasksProcessingJob processingJob;

    @BeforeAll
    static void setUpAll() throws IOException {
        mockServer = new GitlabMockServer();
        mockServer.start();
    }

    @AfterAll
    static void tearDownAll() {
        if (mockServer != null) {
            mockServer.stop();
        }
    }

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("gitlab.base-url", () -> mockServer.getBaseUrl());
    }

    @Test
    void createAndScheduleSync_whenProjectSynced_thenStatusIsSyncedAndDataUpdated() throws Exception {
        // given
        var gitlabId = 999123L;
        var projectName = "sample-repo";
        var webUrl = "https://gitlab.cldx.io/group/sample-repo";
        mockServer.stubProject(gitlabId, projectName, webUrl);

        // when
        var createdProject = projectService.createAndScheduleSync(gitlabId);

        // then
        assertThat(createdProject).isNotNull();
        assertThat(createdProject.getGitlabId()).isEqualTo(999123L);
        assertThat(createdProject.getStatus()).isEqualTo(ProjectStatus.CREATING);
        assertThat(createdProject.getLastSyncedAt()).isNull();

        // trigger scheduled task processing
        processingJob.run();

        // and wait/verify updated status
        Optional<Project> syncedOpt = projectService.findByGitlabId(gitlabId);
        assertThat(syncedOpt).isPresent();

        var synced = syncedOpt.get();
        assertThat(synced.getStatus()).isEqualTo(ProjectStatus.SYNCED);
        assertThat(synced.getName()).isEqualTo("sample-repo");
        assertThat(synced.getWebUrl()).isEqualTo("https://gitlab.cldx.io/group/sample-repo");
        assertThat(synced.getLastSyncedAt()).isNotNull();
    }
}
