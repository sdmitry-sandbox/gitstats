package io.cx.gitstats.web.ui.components.analytics;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import java.util.List;

public class DiscreteBarChartComponent extends Div {

    private static final int SEGMENT_COUNT = 20;

    public record BarItem(String label, double percentage, String valueText) {
    }

    public DiscreteBarChartComponent(List<BarItem> items, String emptyText) {
        getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "8px")
                .set("width", "100%");

        if (items == null || items.isEmpty()) {
            add(new Span(emptyText != null ? emptyText : "No data"));
            return;
        }

        for (BarItem item : items) {
            add(createRow(item));
        }
    }

    private Div createRow(BarItem item) {
        Div row = new Div();
        row.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "12px")
                .set("font-size", "0.82rem")
                .set("width", "100%");

        Span labelSpan = new Span(item.label());
        labelSpan.getStyle()
                .set("width", "120px")
                .set("min-width", "120px")
                .set("color", "#6e6d7a")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis")
                .set("white-space", "nowrap");

        int pct = Math.max(0, Math.min(100, (int) Math.round(item.percentage())));
        Div bar = createDiscreteProgressBar(pct);

        Span valueSpan = new Span(item.valueText());
        valueSpan.getStyle()
                .set("min-width", "60px")
                .set("text-align", "right")
                .set("color", "#1f1e24")
                .set("font-weight", "500")
                .set("white-space", "nowrap");

        row.add(labelSpan, bar, valueSpan);
        return row;
    }

    private Div createDiscreteProgressBar(int percentile) {
        Div bar = new Div();
        bar.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "2px")
                .set("flex", "1");

        String filledColor = getBarColor(percentile);
        int filledSegments = (int) Math.round((percentile / 100.0) * SEGMENT_COUNT);

        for (int i = 0; i < SEGMENT_COUNT; i++) {
            Div segment = new Div();
            segment.getStyle()
                    .set("flex", "1")
                    .set("height", "10px")
                    .set("border-radius", "2px")
                    .set("background", i < filledSegments ? filledColor : "#ededed");
            bar.add(segment);
        }
        return bar;
    }

    private String getBarColor(int percentile) {
        if (percentile <= 0) return "#ededed";
        if (percentile < 25) return "#acd5f2";
        if (percentile < 50) return "#7fa8d1";
        if (percentile < 75) return "#4977b4";
        return "#1b4f8a";
    }
}
