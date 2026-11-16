package io.cx.gitstats.web.ui.components.analytics;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.cx.gitstats.domain.model.analytics.ActivityStats;

import java.time.DayOfWeek;
import java.util.Map;

public class ActivityStatsComponent extends VerticalLayout {

    public ActivityStatsComponent(ActivityStats stats) {
        setWidthFull();
        setPadding(false);
        setSpacing(true);

        H3 sectionTitle = new H3("Activity Statistics");
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

        Div grid = new Div();
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "1fr 3fr")
                .set("gap", "16px")
                .set("width", "100%")
                .set("margin-top", "8px");

        grid.add(
                createCardWrapper("By Day Activity", buildDayOfWeekBars(stats.getHeatmapByDayOfWeek())),
                createCardWrapper("By Hour Activity (Moscow time)", buildHourOfDayBars(stats.getHeatmapByHourOfDay()))
        );
        add(grid);
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

    private Div buildDayOfWeekBars(Map<DayOfWeek, Long> dowMap) {
        Div container = new Div();
        container.getStyle()
                .set("display", "flex")
                .set("gap", "4px")
                .set("width", "100%")
                .set("align-items", "flex-end")
                .set("justify-content", "space-between");

        if (dowMap == null || dowMap.isEmpty()) {
            container.add(new Span("No data"));
            return container;
        }

        long maxCount = dowMap.values().stream().mapToLong(Long::longValue).max().orElse(1L);
        if (maxCount <= 0) maxCount = 1L;

        String[] shortNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        DayOfWeek[] dows = {
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        };

        for (int i = 0; i < 7; i++) {
            long count = dowMap.getOrDefault(dows[i], 0L);
            container.add(createVerticalBarColumn(shortNames[i], count, maxCount, 80));
        }
        return container;
    }

    private Div buildHourOfDayBars(Map<Integer, Long> hourMap) {
        Div container = new Div();
        container.getStyle()
                .set("display", "flex")
                .set("gap", "2px")
                .set("width", "100%")
                .set("align-items", "flex-end")
                .set("justify-content", "space-between");

        if (hourMap == null || hourMap.isEmpty()) {
            container.add(new Span("No data"));
            return container;
        }

        long maxCount = hourMap.values().stream().mapToLong(Long::longValue).max().orElse(1L);
        if (maxCount <= 0) maxCount = 1L;

        for (int h = 0; h < 24; h++) {
            long count = hourMap.getOrDefault(h, 0L);
            String label = String.format("%02d", h);
            container.add(createVerticalBarColumn(label, count, maxCount, 80));
        }
        return container;
    }

    private Div createVerticalBarColumn(String label, long count, long maxCount, int barHeightPx) {
        Div col = new Div();
        col.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("gap", "4px")
                .set("flex", "1")
                .set("min-width", "0");
        col.setTitle(label + ": " + count);

        Span countSpan = new Span(String.valueOf(count));
        countSpan.getStyle()
                .set("font-size", "0.72rem")
                .set("font-weight", "600")
                .set("color", count > 0 ? "#1f1e24" : "#8c8c8c")
                .set("line-height", "1");

        Div track = new Div();
        track.getStyle()
                .set("height", barHeightPx + "px")
                .set("width", "100%")
                .set("max-width", "18px")
                .set("background", "#f0f2f5")
                .set("border-radius", "3px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("justify-content", "flex-end")
                .set("overflow", "hidden");

        double ratio = maxCount > 0 ? (double) count / maxCount : 0.0;
        int fillPct = (int) Math.round(ratio * 100.0);
        if (count > 0 && fillPct < 4) {
            fillPct = 4;
        }

        Div fill = new Div();
        fill.getStyle()
                .set("height", fillPct + "%")
                .set("width", "100%")
                .set("background", getBarColor(ratio))
                .set("border-radius", "3px");
        track.add(fill);

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "0.68rem")
                .set("color", "#6e6d7a")
                .set("font-weight", "500")
                .set("line-height", "1");

        col.add(countSpan, track, labelSpan);
        return col;
    }

    private String getBarColor(double ratio) {
        if (ratio <= 0.0) return "#dcdcde";
        if (ratio < 0.25) return "#acd5f2";
        if (ratio < 0.50) return "#7fa8d1";
        if (ratio < 0.75) return "#4977b4";
        return "#1b4f8a";
    }
}
