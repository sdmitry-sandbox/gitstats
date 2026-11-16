package io.cx.gitstats.web.ui.components.analytics;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.cx.gitstats.domain.model.analytics.CodeReviewStats;
import io.cx.gitstats.domain.model.analytics.MrCommentsBySizePoint;
import io.cx.gitstats.domain.model.analytics.ReviewedAuthorPoint;
import io.cx.gitstats.domain.model.analytics.TimeReviewActivityPoint;
import io.cx.gitstats.domain.model.analytics.TtfrBySizePoint;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;

public class CodeReviewStatsComponent extends VerticalLayout {

    public CodeReviewStatsComponent(CodeReviewStats stats) {
        setWidthFull();
        setPadding(false);
        setSpacing(true);

        H3 sectionTitle = new H3("Code Review Statistics");
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

    private HorizontalLayout createSummaryRow(CodeReviewStats stats) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setSpacing(true);

        String reactText = formatHoursToHhMm(stats.getAverageReactionTimeHours());
        String commentsProducedVal = String.format(Locale.US, "%.1f", stats.getAverageCommentsPer1000Lines());

        row.add(
                createMetricCard("MR Approvals", String.format("%,d", stats.getTotalApprovals()), "Approved merge requests"),
                createMetricCard("MR Comments", String.format("%,d", stats.getTotalComments()), "Total review comments"),
                createMetricCard("Reaction Time", reactText, "Work hours (10:00-19:00, <=8h)"),
                createMetricCard("Comments produced", commentsProducedVal, "Per 1,000 lines of code")
        );
        return row;
    }

    private Div createMetricCard(String label, String value, String subtext) {
        Div card = new Div();
        card.getStyle()
                .set("background", "#ffffff")
                .set("border", "1px solid #dcdcde")
                .set("border-radius", "8px")
                .set("padding", "14px 16px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "4px")
                .set("flex", "1");

        Span lSpan = new Span(label);
        lSpan.getStyle().set("font-size", "0.82rem").set("color", "#6e6d7a").set("font-weight", "500");

        Span vSpan = new Span(value);
        vSpan.getStyle().set("font-size", "1.1rem").set("font-weight", "600").set("color", "#1f1e24");

        Span sSpan = new Span(subtext);
        sSpan.getStyle().set("font-size", "0.75rem").set("color", "#8c8b96");

        card.add(lSpan, vSpan, sSpan);
        return card;
    }

    private Div createChartsGrid(CodeReviewStats stats) {
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
                createCardWrapper("Time to First Response by MR size", buildTtfrBySizeChart(stats.getTtfrByMrSize())),
                createCardWrapper("Produced Comments by MR size (per 1,000 lines of code)", buildProducedCommentsBySizeChart(stats.getProducedCommentsBySize()))
        );
        container.add(sizeMetricsGrid);

        Div authorsGrid = new Div();
        authorsGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "1fr 1fr")
                .set("gap", "16px")
                .set("width", "100%");

        authorsGrid.add(
                createCardWrapper("Top Reviewed Authors", buildReviewedAuthorsChart(stats.getReviewedAuthors())),
                createCardWrapper("Top Reviewed Authors (per 1,000 lines of code)", buildReviewedAuthorsByRateChart(stats.getReviewedAuthors()))
        );
        container.add(authorsGrid);

        container.add(createCardWrapper(
                "Approvals + Comments",
                buildReviewActivityLineChart(stats.getReviewActivityOverTime())
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
                .set("gap", "12px");

        H4 t = new H4(title);
        t.getStyle().set("margin", "0").set("font-size", "0.92rem").set("font-weight", "600").set("color", "#1f1e24");

        wrapper.add(t, content);
        return wrapper;
    }

    private Div buildTtfrBySizeChart(List<TtfrBySizePoint> points) {
        if (points == null || points.isEmpty()) {
            return new DiscreteBarChartComponent(List.of(), "No data");
        }
        var items = points.stream()
                .map(p -> new DiscreteBarChartComponent.BarItem(
                        p.getBucketName(),
                        p.getBarPercentage(),
                        p.getFormattedTime() != null ? p.getFormattedTime() : "—"
                ))
                .toList();
        return new DiscreteBarChartComponent(items, "No data");
    }

    private Div buildProducedCommentsBySizeChart(List<MrCommentsBySizePoint> buckets) {
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

    private Div buildReviewedAuthorsChart(List<ReviewedAuthorPoint> authors) {
        if (authors == null || authors.isEmpty()) {
            return new DiscreteBarChartComponent(List.of(), "No reviewed authors in period");
        }
        long maxCount = authors.stream().mapToLong(ReviewedAuthorPoint::getTotalComments).max().orElse(1L);
        if (maxCount <= 0) maxCount = 1L;

        long finalMaxCount = maxCount;
        var items = authors.stream()
                .limit(6)
                .map(a -> new DiscreteBarChartComponent.BarItem(
                        formatAuthorName(a),
                        (a.getTotalComments() / (double) finalMaxCount) * 100.0,
                        String.format("%,d", a.getTotalComments())
                ))
                .toList();
        return new DiscreteBarChartComponent(items, "No reviewed authors in period");
    }

    private Div buildReviewedAuthorsByRateChart(List<ReviewedAuthorPoint> authors) {
        if (authors == null || authors.isEmpty()) {
            return new DiscreteBarChartComponent(List.of(), "No reviewed authors in period");
        }
        var sorted = authors.stream()
                .sorted(Comparator.comparingDouble(ReviewedAuthorPoint::getCommentsPer1kLines).reversed())
                .toList();

        double maxRate = sorted.stream().mapToDouble(ReviewedAuthorPoint::getCommentsPer1kLines).max().orElse(1.0);
        if (maxRate <= 0.0) maxRate = 1.0;

        double finalMaxRate = maxRate;
        var items = sorted.stream()
                .limit(6)
                .map(a -> new DiscreteBarChartComponent.BarItem(
                        formatAuthorName(a),
                        finalMaxRate > 0 ? (a.getCommentsPer1kLines() / finalMaxRate) * 100.0 : 0.0,
                        String.format(Locale.US, "%.1f", a.getCommentsPer1kLines())
                ))
                .toList();
        return new DiscreteBarChartComponent(items, "No reviewed authors in period");
    }

    private String formatAuthorName(ReviewedAuthorPoint a) {
        return a.getAuthorName() != null && !a.getAuthorName().isBlank()
                ? a.getAuthorName()
                : "@" + a.getAuthorUsername();
    }

    private Div buildReviewActivityLineChart(List<TimeReviewActivityPoint> points) {
        Div container = new Div();
        container.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "12px").set("width", "100%");

        if (points == null || points.isEmpty()) {
            container.add(new Span("No data for period"));
            return container;
        }

        var aggregated = aggregateReviewPoints(points);
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

    private List<AggregatedReviewPoint> aggregateReviewPoints(List<TimeReviewActivityPoint> points) {
        var sorted = points.stream()
                .filter(p -> p.getDate() != null)
                .sorted(Comparator.comparing(TimeReviewActivityPoint::getDate))
                .toList();
        if (sorted.isEmpty()) {
            return List.of();
        }

        LocalDate minDate = sorted.get(0).getDate();
        LocalDate maxDate = sorted.get(sorted.size() - 1).getDate();
        long daysBetween = ChronoUnit.DAYS.between(minDate, maxDate) + 1;

        if (daysBetween <= 31) {
            return aggregateReviewByBucket(sorted, TimeReviewActivityPoint::getDate, d -> d.format(DateTimeFormatter.ofPattern("dd.MM")));
        } else if (daysBetween <= 180) {
            return aggregateReviewByBucket(sorted, p -> p.getDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), d -> d.format(DateTimeFormatter.ofPattern("dd.MM")));
        } else {
            return aggregateReviewByBucket(sorted, p -> YearMonth.from(p.getDate()).atDay(1), d -> YearMonth.from(d).format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)));
        }
    }

    private List<AggregatedReviewPoint> aggregateReviewByBucket(
            List<TimeReviewActivityPoint> points,
            Function<TimeReviewActivityPoint, LocalDate> bucketKeyExtractor,
            Function<LocalDate, String> labelFormatter
    ) {
        Map<LocalDate, Long> bucketMap = new LinkedHashMap<>();
        for (var p : points) {
            LocalDate key = bucketKeyExtractor.apply(p);
            long total = p.getTotal() > 0 ? p.getTotal() : (p.getApprovals() + p.getComments());
            bucketMap.merge(key, total, Long::sum);
        }

        List<AggregatedReviewPoint> result = new ArrayList<>();
        for (var entry : bucketMap.entrySet()) {
            LocalDate date = entry.getKey();
            result.add(new AggregatedReviewPoint(labelFormatter.apply(date), date, entry.getValue()));
        }
        return result;
    }

    private String buildChartSvg(List<AggregatedReviewPoint> points) {
        double width = 900.0;
        double height = 250.0;
        double leftPad = 65.0;
        double topPad = 20.0;
        double bottomPad = 65.0;
        double chartW = width - leftPad - 25.0;
        double chartH = height - topPad - bottomPad;

        long maxVal = points.stream().mapToLong(AggregatedReviewPoint::total).max().orElse(10L);
        if (maxVal <= 0) maxVal = 10L;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
                Locale.US,
                "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 %.0f %.0f' width='100%%' height='%.0f' style='overflow: visible; display: block;'>",
                width, height, height
        ));

        buildGridAndYAxis(sb, maxVal, leftPad, topPad, chartW, chartH);
        buildSingleLineSeries(sb, points, maxVal, leftPad, chartW, topPad, chartH, "#0969da", "Approvals + Comments");
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

    private void buildSingleLineSeries(
            StringBuilder sb, List<AggregatedReviewPoint> points,
            long maxVal, double leftPad, double chartW, double topPad, double chartH,
            String color, String label
    ) {
        int n = points.size();
        if (n == 0) return;

        StringBuilder pathD = new StringBuilder();
        StringBuilder circles = new StringBuilder();

        for (int i = 0; i < n; i++) {
            var pt = points.get(i);
            long val = pt.total();
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
                            + "<title>%s: %s %,d</title></circle>",
                    x, y, color, pt.label(), label, val
            ));
        }

        sb.append(String.format(
                "<path d='%s' fill='none' stroke='%s' stroke-width='2.5' stroke-linecap='round' stroke-linejoin='round'/>",
                pathD, color
        ));
        sb.append(circles);
    }

    private void buildXAxisLabels(
            StringBuilder sb, List<AggregatedReviewPoint> points,
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

        legend.add(createLegendItem("#0969da", "Approvals + Comments"));
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

    private String formatHoursToHhMm(Double hours) {
        if (hours == null) {
            return "N/A";
        }
        long totalMinutes = Math.round(hours * 60.0);
        long hh = totalMinutes / 60;
        long mm = totalMinutes % 60;
        return String.format("%02d:%02d", hh, mm);
    }

    private record AggregatedReviewPoint(
            String label,
            LocalDate date,
            long total
    ) {}
}
