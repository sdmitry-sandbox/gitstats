package io.cx.gitstats.web.ui.components.analytics;

import io.cx.gitstats.domain.model.analytics.ProjectUserCodeStats;
import io.cx.gitstats.domain.model.analytics.ProjectUserCountStats;
import io.cx.gitstats.domain.model.analytics.ProjectUserStats;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectUserStatsComponentTest {

    @Test
    void projectUserStatsComponent_whenNullStats_thenConstructed() {
        // given
        ProjectUserStats stats = null;

        // when
        var component = new ProjectUserStatsComponent(stats);

        // then
        assertThat(component).isNotNull();
    }

    @Test
    void projectUserStatsComponent_whenEmptyLists_thenConstructed() {
        // given
        var stats = ProjectUserStats.builder()
                .codeUsers(List.of())
                .approvedUsers(List.of())
                .commentedUsers(List.of())
                .build();

        // when
        var component = new ProjectUserStatsComponent(stats);

        // then
        assertThat(component).isNotNull();
    }

    @Test
    void projectUserStatsComponent_whenPopulatedStats_thenConstructed() {
        // given
        var stats = createTestStats();

        // when
        var component = new ProjectUserStatsComponent(stats);

        // then
        assertThat(component).isNotNull();
    }

    private ProjectUserStats createTestStats() {
        var codeUser = ProjectUserCodeStats.builder()
                .userId(1L)
                .username("john.doe")
                .name("John Doe")
                .rowsAdded(100)
                .rowsDeleted(20)
                .totalCode(120)
                .percentageOfTotal(60.0)
                .build();

        var approvedUser = ProjectUserCountStats.builder()
                .userId(2L)
                .username("jane.doe")
                .name("Jane Doe")
                .count(5)
                .percentageOfTotal(100.0)
                .build();

        var commentedUser = ProjectUserCountStats.builder()
                .userId(1L)
                .username("john.doe")
                .name("John Doe")
                .count(10)
                .percentageOfTotal(100.0)
                .build();

        return ProjectUserStats.builder()
                .codeUsers(List.of(codeUser))
                .approvedUsers(List.of(approvedUser))
                .commentedUsers(List.of(commentedUser))
                .build();
    }
}
