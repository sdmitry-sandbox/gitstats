package io.cx.gitstats.persistence.storage;

import io.cx.gitstats.domain.model.MergeRequestGitlabState;
import io.cx.gitstats.domain.model.MergeRequestStatus;
import io.cx.gitstats.domain.model.UserGitlabState;
import io.cx.gitstats.persistence.AbstractPersistenceTest;
import io.cx.gitstats.persistence.service.MaterializedViewRefreshService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class UserContributionPersistencePostgresStorageTest extends AbstractPersistenceTest {

    private final UserContributionPersistencePostgresStorage storage;
    private final MaterializedViewRefreshService refreshService;

    @Test
    void findDailyContributionsByUserId_whenDataExists_thenReturnDailyContributions() {
        // given
        var project = testHelper.createAndSaveProjectEntity(801L, "Proj Contrib", io.cx.gitstats.domain.model.ProjectStatus.SYNCED);
        var user = testHelper.createAndSaveUserEntity(701L, "user_contrib", "User Contrib", UserGitlabState.ACTIVE);

        var mr = testHelper.buildMergeRequest(901L, project.getId(), "Title MR", MergeRequestGitlabState.MERGED, MergeRequestStatus.SYNCED);
        var mrEntity = testHelper.createAndSaveMergeRequestEntity(901L, project.getId(), "Title MR", MergeRequestGitlabState.MERGED, MergeRequestStatus.SYNCED, user.getId());

        testHelper.createAndSaveMergeRequestCommentEntity(902L, mrEntity.getId(), user.getId(), "Comment test");

        refreshService.refreshAllViews();

        // when
        var contributions = storage.findDailyContributionsByUserId(user.getId());

        // then
        assertThat(contributions).isNotEmpty();
        var contrib = contributions.get(0);
        assertThat(contrib.getUserId()).isEqualTo(user.getId());
        assertThat(contrib.getDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void findDailyContributionsByUsername_whenDataExists_thenReturnDailyContributions() {
        // given
        var project = testHelper.createAndSaveProjectEntity(802L, "Proj Contrib 2", io.cx.gitstats.domain.model.ProjectStatus.SYNCED);
        var user = testHelper.createAndSaveUserEntity(702L, "user_contrib_2", "User Contrib 2", UserGitlabState.ACTIVE);

        var mrEntity = testHelper.createAndSaveMergeRequestEntity(903L, project.getId(), "Title MR 2", MergeRequestGitlabState.MERGED, MergeRequestStatus.SYNCED, user.getId());

        refreshService.refreshAllViews();

        // when
        var contributions = storage.findDailyContributionsByUsername("user_contrib_2");

        // then
        assertThat(contributions).isNotEmpty();
        assertThat(contributions.get(0).getUsername()).isEqualTo("user_contrib_2");
    }

    @Test
    void findMaxDailyContribution_whenCalculated_thenReturnCorrectMax() {
        // given
        var project = testHelper.createAndSaveProjectEntity(803L, "Proj Contrib 3", io.cx.gitstats.domain.model.ProjectStatus.SYNCED);
        var user = testHelper.createAndSaveUserEntity(703L, "user_contrib_3", "User Contrib 3", UserGitlabState.ACTIVE);

        var mrEntity = testHelper.createAndSaveMergeRequestEntity(904L, project.getId(), "Title MR 3", MergeRequestGitlabState.MERGED, MergeRequestStatus.SYNCED, user.getId());

        refreshService.refreshAllViews();

        // when
        var maxVal = storage.findMaxDailyContribution(true, true, true, false, user.getId());

        // then
        assertThat(maxVal).isGreaterThanOrEqualTo(0);
    }

    @Test
    void findDailyContributionCounts_whenCalculated_thenReturnCounts() {
        // given
        var project = testHelper.createAndSaveProjectEntity(804L, "Proj Contrib 4", io.cx.gitstats.domain.model.ProjectStatus.SYNCED);
        var user = testHelper.createAndSaveUserEntity(704L, "user_contrib_4", "User Contrib 4", UserGitlabState.ACTIVE);

        var mrEntity = testHelper.createAndSaveMergeRequestEntity(905L, project.getId(), "Title MR 4", MergeRequestGitlabState.MERGED, MergeRequestStatus.SYNCED, user.getId());

        refreshService.refreshAllViews();

        // when
        var counts = storage.findDailyContributionCounts(true, true, true, false, user.getId());

        // then
        assertThat(counts).isNotEmpty();
    }
}
