package io.cx.gitstats.web.ui.components.analytics;

import io.cx.gitstats.domain.model.analytics.CodeReviewStats;
import io.cx.gitstats.domain.model.analytics.MrCommentsBySizePoint;
import io.cx.gitstats.domain.model.analytics.ReviewedAuthorPoint;
import io.cx.gitstats.domain.model.analytics.TimeReviewActivityPoint;
import io.cx.gitstats.domain.model.analytics.TtfrBySizePoint;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CodeReviewStatsComponentTest {

    @Test
    void codeReviewStatsComponent_whenStatsNull_thenComponentConstructed() {
        // given
        CodeReviewStats stats = null;

        // when
        var component = new CodeReviewStatsComponent(stats);

        // then
        assertThat(component.getChildren()).isNotEmpty();
    }

    @Test
    void codeReviewStatsComponent_whenValidStats_thenComponentConstructedWithCards() {
        // given
        var stats = createTestStats(List.of(TimeReviewActivityPoint.builder()
                .date(LocalDate.of(2026, 1, 1))
                .approvals(5)
                .comments(10)
                .total(15)
                .build()));

        // when
        var component = new CodeReviewStatsComponent(stats);

        // then
        assertThat(component.getChildren()).isNotEmpty();
    }

    @Test
    void codeReviewStatsComponent_whenEmptyActivity_thenComponentConstructed() {
        // given
        var stats = createTestStats(List.of());

        // when
        var component = new CodeReviewStatsComponent(stats);

        // then
        assertThat(component.getChildren()).isNotEmpty();
    }

    @Test
    void codeReviewStatsComponent_whenWeeklyPeriod_thenComponentConstructed() {
        // given
        var points = List.of(
                TimeReviewActivityPoint.builder().date(LocalDate.of(2026, 1, 1)).approvals(2).comments(4).total(6).build(),
                TimeReviewActivityPoint.builder().date(LocalDate.of(2026, 2, 15)).approvals(3).comments(7).total(10).build(),
                TimeReviewActivityPoint.builder().date(LocalDate.of(2026, 3, 1)).approvals(1).comments(5).total(6).build()
        );
        var stats = createTestStats(points);

        // when
        var component = new CodeReviewStatsComponent(stats);

        // then
        assertThat(component.getChildren()).isNotEmpty();
    }

    @Test
    void codeReviewStatsComponent_whenMonthlyPeriod_thenComponentConstructed() {
        // given
        var points = List.of(
                TimeReviewActivityPoint.builder().date(LocalDate.of(2025, 1, 1)).approvals(10).comments(20).total(30).build(),
                TimeReviewActivityPoint.builder().date(LocalDate.of(2025, 5, 15)).approvals(15).comments(35).total(50).build(),
                TimeReviewActivityPoint.builder().date(LocalDate.of(2025, 10, 1)).approvals(8).comments(18).total(26).build()
        );
        var stats = createTestStats(points);

        // when
        var component = new CodeReviewStatsComponent(stats);

        // then
        assertThat(component.getChildren()).isNotEmpty();
    }

    private CodeReviewStats createTestStats(List<TimeReviewActivityPoint> activity) {
        return CodeReviewStats.builder()
                .totalApprovals(25)
                .totalComments(60)
                .averageCommentsPer1000Lines(8.4)
                .averageReactionTimeHours(1.5)
                .ttfrByMrSize(List.of(
                        TtfrBySizePoint.builder().bucketName("0–50").averageHours(0.5).formattedTime("00:30").barPercentage(100.0).build(),
                        TtfrBySizePoint.builder().bucketName("50–200").averageHours(2.0).formattedTime("02:00").barPercentage(75.0).build(),
                        TtfrBySizePoint.builder().bucketName("200–600").averageHours(4.0).formattedTime("04:00").barPercentage(50.0).build()
                ))
                .producedCommentsBySize(List.of(
                        MrCommentsBySizePoint.builder().bucketName("0–50").commentsPer1kLines(12.0).ratioPercentage(100.0).build(),
                        MrCommentsBySizePoint.builder().bucketName("50–200").commentsPer1kLines(6.0).ratioPercentage(50.0).build()
                ))
                .reviewedAuthors(List.of(
                        ReviewedAuthorPoint.builder().authorId(101L).authorUsername("author1").authorName("Author One").totalComments(30L).commentsPer1kLines(15.0).build(),
                        ReviewedAuthorPoint.builder().authorId(102L).authorUsername("author2").authorName("Author Two").totalComments(15L).commentsPer1kLines(20.0).build()
                ))
                .reviewActivityOverTime(activity)
                .build();
    }
}
