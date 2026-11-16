package io.cx.gitstats.web.ui.components.analytics;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.cx.gitstats.domain.model.analytics.GeneralStats;
import io.cx.gitstats.domain.model.analytics.PercentileRankBadge;

public class GeneralStatsComponent extends VerticalLayout {

    private static final int SEGMENT_COUNT = 20;

    @FunctionalInterface
    public interface AiReviewChangeListener {
        void onAiReviewChanged(boolean includeAiReview);
    }

    private boolean includeAiReview;
    private final AiReviewChangeListener aiReviewChangeListener;
    private Div settingsPopup;
    private Div dropdownBackdrop;

    public GeneralStatsComponent(GeneralStats stats) {
        this(stats, false, null);
    }

    public GeneralStatsComponent(
            GeneralStats stats,
            boolean includeAiReview,
            AiReviewChangeListener listener
    ) {
        this.includeAiReview = includeAiReview;
        this.aiReviewChangeListener = listener;

        setWidthFull();
        setPadding(false);
        setSpacing(true);

        initBackdrop();
        HorizontalLayout headerLayout = buildHeaderLayout();
        add(headerLayout, dropdownBackdrop);

        if (stats == null) {
            add(new Span("No data available"));
            return;
        }

        Div card = new Div();
        styleCard(card);
        card.add(
                createStatRow("Code written", stats.getCodeContributionRank(), true),
                createStatRow("Merged MRs", stats.getMergedMrsRank(), false),
                createStatRow("Review (Approvals + Comments)", stats.getReviewActivityRank(), false)
        );
        add(card);

        if (stats.getRadarChartStats() != null) {
            add(new RadarChartComponent(stats.getRadarChartStats()));
        }
    }

    private void initBackdrop() {
        dropdownBackdrop = new Div();
        dropdownBackdrop.setVisible(false);
        dropdownBackdrop.getStyle()
                .set("position", "fixed")
                .set("top", "0")
                .set("left", "0")
                .set("width", "100vw")
                .set("height", "100vh")
                .set("z-index", "999")
                .set("background", "transparent");
        dropdownBackdrop.addClickListener(e -> closePopups());
    }

    private HorizontalLayout buildHeaderLayout() {
        H3 sectionTitle = new H3("General Statistics");
        sectionTitle.getStyle()
                .set("font-size", "1.1rem")
                .set("font-weight", "600")
                .set("margin", "0")
                .set("color", "#1f1e24");

        HorizontalLayout controlsLayout = new HorizontalLayout();
        controlsLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        controlsLayout.setSpacing(true);
        controlsLayout.add(buildSettingsDropdown());

        HorizontalLayout headerLayout = new HorizontalLayout(sectionTitle, controlsLayout);
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        headerLayout.getStyle().set("margin-bottom", "12px");
        return headerLayout;
    }

    private Div buildSettingsDropdown() {
        Div container = new Div();
        container.getStyle()
                .set("position", "relative")
                .set("z-index", "1000");

        Button settingsBtn = new Button("Statistics settings", VaadinIcon.CHEVRON_DOWN.create());
        settingsBtn.setIconAfterText(true);
        settingsBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        styleDropdownButton(settingsBtn);

        settingsPopup = createSettingsPopup();
        settingsBtn.addClickListener(e -> {
            boolean nextVisible = !settingsPopup.isVisible();
            settingsPopup.setVisible(nextVisible);
            dropdownBackdrop.setVisible(nextVisible);
        });

        container.add(settingsBtn, settingsPopup);
        return container;
    }

    private void styleDropdownButton(Button button) {
        button.getStyle()
                .set("color", "#1f1e24")
                .set("font-size", "12px")
                .set("font-weight", "500")
                .set("border", "1px solid #dcdcde")
                .set("border-radius", "6px")
                .set("padding", "0 10px")
                .set("background-color", "#ffffff")
                .set("cursor", "pointer");
    }

    private Div createSettingsPopup() {
        Div popup = new Div();
        popup.setVisible(false);
        popup.getStyle()
                .set("position", "absolute")
                .set("right", "0")
                .set("top", "100%")
                .set("background", "#ffffff")
                .set("border", "1px solid #dcdcde")
                .set("border-radius", "6px")
                .set("box-shadow", "0 4px 12px rgba(0,0,0,0.1)")
                .set("padding", "8px 12px")
                .set("z-index", "1000")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "4px")
                .set("min-width", "150px");

        Checkbox aiReviewCb = new Checkbox("Include AI review", includeAiReview);
        aiReviewCb.getStyle().set("font-size", "12px");
        aiReviewCb.addValueChangeListener(e -> {
            includeAiReview = Boolean.TRUE.equals(e.getValue());
            if (aiReviewChangeListener != null) {
                aiReviewChangeListener.onAiReviewChanged(includeAiReview);
            }
        });
        popup.add(aiReviewCb);
        return popup;
    }

    private void closePopups() {
        if (settingsPopup != null) {
            settingsPopup.setVisible(false);
        }
        if (dropdownBackdrop != null) {
            dropdownBackdrop.setVisible(false);
        }
    }

    private void styleCard(Div card) {
        card.getStyle()
                .set("background", "#ffffff")
                .set("border", "1px solid #dcdcde")
                .set("border-radius", "8px")
                .set("padding", "16px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "14px")
                .set("width", "100%")
                .set("box-sizing", "border-box");
    }

    private Div createStatRow(String title, PercentileRankBadge badge, boolean isCode) {
        Div row = new Div();
        row.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "200px 45px 1fr 300px")
                .set("align-items", "center")
                .set("column-gap", "16px")
                .set("width", "100%")
                .set("box-sizing", "border-box");

        int percentile = badge != null ? Math.max(0, Math.min(100, badge.getPercentile())) : 0;

        row.add(
                createTitleSpan(title),
                createPercentileSpan(percentile),
                createDiscreteProgressBar(percentile),
                createMetricsBlock(badge, isCode)
        );
        return row;
    }

    private Div createMetricsBlock(PercentileRankBadge badge, boolean isCode) {
        Div block = new Div();
        block.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "flex-end")
                .set("gap", "12px")
                .set("width", "100%")
                .set("white-space", "nowrap");

        block.add(createValueBlock(badge, isCode), createSubtext(badge));
        return block;
    }

    private Span createTitleSpan(String title) {
        Span span = new Span(title);
        span.getStyle()
                .set("font-size", "0.88rem")
                .set("color", "#1f1e24")
                .set("font-weight", "500")
                .set("white-space", "nowrap");
        return span;
    }

    private Span createPercentileSpan(int percentile) {
        Span span = new Span(percentile + "%");
        span.getStyle()
                .set("font-size", "0.92rem")
                .set("color", "#1f1e24")
                .set("font-weight", "600")
                .set("text-align", "right")
                .set("white-space", "nowrap");
        return span;
    }

    private Div createDiscreteProgressBar(int percentile) {
        Div bar = new Div();
        bar.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "2px")
                .set("width", "100%");

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

    private Div createValueBlock(PercentileRankBadge badge, boolean isCode) {
        Div valueDiv = new Div();
        valueDiv.getStyle()
                .set("font-size", "0.92rem")
                .set("font-weight", "600")
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "4px")
                .set("white-space", "nowrap");

        if (badge == null) {
            valueDiv.add(new Span("0"));
            return valueDiv;
        }

        if (isCode && badge.getRowsAdded() != null && badge.getRowsDeleted() != null) {
            Span addedSpan = new Span(String.format("+%,d", badge.getRowsAdded()));
            addedSpan.getStyle().set("color", "#2da44e");

            Span sepSpan = new Span(" / ");
            sepSpan.getStyle().set("color", "#6e6d7a");

            Span deletedSpan = new Span(String.format("-%,d", badge.getRowsDeleted()));
            deletedSpan.getStyle().set("color", "#cf222e");

            valueDiv.add(addedSpan, sepSpan, deletedSpan);
        } else {
            Span userValSpan = new Span(String.format("%,d", badge.getUserValue()));
            userValSpan.getStyle().set("color", "#1f1e24");
            valueDiv.add(userValSpan);
        }
        return valueDiv;
    }

    private Span createSubtext(PercentileRankBadge badge) {
        Span sub = new Span();
        sub.getStyle()
                .set("font-size", "0.8rem")
                .set("color", "#6e6d7a")
                .set("white-space", "nowrap");

        if (badge == null) {
            return sub;
        }

        long roundedAvg = Math.round(badge.getAverageValue());
        long roundedDiff = Math.round(badge.getComparisonWithAveragePercent());
        String sign = roundedDiff >= 0 ? "+" : "";
        sub.setText(String.format("Avg: %,d (%s%d%%)", roundedAvg, sign, roundedDiff));
        return sub;
    }
}
