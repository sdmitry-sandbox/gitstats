package io.cx.gitstats.persistence.storage;

import io.cx.gitstats.domain.model.MergeRequestGitlabState;
import io.cx.gitstats.domain.model.MergeRequestStatus;
import io.cx.gitstats.domain.model.ProjectStatus;
import io.cx.gitstats.domain.model.UserGitlabState;
import io.cx.gitstats.domain.model.activity.UserActivityType;
import io.cx.gitstats.persistence.AbstractPersistenceTest;
import io.cx.gitstats.persistence.service.MaterializedViewRefreshService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class ProjectActivityPersistencePostgresStorageTest extends AbstractPersistenceTest {

    private final ProjectActivityPersistencePostgresStorage storage;
    private final MaterializedViewRefreshService refreshService;

    @Test
    void findProjectActivities_whenActivitiesExist_thenReturnActivities() {
        // given
        var project = testHelper.createAndSaveProjectEntity(2801L, "Proj Activity Test", ProjectStatus.SYNCED);
        var user = testHelper.createAndSaveUserEntity(2701L, "proj_activity_user", "Proj Activity User", UserGitlabState.ACTIVE);

        var mrEntity = testHelper.createAndSaveMergeRequestEntity(
                2901L, project.getId(), "Title MR Activity",
                MergeRequestGitlabState.MERGED, MergeRequestStatus.SYNCED, user.getId()
        );
        testHelper.createAndSaveMergeRequestCommentEntity(2902L, mrEntity.getId(), user.getId(), "Comment activity");

        refreshService.refreshAllViews();

        // when
        var activities = storage.findProjectActivities(
                project.getId(), null, null, true, true, true, 100
        );

        // then
        assertThat(activities).isNotEmpty();
        var types = activities.stream().map(a -> a.getType()).toList();
        assertThat(types).contains(UserActivityType.MERGE_REQUEST, UserActivityType.COMMENT);
        assertThat(activities.get(0).getUsername()).isEqualTo("proj_activity_user");
    }

    @Test
    void findProjectActivities_whenNoActivities_thenReturnEmptyList() {
        // given
        var project = testHelper.createAndSaveProjectEntity(2802L, "Empty Activity Proj", ProjectStatus.SYNCED);

        refreshService.refreshAllViews();

        // when
        var activities = storage.findProjectActivities(
                project.getId(), null, null, true, true, true, 100
        );

        // then
        assertThat(activities).isEmpty();
    }
}
