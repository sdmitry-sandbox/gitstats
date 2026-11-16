package io.cx.gitstats.persistence.storage;

import io.cx.gitstats.domain.model.MergeRequestGitlabState;
import io.cx.gitstats.domain.model.MergeRequestStatus;
import io.cx.gitstats.domain.model.ProjectStatus;
import io.cx.gitstats.domain.model.UserGitlabState;
import io.cx.gitstats.persistence.AbstractPersistenceTest;
import io.cx.gitstats.persistence.service.MaterializedViewRefreshService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class ProjectContributionPersistencePostgresStorageTest extends AbstractPersistenceTest {

    private final ProjectContributionPersistencePostgresStorage storage;
    private final MaterializedViewRefreshService refreshService;

    @Test
    void findDailyContributionsByProjectId_whenDataExists_thenReturnDailyContributions() {
        // given
        var project = testHelper.createAndSaveProjectEntity(1801L, "Proj Contrib Test", ProjectStatus.SYNCED);
        var user = testHelper.createAndSaveUserEntity(1701L, "proj_user_contrib", "Proj User Contrib", UserGitlabState.ACTIVE);

        var mrEntity = testHelper.createAndSaveMergeRequestEntity(
                1901L, project.getId(), "Title MR Project",
                MergeRequestGitlabState.MERGED, MergeRequestStatus.SYNCED, user.getId()
        );
        testHelper.createAndSaveMergeRequestCommentEntity(1902L, mrEntity.getId(), user.getId(), "Comment test");

        refreshService.refreshAllViews();

        // when
        var contributions = storage.findDailyContributionsByProjectId(project.getId());

        // then
        assertThat(contributions).isNotEmpty();
        var contrib = contributions.get(0);
        assertThat(contrib.getDate()).isEqualTo(LocalDate.now());
        assertThat(contrib.getMrCount()).isGreaterThanOrEqualTo(1);
        assertThat(contrib.getCommentCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void findDailyContributionsByProjectId_whenNoData_thenReturnEmptyList() {
        // given
        var project = testHelper.createAndSaveProjectEntity(1802L, "Empty Proj", ProjectStatus.SYNCED);

        refreshService.refreshAllViews();

        // when
        var contributions = storage.findDailyContributionsByProjectId(project.getId());

        // then
        assertThat(contributions).isEmpty();
    }
}
