package io.cx.gitstats.web.ui.components.analytics;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.cx.gitstats.domain.model.analytics.CodeContributionStats;
import io.cx.gitstats.domain.model.analytics.MrCommentsBySizePoint;
import io.cx.gitstats.domain.model.analytics.MrQuantityBySizePoint;
import io.cx.gitstats.domain.model.analytics.ReviewedAuthorPoint;
import io.cx.gitstats.domain.model.analytics.TimeCodeChangesPoint;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.function.ToLongFunction;

public class CodeContributionStatsComponent extends VerticalLayout {

    public CodeContributionStatsComponent(CodeContributionStats stats) {
        setWidthFull();
        setPadding(false);
        setSpacing(true);

        H3 sectionTitle = new H3("Code Contribution Statistics");
        sectionTitle.getStyle()
                .set("font-size", "1.1rem")
                .set("font-weight", "600")
                .set("margin", "16px 0 8px 0")
                .set("color", "#1f1e24");
        add(sectionTitle);

        if (stats == null) {
            add(new Span("No data available"));
            return;
        }

        add(createSummaryRow(stats));
        add(createChartsGrid(stats));
    }

    private HorizontalLayout createSummaryRow(CodeContributionStats stats) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setSpacing(true);

        long dayValRounded = Math.round(stats.getAverageCodePerWorkingDay());
        String dayVal = String.format("%,d lines/day", dayValRounded);

        long mrValRounded = Math.round(stats.getAverageCodePerMergeRequest());
        String mrVal = String.format("%,d lines/MR", mrValRounded);

        String commentsReceivedVal = String.format(Locale.US, "%.1f", stats.getCommentsReceivedPer1kLines());

        row.add(
                createCodeMetricCard("Total Code Added & Deleted", stats, "Overall period code volume"),
                createMetricCard("Avg Code / Working Day", dayVal, "Adjusted start: " + stats.getAdjustedPeriodStart()),
                createMetricCard("Avg Code / Merge Request", mrVal, "Based on merged MRs"),
                createMetricCard("Comments received", commentsReceivedVal, "Per 1,000 lines of code")
        );
        return row;
    }

    private Div createCodeMetricCard(String label, CodeContributionStats stats, String subtext) {
        Div container = new Div();
        container.getStyle()
                .set("display", "flex")
                .set("align-items", "baseline")
                .set("gap", "4px")
                .set("white-space", "nowrap");

        Span totalSpan = new Span(String.format("%,d", stats.getTotalCode()));
        totalSpan.getStyle()
                .set("color", "#1f1e24")
                .set("font-size", "1.1rem")
                .set("font-weight", "600");

        Span detailsSpan = new Span();
        detailsSpan.getStyle()
                .set("font-size", "0.78rem")
                .set("font-weight", "500")
                .set("white-space", "nowrap");

        Span openSpan = new Span("(");
        openSpan.getStyle().set("color", "#6e6d7a");

        Span addedSpan = new Span(String.format("+%,d", stats.getTotalAdded()));
        addedSpan.getStyle().set("color", "#2da44e");

        Span sepSpan = new Span(" / ");
        sepSpan.getStyle().set("color", "#6e6d7a");

        Span deletedSpan = new Span(String.format("-%,d", stats.getTotalDeleted()));
        deletedSpan.getStyle().set("color", "#cf222e");

        Span closeSpan = new Span(")");
        closeSpan.getStyle().set("color", "#6e6d7a");

        detailsSpan.add(openSpan, addedSpan, sepSpan, deletedSpan, closeSpan);
        container.add(totalSpan, detailsSpan);
        return createMetricCard(label, container, subtext);
    }

    private Div createMetricCard(String label, com.vaadin.flow.component.Component valueComponent, String subtext) {
        Div card = new Div();
        card.getStyle()
                .set("background", "#ffffff")
                .set("border", "1px solid #dcdcde")
                .set("border-radius", "8px")
                .set("padding", "14px 16px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "4px")
                .set("flex", "1")
                .set("box-sizing", "border-box");

        Span lSpan = new Span(label);
        lSpan.getStyle().set("font-size", "0.82rem").set("color", "#6e6d7a").set("font-weight", "500");

        Span sSpan = new Span(subtext);
        sSpan.getStyle().set("font-size", "0.75rem").set("color", "#8c8b96");

        card.add(lSpan, valueComponent, sSpan);
        return card;
    }

    private Div createMetricCard(String label, String value, String subtext) {
        Span vSpan = new Span(value);
        vSpan.getStyle().set("font-size", "1.1rem").set("font-weight", "600").set("color", "#1f1e24");
        return createMetricCard(label, vSpan, subtext);
    }

    private Div createChartsGrid(CodeContributionStats stats) {
        Div container = new Div();
        container.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "16px")
                .set("width", "100%")
                .set("margin-top", "8px");

        Div sizeMetricsGrid = new Div();
        sizeMetricsGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "1fr 1fr")
                .set("gap", "16px")
                .set("width", "100%");

        sizeMetricsGrid.add(
                createCardWrapper("MR quantity by size", buildMrQuantityBySize(stats.getMrQuantityBySize())),
                createCardWrapper("Received Comments by MR size (per 1,000 lines of code)", buildReceivedCommentsBySize(stats.getReceivedCommentsBySize()))
        );
        container.add(sizeMetricsGrid);

        Div reviewersGrid = new Div();
        reviewersGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "1fr 1fr")
                .set("gap", "16px")
                .set("width", "100%");

        reviewersGrid.add(
                createCardWrapper("Top Reviewers", buildTopReviewersChart(stats.getTopReviewers())),
                createCardWrapper("Top Reviewers (per 1,000 lines of code)", buildTopReviewersByRateChart(stats.getTopReviewers()))
        );
        container.add(reviewersGrid);

        container.add(createCardWrapper(
                "Code Changes",
                buildCodeChangesLineChart(stats.getStackedCodeChanges())
        ));

        return container;
    }

    private Div createCardWrapper(String title, Div content) {
        Div wrapper = new Div();
        wrapper.getStyle()
                .set("background", "#ffffff")
                .set("border", "1px solid #dcdcde")
                .set("border-radius", "8px")
                .set("padding", "16px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "12px")
                .set("width", "100%")
                .set("box-sizing", "border-box");

        H4 t = new H4(title);
        t.getStyle().set("margin", "0").set("font-size", "0.92rem").set("font-weight", "600").set("color", "#1f1e24");

        wrapper.add(t, content);
        return wrapper;
    }

    private Div buildMrQuantityBySize(List<MrQuantityBySizePoint> buckets) {
        if (buckets == null || buckets.isEmpty()) {
            return new DiscreteBarChartComponent(List.of(), "No data");
        }
        var items = buckets.stream()
                .map(b -> new DiscreteBarChartComponent.BarItem(
                        b.getBucketName(),
                        b.getPercentage(),
                        String.format("%d (%d%%)", b.getCount(), Math.round(b.getPercentage()))
                ))
                .toList();
        return new DiscreteBarChartComponent(items, "No data");
    }

    private Div buildReceivedCommentsBySize(List<MrCommentsBySizePoint> buckets) {
        if (buckets == null || buckets.isEmpty()) {
            return new DiscreteBarChartComponent(List.of(), "No data");
        }
        var items = buckets.stream()
                .map(b -> new DiscreteBarChartComponent.BarItem(
                        b.getBucketName(),
                        b.getRatioPercentage(),
                        String.format(Locale.US, "%.1f", b.getCommentsPer1kLines())
                ))
                .toList();
        return new DiscreteBarChartComponent(items, "No data");
    }

    private Div buildTopReviewersChart(List<ReviewedAuthorPoint> reviewers) {
        if (reviewers == null || reviewers.isEmpty()) {
            return new DiscreteBarChartComponent(List.of(), "No reviewers in period");
        }
        long maxCount = reviewers.stream().mapToLong(ReviewedAuthorPoint::getTotalComments).max().orElse(1L);
        if (maxCount <= 0) maxCount = 1L;

        long finalMaxCount = maxCount;
        var items = reviewers.stream()
                .limit(6)
                .map(r -> new DiscreteBarChartComponent.BarItem(
                        formatAuthorName(r),
                        (r.getTotalComments() / (double) finalMaxCount) * 100.0,
                        String.format("%,d", r.getTotalComments())
                ))
                .toList();
        return new DiscreteBarChartComponent(items, "No reviewers in period");
    }

    private Div buildTopReviewersByRateChart(List<ReviewedAuthorPoint> reviewers) {
        if (reviewers == null || reviewers.isEmpty()) {
            return new DiscreteBarChartComponent(List.of(), "No reviewers in period");
        }
        var sorted = reviewers.stream()
                .sorted(Comparator.comparingDouble(ReviewedAuthorPoint::getCommentsPer1kLines).reversed())
                .toList();

        double maxRate = sorted.stream().mapToDouble(ReviewedAuthorPoint::getCommentsPer1kLines).max().orElse(1.0);
        if (maxRate <= 0.0) maxRate = 1.0;

        double finalMaxRate = maxRate;
        var items = sorted.stream()
                .limit(6)
                .map(r -> new DiscreteBarChartComponent.BarItem(
                        formatAuthorName(r),
                        finalMaxRate > 0 ? (r.getCommentsPer1kLines() / finalMaxRate) * 100.0 : 0.0,
                        String.format(Locale.US, "%.1f", r.getCommentsPer1kLines())
                ))
                .toList();
        return new DiscreteBarChartComponent(items, "No reviewers in period");
    }

    private String formatAuthorName(ReviewedAuthorPoint r) {
        return r.getAuthorName() != null && !r.getAuthorName().isBlank()
                ? r.getAuthorName()
                : "@" + r.getAuthorUsername();
    }

    private Div buildCodeChangesLineChart(List<TimeCodeChangesPoint> points) {
        Div container = new Div();
        container.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "12px")
                .set("width", "100%");

        if (points == null || points.isEmpty()) {
            container.add(new Span("No data for period"));
            return container;
        }

        var aggregated = aggregatePoints(points);
        if (aggregated.isEmpty()) {
            container.add(new Span("No data for period"));
            return container;
        }

        container.add(createChartLegend());
        String svg = buildChartSvg(aggregated);

        Div svgWrapper = new Div();
        svgWrapper.setWidthFull();
        svgWrapper.getStyle().set("width", "100%").set("overflow", "hidden");
        svgWrapper.getElement().setProperty("innerHTML", svg);
        container.add(svgWrapper);

        return container;
    }

    private List<AggregatedPoint> aggregatePoints(List<TimeCodeChangesPoint> points) {
        if (points == null || points.isEmpty()) {
            return List.of();
        }
        var sorted = points.stream()
                .filter(p -> p.getDate() != null)
                .sorted(Comparator.comparing(TimeCodeChangesPoint::getDate))
                .toList();
        if (sorted.isEmpty()) {
            return List.of();
        }

        LocalDate minDate = sorted.get(0).getDate();
        LocalDate maxDate = sorted.get(sorted.size() - 1).getDate();
        long daysBetween = ChronoUnit.DAYS.between(minDate, maxDate) + 1;

        if (daysBetween <= 31) {
            return aggregateByBucket(sorted, TimeCodeChangesPoint::getDate, d -> d.format(DateTimeFormatter.ofPattern("dd.MM")));
        } else if (daysBetween <= 180) {
            return aggregateByBucket(sorted, p -> p.getDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), d -> d.format(DateTimeFormatter.ofPattern("dd.MM")));
        } else {
            return aggregateByBucket(sorted, p -> YearMonth.from(p.getDate()).atDay(1), d -> YearMonth.from(d).format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)));
        }
    }

    private List<AggregatedPoint> aggregateByBucket(
            List<TimeCodeChangesPoint> points,
            Function<TimeCodeChangesPoint, LocalDate> bucketKeyExtractor,
            Function<LocalDate, String> labelFormatter
    ) {
        Map<LocalDate, long[]> bucketMap = new LinkedHashMap<>();
        for (var p : points) {
            LocalDate key = bucketKeyExtractor.apply(p);
            long[] sums = bucketMap.computeIfAbsent(key, k -> new long[3]);
            sums[0] += p.getAdded();
            sums[1] += p.getDeleted();
            sums[2] += (p.getTotal() > 0 ? p.getTotal() : p.getAdded() + p.getDeleted());
        }

        List<AggregatedPoint> result = new ArrayList<>();
        for (var entry : bucketMap.entrySet()) {
            LocalDate date = entry.getKey();
            long[] sums = entry.getValue();
            result.add(new AggregatedPoint(labelFormatter.apply(date), date, sums[0], sums[1], sums[2]));
        }
        return result;
    }

    private String buildChartSvg(List<AggregatedPoint> points) {
        double width = 900.0;
        double height = 250.0;
        double leftPad = 65.0;
        double topPad = 20.0;
        double bottomPad = 65.0;
        double chartW = width - leftPad - 25.0;
        double chartH = height - topPad - bottomPad;

        long maxVal = points.stream()
                .mapToLong(p -> Math.max(p.total(), Math.max(p.added(), p.deleted())))
                .max()
                .orElse(10L);
        if (maxVal <= 0) {
            maxVal = 10L;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
                Locale.US,
                "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 %.0f %.0f' width='100%%' height='%.0f' style='overflow: visible; display: block;'>",
                width, height, height
        ));

        buildGridAndYAxis(sb, maxVal, leftPad, topPad, chartW, chartH);
        buildLineSeries(sb, points, AggregatedPoint::deleted, maxVal, leftPad, chartW, topPad, chartH, "#cf222e", "Deleted");
        buildLineSeries(sb, points, AggregatedPoint::added, maxVal, leftPad, chartW, topPad, chartH, "#2da44e", "Added");
        buildLineSeries(sb, points, AggregatedPoint::total, maxVal, leftPad, chartW, topPad, chartH, "#0969da", "Total");
        buildXAxisLabels(sb, points, leftPad, chartW, topPad + chartH);

        sb.append("</svg>");
        return sb.toString();
    }

    private void buildGridAndYAxis(
            StringBuilder sb, long maxVal, double leftPad,
            double topPad, double chartW, double chartH
    ) {
        int ticks = 4;
        for (int i = 0; i <= ticks; i++) {
            double ratio = (double) i / ticks;
            double y = topPad + chartH * (1.0 - ratio);
            long tickVal = Math.round(maxVal * ratio);

            sb.append(String.format(
                    Locale.US,
                    "<line x1='%.1f' y1='%.1f' x2='%.1f' y2='%.1f' stroke='%s' stroke-width='1'/>",
                    leftPad, y, leftPad + chartW, y, i == 0 ? "#dcdcde" : "#f0f2f5"
            ));
            sb.append(String.format(
                    Locale.US,
                    "<text x='%.1f' y='%.1f' text-anchor='end' font-size='11' fill='#6e6d7a' font-family='sans-serif'>%,d</text>",
                    leftPad - 8.0, y + 4.0, tickVal
            ));
        }
    }

    private void buildLineSeries(
            StringBuilder sb, List<AggregatedPoint> points,
            ToLongFunction<AggregatedPoint> valExtractor, long maxVal,
            double leftPad, double chartW, double topPad, double chartH,
            String color, String label
    ) {
        int n = points.size();
        if (n == 0) return;

        StringBuilder pathD = new StringBuilder();
        StringBuilder circles = new StringBuilder();

        for (int i = 0; i < n; i++) {
            var pt = points.get(i);
            long val = valExtractor.applyAsLong(pt);
            double x = n == 1 ? leftPad + chartW / 2.0 : leftPad + i * (chartW / (n - 1));
            double y = topPad + chartH * (1.0 - (double) val / maxVal);

            if (i == 0) {
                pathD.append(String.format(Locale.US, "M %.1f %.1f", x, y));
            } else {
                pathD.append(String.format(Locale.US, " L %.1f %.1f", x, y));
            }

            circles.append(String.format(
                    Locale.US,
                    "<circle cx='%.1f' cy='%.1f' r='3.5' fill='%s'>"
                            + "<title>%s: %s %,d (Total: %,d)</title></circle>",
                    x, y, color, pt.label(), label, val, pt.total()
            ));
        }

        sb.append(String.format(
                "<path d='%s' fill='none' stroke='%s' stroke-width='2.5' stroke-linecap='round' stroke-linejoin='round'/>",
                pathD, color
        ));
        sb.append(circles);
    }

    private void buildXAxisLabels(
            StringBuilder sb, List<AggregatedPoint> points,
            double leftPad, double chartW, double yPos
    ) {
        int n = points.size();
        if (n == 0) return;

        int step = n <= 24 ? 1 : (int) Math.ceil((double) n / 20.0);
        for (int i = 0; i < n; i++) {
            if (i % step == 0 || i == n - 1) {
                var pt = points.get(i);
                double x = n == 1 ? leftPad + chartW / 2.0 : leftPad + i * (chartW / (n - 1));
                sb.append(String.format(
                        Locale.US,
                        "<text x='%.1f' y='%.1f' transform='rotate(-90 %.1f %.1f)' text-anchor='end' dominant-baseline='central' font-size='11' fill='#6e6d7a' font-family='sans-serif'>%s</text>",
                        x, yPos + 8.0, x, yPos + 8.0, pt.label()
                ));
            }
        }
    }

    private HorizontalLayout createChartLegend() {
        HorizontalLayout legend = new HorizontalLayout();
        legend.setWidthFull();
        legend.setSpacing(true);
        legend.getStyle().set("gap", "16px").set("margin-bottom", "4px").set("font-size", "0.82rem");

        legend.add(
                createLegendItem("#2da44e", "Added"),
                createLegendItem("#cf222e", "Deleted"),
                createLegendItem("#0969da", "Total (Added + Deleted)")
        );
        return legend;
    }

    private Div createLegendItem(String color, String text) {
        Div item = new Div();
        item.getStyle().set("display", "flex").set("align-items", "center").set("gap", "6px");

        Div dot = new Div();
        dot.getStyle()
                .set("width", "12px")
                .set("height", "4px")
                .set("border-radius", "2px")
                .set("background", color);

        Span label = new Span(text);
        label.getStyle().set("color", "#6e6d7a").set("font-weight", "500");

        item.add(dot, label);
        return item;
    }

    private record AggregatedPoint(
            String label,
            LocalDate date,
            long added,
            long deleted,
            long total
    ) {}
}
