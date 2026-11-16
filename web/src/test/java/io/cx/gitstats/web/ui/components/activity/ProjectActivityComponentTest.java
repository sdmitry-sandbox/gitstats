package io.cx.gitstats.web.ui.components.activity;

import io.cx.gitstats.domain.model.DailyContribution;
import io.cx.gitstats.domain.model.Project;
import io.cx.gitstats.domain.model.ProjectStatus;
import io.cx.gitstats.domain.model.activity.UserActivityItem;
import io.cx.gitstats.domain.model.activity.UserActivityType;
import io.cx.gitstats.domain.service.ProjectActivityService;
import io.cx.gitstats.domain.service.ProjectContributionService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectActivityComponentTest {

    private static class FakeProjectActivityService implements ProjectActivityService {
        private final List<UserActivityItem> items;

        FakeProjectActivityService(List<UserActivityItem> items) {
            this.items = items;
        }

        @Override
        public List<UserActivityItem> getProjectActivities(
                Long projectId,
                LocalDate from,
                LocalDate to,
                boolean includeMr,
                boolean includeApprovals,
                boolean includeComments
        ) {
            return items;
        }

        @Override
        public List<UserActivityItem> getProjectActivities(
                Long projectId,
                LocalDate from,
                LocalDate to,
                boolean includeMr,
                boolean includeApprovals,
                boolean includeComments,
                int limit
        ) {
            return items;
        }
    }

    private static class FakeProjectContributionService implements ProjectContributionService {
        @Override
        public List<DailyContribution> getProjectDailyContributions(Long projectId) {
            return List.of();
        }
    }

    @Test
    void projectActivityComponent_whenEmptyActivities_thenConstructedSuccessfully() {
        // given
        var project = createProject();
        var contributionService = new FakeProjectContributionService();
        var service = new FakeProjectActivityService(List.of());

        // when
        var component = new ProjectActivityComponent(
                project,
                contributionService,
                service,
                List.of(),
                null,
                null,
                true,
                true,
                true
        );

        // then
        assertThat(component).isNotNull();
        assertThat(component.getChildren().count()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void projectActivityComponent_whenMergeRequestActivity_thenConstructedWithItems() {
        // given
        var project = createProject();
        var contributionService = new FakeProjectContributionService();
        var item = UserActivityItem.builder()
                .id(1L)
                .gitlabId(101L)
                .type(UserActivityType.MERGE_REQUEST)
                .mergeRequestId(1L)
                .mrGitlabId(101L)
                .mrTitle("Add project page")
                .mrWebUrl("https://gitlab.com/mr/101")
                .projectId(10L)
                .projectName("GitStats")
                .userId(1L)
                .username("john.doe")
                .activityTime(Instant.now().minusSeconds(3600))
                .contributionDate(LocalDate.now())
                .rowsAdded(150)
                .rowsDeleted(30)
                .build();
        var service = new FakeProjectActivityService(List.of(item));

        // when
        var component = new ProjectActivityComponent(
                project,
                contributionService,
                service,
                List.of(),
                null,
                null,
                true,
                false,
                false
        );

        // then
        assertThat(component).isNotNull();
    }

    @Test
    void projectActivityComponent_whenApprovalWithReactionTime_thenConstructed() {
        // given
        var project = createProject();
        var contributionService = new FakeProjectContributionService();
        var item = UserActivityItem.builder()
                .id(2L)
                .gitlabId(202L)
                .type(UserActivityType.APPROVAL)
                .mergeRequestId(2L)
                .mrGitlabId(202L)
                .mrTitle("Fix layout issue")
                .mrWebUrl("https://gitlab.com/mr/202")
                .projectId(10L)
                .projectName("GitStats")
                .userId(2L)
                .username("jane.doe")
                .activityTime(Instant.now().minusSeconds(7200))
                .contributionDate(LocalDate.now())
                .reactionTime("1h 30m")
                .build();
        var service = new FakeProjectActivityService(List.of(item));

        // when
        var component = new ProjectActivityComponent(
                project,
                contributionService,
                service,
                List.of(),
                null,
                null,
                false,
                true,
                false
        );

        // then
        assertThat(component).isNotNull();
    }

    @Test
    void projectActivityComponent_whenCommentWithReactionTimeAndMessage_thenConstructed() {
        // given
        var project = createProject();
        var contributionService = new FakeProjectContributionService();
        var item = UserActivityItem.builder()
                .id(3L)
                .gitlabId(303L)
                .type(UserActivityType.COMMENT)
                .mergeRequestId(3L)
                .mrGitlabId(303L)
                .mrTitle("Refactor controllers")
                .mrWebUrl("https://gitlab.com/mr/303")
                .projectId(10L)
                .projectName("GitStats")
                .userId(3L)
                .username("alex.smith")
                .activityTime(Instant.now().minusSeconds(1800))
                .contributionDate(LocalDate.now())
                .reactionTime("15m")
                .commentMessage("Looks great to me!")
                .build();
        var service = new FakeProjectActivityService(List.of(item));

        // when
        var component = new ProjectActivityComponent(
                project,
                contributionService,
                service,
                List.of(),
                null,
                null,
                false,
                false,
                true
        );

        // then
        assertThat(component).isNotNull();
    }

    @Test
    void formatRelativeTime_whenInstantNull_thenEmptyString() {
        // given
        Instant instant = null;

        // when
        var formatted = ProjectActivityComponent.formatRelativeTime(instant);

        // then
        assertThat(formatted).isEmpty();
    }

    @Test
    void formatRelativeTime_whenRecent_thenJustNow() {
        // given
        var now = Instant.now().minusSeconds(10);

        // when
        var formatted = ProjectActivityComponent.formatRelativeTime(now);

        // then
        assertThat(formatted).isEqualTo("just now");
    }

    @Test
    void formatMoscowDateTime_whenInstantNull_thenEmptyString() {
        // given
        Instant instant = null;

        // when
        var formatted = ProjectActivityComponent.formatMoscowDateTime(instant);

        // then
        assertThat(formatted).isEmpty();
    }

    @Test
    void formatMoscowDateTime_whenValidInstant_thenFormatted() {
        // given
        var instant = Instant.parse("2026-11-23T10:15:30Z");

        // when
        var formatted = ProjectActivityComponent.formatMoscowDateTime(instant);

        // then
        assertThat(formatted).isEqualTo("2026-11-23 13:15:30");
    }

    private Project createProject() {
        return Project.builder()
                .id(10L)
                .gitlabId(1001L)
                .name("GitStats")
                .status(ProjectStatus.SYNCED)
                .webUrl("https://gitlab.com/cx/gitstats")
                .build();
    }
}
