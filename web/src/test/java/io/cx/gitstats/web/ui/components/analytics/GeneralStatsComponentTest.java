package io.cx.gitstats.web.ui.components.analytics;

import io.cx.gitstats.domain.model.analytics.GeneralStats;
import io.cx.gitstats.domain.model.analytics.PercentileRankBadge;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeneralStatsComponentTest {

    @Test
    void generalStatsComponent_whenStatsNull_thenComponentConstructed() {
        // given
        GeneralStats stats = null;

        // when
        var component = new GeneralStatsComponent(stats);

        // then
        assertThat(component.getChildren()).isNotEmpty();
    }

    @Test
    void generalStatsComponent_whenValidStats_thenComponentConstructedWithCards() {
        // given
        var codeRank = PercentileRankBadge.builder()
                .percentile(63)
                .userValue(1500)
                .rowsAdded(1200L)
                .rowsDeleted(300L)
                .averageValue(1000.4)
                .comparisonWithAveragePercent(23.6)
                .build();
        var mrRank = PercentileRankBadge.builder()
                .percentile(27)
                .userValue(15)
                .averageValue(10.2)
                .comparisonWithAveragePercent(-5.3)
                .build();
        var reviewRank = PercentileRankBadge.builder()
                .percentile(95)
                .userValue(40)
                .averageValue(20.0)
                .comparisonWithAveragePercent(100.0)
                .build();
        var stats = GeneralStats.builder()
                .codeContributionRank(codeRank)
                .mergedMrsRank(mrRank)
                .reviewActivityRank(reviewRank)
                .build();

        // when
        var component = new GeneralStatsComponent(stats);

        // then
        assertThat(component.getChildren()).isNotEmpty();
    }

    @Test
    void generalStatsComponent_whenConstructedWithListener_thenComponentConstructed() {
        // given
        var triggered = new boolean[1];
        GeneralStatsComponent.AiReviewChangeListener listener = isAi -> triggered[0] = isAi;

        // when
        var component = new GeneralStatsComponent(null, true, listener);

        // then
        assertThat(component.getChildren()).isNotEmpty();
    }
}
