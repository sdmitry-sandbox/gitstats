package io.cx.gitstats.persistence.storage;

import io.cx.gitstats.domain.model.MergeRequestGitlabState;
import io.cx.gitstats.domain.model.MergeRequestStatus;
import io.cx.gitstats.domain.model.ProjectStatus;
import io.cx.gitstats.domain.model.UserGitlabState;
import io.cx.gitstats.persistence.AbstractPersistenceTest;
import io.cx.gitstats.persistence.service.MaterializedViewRefreshService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class UserAnalyticsPersistencePostgresStorageTest extends AbstractPersistenceTest {

    private final UserAnalyticsPersistencePostgresStorage storage;
    private final MaterializedViewRefreshService refreshService;

    @Test
    void findAllMergeRequests_whenMergeRequestsExist_thenReturnMergeRequestRows() {
        // given
        var project = testHelper.createAndSaveProjectEntity(1801L, "Proj Analytics MR", ProjectStatus.SYNCED);
        var author = testHelper.createAndSaveUserEntity(1701L, "author_mr_test", "Author MR", UserGitlabState.ACTIVE);
        var approver = testHelper.createAndSaveUserEntity(1702L, "approver_mr_test", "Approver MR", UserGitlabState.ACTIVE);

        var mr = testHelper.createAndSaveMergeRequestEntity(
                1901L, project.getId(), "Title MR Analytics",
                MergeRequestGitlabState.MERGED, MergeRequestStatus.SYNCED,
                author.getId(), 100, 20, Instant.now(), Instant.now(), approver.getId()
        );

        refreshService.refreshAllViews();

        // when
        var mrRows = storage.findAllMergeRequests();

        // then
        assertThat(mrRows).isNotEmpty();
        var foundRow = mrRows.stream()
                .filter(r -> mr.getId().equals(r.getMrId()))
                .findFirst();
        assertThat(foundRow).isPresent();
        assertThat(foundRow.get().getAuthorId()).isEqualTo(author.getId());
        assertThat(foundRow.get().getApproverId()).isEqualTo(approver.getId());
        assertThat(foundRow.get().getRowsAdded()).isEqualTo(100);
        assertThat(foundRow.get().getRowsDeleted()).isEqualTo(20);
        assertThat(foundRow.get().getTotalCode()).isEqualTo(120);
        assertThat(foundRow.get().getProjectName()).isEqualTo("Proj Analytics MR");
    }

    @Test
    void findAllComments_whenCommentsExist_thenReturnCommentRows() {
        // given
        var project = testHelper.createAndSaveProjectEntity(1802L, "Proj Analytics Comm", ProjectStatus.SYNCED);
        var author = testHelper.createAndSaveUserEntity(1703L, "author_comm_test", "Author Comm", UserGitlabState.ACTIVE);
        var reviewer = testHelper.createAndSaveUserEntity(1704L, "reviewer_comm_test", "Reviewer Comm", UserGitlabState.ACTIVE);

        var mr = testHelper.createAndSaveMergeRequestEntity(
                1902L, project.getId(), "Title MR Comm",
                MergeRequestGitlabState.MERGED, MergeRequestStatus.SYNCED,
                author.getId(), 50, 10, Instant.now()
        );
        var comment = testHelper.createAndSaveMergeRequestCommentEntity(1903L, mr.getId(), reviewer.getId(), "Review comment");

        refreshService.refreshAllViews();

        // when
        var comments = storage.findAllComments();

        // then
        assertThat(comments).isNotEmpty();
        var foundComment = comments.stream()
                .filter(c -> comment.getId().equals(c.getCommentId()))
                .findFirst();
        assertThat(foundComment).isPresent();
        assertThat(foundComment.get().getAuthorId()).isEqualTo(reviewer.getId());
        assertThat(foundComment.get().getMrAuthorId()).isEqualTo(author.getId());
        assertThat(foundComment.get().getMergeRequestId()).isEqualTo(mr.getId());
        assertThat(foundComment.get().getProjectName()).isEqualTo("Proj Analytics Comm");
    }

    @Test
    void findHourlyData_whenHourlyActivityExists_thenReturnHourlyRows() {
        // given
        var project = testHelper.createAndSaveProjectEntity(1803L, "Proj Analytics Hourly", ProjectStatus.SYNCED);
        var user = testHelper.createAndSaveUserEntity(1705L, "user_hourly_test", "User Hourly", UserGitlabState.ACTIVE);

        testHelper.createAndSaveMergeRequestEntity(
                1904L, project.getId(), "Title MR Hourly",
                MergeRequestGitlabState.MERGED, MergeRequestStatus.SYNCED,
                user.getId(), 10, 5, Instant.now()
        );

        refreshService.refreshAllViews();

        // when
        var hourlyRows = storage.findHourlyData(user.getId());

        // then
        assertThat(hourlyRows).isNotEmpty();
        var totalCount = hourlyRows.stream().mapToLong(io.cx.gitstats.domain.model.analytics.UserMrHourlyViewRow::getMrCount).sum();
        assertThat(totalCount).isGreaterThanOrEqualTo(1L);
    }
}
