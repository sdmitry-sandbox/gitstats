package io.cx.gitstats.web.ui.components.analytics;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.cx.gitstats.domain.model.analytics.RadarChartStats;
import io.cx.gitstats.domain.model.analytics.RadarMetricItem;

import java.util.List;

public class RadarChartComponent extends VerticalLayout {

    private static final int SVG_SIZE = 420;
    private static final int CENTER = SVG_SIZE / 2;
    private static final int MAX_RADIUS = 120;
    private static final int GRID_LEVELS = 10;

    public RadarChartComponent(RadarChartStats stats) {
        setWidthFull();
        setPadding(false);
        setSpacing(true);

        if (stats == null || stats.getAllMetrics() == null || stats.getAllMetrics().isEmpty()) {
            return;
        }

        Div card = new Div();
        styleCard(card);

        Div chartWrapper = buildChartCenterWrapper(stats.getAllMetrics());
        card.add(chartWrapper);

        Div bottomMetricsContainer = buildBottomMetricsContainer(stats);
        card.add(bottomMetricsContainer);

        add(card);
    }

    private void styleCard(Div card) {
        card.getStyle()
                .set("background", "#ffffff")
                .set("border", "1px solid #e1e4e8")
                .set("border-radius", "8px")
                .set("padding", "20px")
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.05)")
                .set("margin-top", "16px")
                .set("box-sizing", "border-box")
                .set("width", "100%")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center");
    }

    private Div buildChartCenterWrapper(List<RadarMetricItem> metrics) {
        Div wrapper = new Div();
        wrapper.getStyle()
                .set("display", "flex")
                .set("justify-content", "center")
                .set("align-items", "center")
                .set("width", "100%")
                .set("margin-bottom", "16px");

        Div chartContainer = buildChartSvgContainer(metrics);
        wrapper.add(chartContainer);
        return wrapper;
    }

    private Div buildChartSvgContainer(List<RadarMetricItem> metrics) {
        Div container = new Div();
        container.getStyle()
                .set("display", "flex")
                .set("justify-content", "center")
                .set("align-items", "center")
                .set("position", "relative")
                .set("max-width", "100%");

        String svgContent = generateRadarSvg(metrics);
        container.getElement().setProperty("innerHTML", svgContent);
        return container;
    }

    private Div buildBottomMetricsContainer(RadarChartStats stats) {
        Div container = new Div();
        container.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("width", "100%")
                .set("gap", "20px")
                .set("box-sizing", "border-box");

        VerticalLayout leftColumn = buildMetricsColumn("Code Contribution", stats.getContributionMetrics(), true);
        VerticalLayout rightColumn = buildMetricsColumn("Code Review", stats.getReviewMetrics(), false);

        container.add(leftColumn, rightColumn);
        return container;
    }

    private VerticalLayout buildMetricsColumn(String title, List<RadarMetricItem> metrics, boolean isLeft) {
        VerticalLayout col = new VerticalLayout();
        col.setPadding(false);
        col.setSpacing(false);
        col.setWidth("50%");

        H4 titleHeader = new H4(title);
        titleHeader.getStyle()
                .set("margin", "0 0 10px 0")
                .set("font-size", "0.95rem")
                .set("font-weight", "600")
                .set("color", isLeft ? "#1b4f8a" : "#2da44e");
        col.add(titleHeader);

        if (metrics != null) {
            for (RadarMetricItem item : metrics) {
                col.add(buildMetricCard(item));
            }
        }
        return col;
    }

    private Div buildMetricCard(RadarMetricItem item) {
        Div card = new Div();
        card.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("padding", "8px 12px")
                .set("margin-bottom", "8px")
                .set("background", "#f8f9fa")
                .set("border-radius", "6px")
                .set("border", "1px solid #ebeef1")
                .set("width", "100%")
                .set("box-sizing", "border-box");

        card.add(buildMetricHeader(item), buildMetricValues(item), buildMetricDesc(item));
        return card;
    }

    private Div buildMetricHeader(RadarMetricItem item) {
        Div header = new Div();
        header.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("margin-bottom", "4px");

        Span nameSpan = new Span(item.getName());
        nameSpan.getStyle()
                .set("font-size", "0.85rem")
                .set("font-weight", "600")
                .set("color", "#1f1e24");

        header.add(nameSpan);
        return header;
    }

    private Div buildMetricValues(RadarMetricItem item) {
        Div valuesContainer = new Div();
        valuesContainer.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "12px")
                .set("font-size", "0.78rem")
                .set("margin-bottom", "4px")
                .set("white-space", "nowrap");

        Span avgSpan = new Span("Avg: " + item.getFormattedAvgValue());
        avgSpan.getStyle().set("color", "#57606a");

        String diffText = formatDifferencePercent(item.getUserValue(), item.getAvgValue());
        Span userSpan = new Span("User: " + item.getFormattedUserValue() + diffText);
        userSpan.getStyle().set("font-weight", "600").set("color", "#24292f");

        valuesContainer.add(avgSpan, userSpan);
        return valuesContainer;
    }

    private String formatDifferencePercent(double userValue, double avgValue) {
        double diff = avgValue > 0 ? ((userValue - avgValue) / avgValue) * 100.0 : 0.0;
        long roundedDiff = Math.round(diff);
        String sign = roundedDiff >= 0 ? "+" : "";
        return String.format(" (%s%d%%)", sign, roundedDiff);
    }

    private Span buildMetricDesc(RadarMetricItem item) {
        Span desc = new Span(item.getDescription());
        desc.getStyle()
                .set("font-size", "0.73rem")
                .set("color", "#6e7781")
                .set("line-height", "1.25");
        return desc;
    }

    private String generateRadarSvg(List<RadarMetricItem> metrics) {
        int count = metrics.size();
        StringBuilder sb = new StringBuilder();
        sb.append("<svg width=\"").append(SVG_SIZE).append("\" height=\"").append(SVG_SIZE)
                .append("\" viewBox=\"0 0 ").append(SVG_SIZE).append(" ").append(SVG_SIZE)
                .append("\" xmlns=\"http://www.w3.org/2000/svg\" style=\"overflow: visible; max-width: 100%; height: auto;\">");

        appendGridPolygons(sb, count);
        appendAxisLines(sb, count);
        appendDataPolygon(sb, metrics);
        appendAxisLabels(sb, metrics);

        sb.append("</svg>");
        return sb.toString();
    }

    private void appendGridPolygons(StringBuilder sb, int count) {
        for (int level = 1; level <= GRID_LEVELS; level++) {
            double radius = (MAX_RADIUS * level) / (double) GRID_LEVELS;
            String stroke = (level == GRID_LEVELS) ? "#c8d1dc" : (level % 2 == 0 ? "#e1e4e8" : "#f0f2f5");
            sb.append("<polygon points=\"");
            for (int i = 0; i < count; i++) {
                double angle = getAngleForIndex(i, count);
                double x = CENTER + radius * Math.cos(angle);
                double y = CENTER + radius * Math.sin(angle);
                sb.append(String.format(java.util.Locale.US, "%.1f,%.1f ", x, y));
            }
            sb.append("\" fill=\"none\" stroke=\"").append(stroke).append("\" stroke-width=\"1\"/>");

            if (level % 2 == 0 || level == GRID_LEVELS) {
                double labelY = CENTER - radius + 3;
                sb.append("<text x=\"").append(CENTER + 4).append("\" y=\"").append(labelY)
                        .append("\" font-size=\"9\" fill=\"#959da5\" font-family=\"sans-serif\">")
                        .append(level * 10).append("%</text>");
            }
        }
    }

    private void appendAxisLines(StringBuilder sb, int count) {
        for (int i = 0; i < count; i++) {
            double angle = getAngleForIndex(i, count);
            double x = CENTER + MAX_RADIUS * Math.cos(angle);
            double y = CENTER + MAX_RADIUS * Math.sin(angle);
            sb.append("<line x1=\"").append(CENTER).append("\" y1=\"").append(CENTER)
                    .append("\" x2=\"").append(String.format(java.util.Locale.US, "%.1f", x))
                    .append("\" y2=\"").append(String.format(java.util.Locale.US, "%.1f", y))
                    .append("\" stroke=\"#d0d7de\" stroke-dasharray=\"2,2\" stroke-width=\"1\"/>");
        }
    }

    private void appendDataPolygon(StringBuilder sb, List<RadarMetricItem> metrics) {
        int count = metrics.size();
        StringBuilder pointsSb = new StringBuilder();
        StringBuilder dotsSb = new StringBuilder();

        for (int i = 0; i < count; i++) {
            RadarMetricItem item = metrics.get(i);
            double radius = (MAX_RADIUS * Math.max(0, Math.min(100, item.getScorePercent()))) / 100.0;
            double angle = getAngleForIndex(i, count);
            double x = CENTER + radius * Math.cos(angle);
            double y = CENTER + radius * Math.sin(angle);

            pointsSb.append(String.format(java.util.Locale.US, "%.1f,%.1f ", x, y));
            dotsSb.append("<circle cx=\"").append(String.format(java.util.Locale.US, "%.1f", x))
                    .append("\" cy=\"").append(String.format(java.util.Locale.US, "%.1f", y))
                    .append("\" r=\"4\" fill=\"#1b4f8a\" stroke=\"#ffffff\" stroke-width=\"1.5\">")
                    .append("<title>").append(item.getName()).append(": ").append(item.getScorePercent()).append("%</title>")
                    .append("</circle>");
        }

        sb.append("<polygon points=\"").append(pointsSb).append("\" fill=\"rgba(27, 79, 138, 0.25)\" stroke=\"#1b4f8a\" stroke-width=\"2\"/>");
        sb.append(dotsSb);
    }

    private void appendAxisLabels(StringBuilder sb, List<RadarMetricItem> metrics) {
        int count = metrics.size();
        double labelRadius = MAX_RADIUS + 22;

        for (int i = 0; i < count; i++) {
            RadarMetricItem item = metrics.get(i);
            double angle = getAngleForIndex(i, count);
            double x = CENTER + labelRadius * Math.cos(angle);
            double y = CENTER + labelRadius * Math.sin(angle);

            String textAnchor = (Math.abs(Math.cos(angle)) < 0.15) ? "middle" : (Math.cos(angle) > 0 ? "start" : "end");
            String color = (i < 4) ? "#1b4f8a" : "#2da44e";

            sb.append("<text x=\"").append(String.format(java.util.Locale.US, "%.1f", x))
                    .append("\" y=\"").append(String.format(java.util.Locale.US, "%.1f", y))
                    .append("\" font-size=\"10\" font-weight=\"600\" fill=\"").append(color)
                    .append("\" text-anchor=\"").append(textAnchor)
                    .append("\" dominant-baseline=\"central\" font-family=\"sans-serif\">")
                    .append(item.getName())
                    .append("</text>");
        }
    }

    private double getAngleForIndex(int index, int total) {
        double[] angles = {
                Math.toRadians(112.5), // 0: Code written (Left bottom-mid)
                Math.toRadians(157.5), // 1: Merged MRs (Left mid)
                Math.toRadians(202.5), // 2: Received comments (Left mid-top)
                Math.toRadians(247.5), // 3: Avg MR size (Left top)
                Math.toRadians(292.5), // 4: MR Approvals (Right top)
                Math.toRadians(337.5), // 5: Reaction Time (Right mid-top)
                Math.toRadians(22.5),  // 6: Produced comments (Right mid)
                Math.toRadians(67.5)   // 7: Review density by MR size (Right bottom-mid)
        };
        if (index >= 0 && index < angles.length) {
            return angles[index];
        }
        return -Math.PI / 2.0 + (index * 2.0 * Math.PI / total);
    }
}
