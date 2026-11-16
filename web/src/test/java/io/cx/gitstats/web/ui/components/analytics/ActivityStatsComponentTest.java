package io.cx.gitstats.web.ui.components.analytics;

import io.cx.gitstats.domain.model.analytics.ActivityStats;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityStatsComponentTest {

    @Test
    void activityStatsComponent_whenStatsNull_thenComponentConstructed() {
        // given
        ActivityStats stats = null;

        // when
        var component = new ActivityStatsComponent(stats);

        // then
        assertThat(component.getChildren()).isNotEmpty();
    }

    @Test
    void activityStatsComponent_whenValidStats_thenComponentConstructedWithCards() {
        // given
        var stats = ActivityStats.builder()
                .heatmapByDayOfWeek(Map.of(DayOfWeek.MONDAY, 10L, DayOfWeek.TUESDAY, 5L))
                .heatmapByHourOfDay(Map.of(10, 3L, 14, 7L))
                .build();

        // when
        var component = new ActivityStatsComponent(stats);

        // then
        assertThat(component.getChildren()).isNotEmpty();
    }

    @Test
    void activityStatsComponent_whenEmptyMaps_thenComponentConstructed() {
        // given
        var stats = ActivityStats.builder()
                .heatmapByDayOfWeek(Map.of())
                .heatmapByHourOfDay(Map.of())
                .build();

        // when
        var component = new ActivityStatsComponent(stats);

        // then
        assertThat(component.getChildren()).isNotEmpty();
    }
}
