package io.cx.gitstats.web.ui.components.analytics;

import io.cx.gitstats.domain.model.analytics.CodeContributionStats;
import io.cx.gitstats.domain.model.analytics.MrCommentsBySizePoint;
import io.cx.gitstats.domain.model.analytics.MrQuantityBySizePoint;
import io.cx.gitstats.domain.model.analytics.ReviewedAuthorPoint;
import io.cx.gitstats.domain.model.analytics.TimeCodeChangesPoint;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CodeContributionStatsComponentTest {

    @Test
    void codeContributionStatsComponent_whenStatsNull_thenComponentConstructed() {
        // given
        CodeContributionStats stats = null;

        // when
        var component = new CodeContributionStatsComponent(stats);

        // then
        assertThat(component.getChildren()).isNotEmpty();
    }

    @Test
    void codeContributionStatsComponent_whenValidStats_thenComponentConstructedWithCards() {
        // given
        var stats = createTestStats(List.of(TimeCodeChangesPoint.builder()
                .date(LocalDate.of(2026, 1, 1))
                .added(100)
                .deleted(20)
                .total(120)
                .build()));

        // when
        var component = new CodeContributionStatsComponent(stats);

        // then
        assertThat(component.getChildren()).isNotEmpty();
    }

    @Test
    void codeContributionStatsComponent_whenEmptyChanges_thenComponentConstructed() {
        // given
        var stats = createTestStats(List.of());

        // when
        var component = new CodeContributionStatsComponent(stats);

        // then
        assertThat(component.getChildren()).isNotEmpty();
    }

    @Test
    void codeContributionStatsComponent_whenWeeklyPeriod_thenComponentConstructed() {
        // given
        var points = List.of(
                TimeCodeChangesPoint.builder().date(LocalDate.of(2026, 1, 1)).added(100).deleted(20).total(120).build(),
                TimeCodeChangesPoint.builder().date(LocalDate.of(2026, 2, 15)).added(200).deleted(50).total(250).build(),
                TimeCodeChangesPoint.builder().date(LocalDate.of(2026, 3, 1)).added(150).deleted(30).total(180).build()
        );
        var stats = createTestStats(points);

        // when
        var component = new CodeContributionStatsComponent(stats);

        // then
        assertThat(component.getChildren()).isNotEmpty();
    }

    @Test
    void codeContributionStatsComponent_whenMonthlyPeriod_thenComponentConstructed() {
        // given
        var points = List.of(
                TimeCodeChangesPoint.builder().date(LocalDate.of(2025, 1, 1)).added(1000).deleted(200).total(1200).build(),
                TimeCodeChangesPoint.builder().date(LocalDate.of(2025, 5, 15)).added(2000).deleted(500).total(2500).build(),
                TimeCodeChangesPoint.builder().date(LocalDate.of(2025, 10, 1)).added(1500).deleted(300).total(1800).build()
        );
        var stats = createTestStats(points);

        // when
        var component = new CodeContributionStatsComponent(stats);

        // then
        assertThat(component.getChildren()).isNotEmpty();
    }

    private CodeContributionStats createTestStats(List<TimeCodeChangesPoint> changes) {
        return CodeContributionStats.builder()
                .totalCode(2500)
                .totalAdded(2000)
                .totalDeleted(500)
                .averageCodePerWorkingDay(123.4)
                .averageCodePerMergeRequest(456.7)
                .adjustedPeriodStart(LocalDate.of(2026, 1, 1))
                .stackedCodeChanges(changes)
                .mrQuantityBySize(List.of(
                        MrQuantityBySizePoint.builder().bucketName("0–50").count(5).percentage(50.0).build(),
                        MrQuantityBySizePoint.builder().bucketName("50–200").count(3).percentage(30.0).build(),
                        MrQuantityBySizePoint.builder().bucketName("200–600").count(2).percentage(20.0).build()
                ))
                .receivedCommentsBySize(List.of(
                        MrCommentsBySizePoint.builder().bucketName("0–50").commentsPer1kLines(15.0).ratioPercentage(100.0).build(),
                        MrCommentsBySizePoint.builder().bucketName("50–200").commentsPer1kLines(7.5).ratioPercentage(50.0).build()
                ))
                .commentsReceivedPer1kLines(12.5)
                .topReviewers(List.of(
                        ReviewedAuthorPoint.builder().authorId(1L).authorUsername("reviewer1").authorName("Reviewer 1").totalComments(10L).commentsPer1kLines(25.0).build(),
                        ReviewedAuthorPoint.builder().authorId(2L).authorUsername("reviewer2").authorName("Reviewer 2").totalComments(5L).commentsPer1kLines(10.0).build()
                ))
                .build();
    }
}
