package io.cx.gitstats.web.ui.components.analytics;

import io.cx.gitstats.domain.model.analytics.RadarChartStats;
import io.cx.gitstats.domain.model.analytics.RadarMetricItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RadarChartComponentTest {

    @Test
    void radarChartComponent_whenStatsNull_thenEmptyComponent() {
        // given
        RadarChartStats stats = null;

        // when
        var component = new RadarChartComponent(stats);

        // then
        assertThat(component.getChildren()).isEmpty();
    }

    @Test
    void radarChartComponent_whenEmptyMetrics_thenEmptyComponent() {
        // given
        var stats = RadarChartStats.builder()
                .allMetrics(List.of())
                .build();

        // when
        var component = new RadarChartComponent(stats);

        // then
        assertThat(component.getChildren()).isEmpty();
    }

    @Test
    void radarChartComponent_whenValidStats_thenComponentConstructedWithCards() {
        // given
        var m1 = createMetric("Code written", 1000, 800, 70, "1,000 lines", "800 lines");
        var m2 = createMetric("Merged MRs", 10, 8, 60, "10", "8.0");
        var m3 = createMetric("Received comments", 5.0, 10.0, 80, "5.0 / 1k loc", "10.0 / 1k loc");
        var m4 = createMetric("Avg MR size", 350, 420, 90, "350 lines", "420 lines");

        var m5 = createMetric("MR Approvals", 25, 20, 65, "25", "20.0");
        var m6 = createMetric("Reaction Time", 1.5, 3.0, 85, "01:30", "03:00");
        var m7 = createMetric("Produced comments", 12.0, 8.0, 75, "12.0 / 1k loc", "8.0 / 1k loc");
        var m8 = createMetric("Review density by MR size", 80.0, 60.0, 70, "80%", "60%");

        var left = List.of(m1, m2, m3, m4);
        var right = List.of(m5, m6, m7, m8);
        var all = List.of(m1, m2, m3, m4, m5, m6, m7, m8);

        var stats = RadarChartStats.builder()
                .contributionMetrics(left)
                .reviewMetrics(right)
                .allMetrics(all)
                .build();

        // when
        var component = new RadarChartComponent(stats);

        // then
        assertThat(component.getChildren()).isNotEmpty();
    }

    private RadarMetricItem createMetric(
            String name,
            double userVal,
            double avgVal,
            int score,
            String fmtUser,
            String fmtAvg
    ) {
        return RadarMetricItem.builder()
                .name(name)
                .userValue(userVal)
                .avgValue(avgVal)
                .scorePercent(score)
                .formattedUserValue(fmtUser)
                .formattedAvgValue(fmtAvg)
                .description("Test description for metric")
                .build();
    }
}
