package io.cx.gitstats.web.ui.components.analytics;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.cx.gitstats.domain.model.analytics.ProjectCodeStats;
import io.cx.gitstats.domain.model.analytics.ProjectCountStats;
import io.cx.gitstats.domain.model.analytics.ProjectStats;

import java.util.List;

public class ProjectStatsComponent extends VerticalLayout {

    public ProjectStatsComponent(ProjectStats stats) {
        setWidthFull();
        setPadding(false);
        setSpacing(true);

        H3 sectionTitle = new H3("Project Statistics");
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
                .set("grid-template-columns", "repeat(3, 1fr)")
                .set("gap", "16px")
                .set("width", "100%")
                .set("margin-top", "8px");

        grid.add(
                createCard("Most Written Projects (Code volume)", buildCodeProjectsList(stats.getCodeProjects())),
                createCard("Most Approved Projects", buildCountProjectsList(stats.getApprovedProjects(), "approvals", "#2da44e")),
                createCard("Most Commented Projects", buildCountProjectsList(stats.getCommentedProjects(), "comments", "#0969da"))
        );
        add(grid);
    }

    private Div createCard(String title, Div content) {
        Div card = new Div();
        card.getStyle()
                .set("background", "#ffffff")
                .set("border", "1px solid #dcdcde")
                .set("border-radius", "8px")
                .set("padding", "16px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "12px");

        H4 t = new H4(title);
        t.getStyle().set("margin", "0").set("font-size", "0.92rem").set("font-weight", "600").set("color", "#1f1e24");

        card.add(t, content);
        return card;
    }

    private Div buildCodeProjectsList(List<ProjectCodeStats> projects) {
        Div container = new Div();
        container.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "8px").set("width", "100%");

        if (projects == null || projects.isEmpty()) {
            container.add(new Span("No project activity"));
            return container;
        }

        for (var p : projects) {
            container.add(createCodeProjectRow(p));
        }
        return container;
    }

    private Div createCodeProjectRow(ProjectCodeStats p) {
        Div row = new Div();
        row.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "4px").set("font-size", "0.82rem");

        Div topRow = new Div();
        topRow.getStyle().set("display", "flex").set("justify-content", "space-between").set("align-items", "center");

        Span name = new Span(p.getProjectName());
        name.getStyle().set("font-weight", "500").set("color", "#1f1e24").set("overflow", "hidden").set("text-overflow", "ellipsis").set("white-space", "nowrap");

        Span val = new Span(String.format("%,d (+%,d / -%,d)  %.1f%%", p.getTotalCode(), p.getRowsAdded(), p.getRowsDeleted(), p.getPercentageOfTotal()));
        val.getStyle().set("color", "#6e6d7a").set("font-size", "0.75rem").set("white-space", "nowrap");

        topRow.add(name, val);

        Div track = new Div();
        track.getStyle().set("background", "#f0f2f5").set("height", "6px").set("border-radius", "3px").set("overflow", "hidden");

        Div fill = new Div();
        fill.getStyle().set("width", p.getPercentageOfTotal() + "%").set("background", "#8250df").set("height", "100%");
        track.add(fill);

        row.add(topRow, track);
        return row;
    }

    private Div buildCountProjectsList(List<ProjectCountStats> projects, String unit, String color) {
        Div container = new Div();
        container.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "8px").set("width", "100%");

        if (projects == null || projects.isEmpty()) {
            container.add(new Span("No project activity"));
            return container;
        }

        for (var p : projects) {
            container.add(createCountProjectRow(p, unit, color));
        }
        return container;
    }

    private Div createCountProjectRow(ProjectCountStats p, String unit, String color) {
        Div row = new Div();
        row.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "4px").set("font-size", "0.82rem");

        Div topRow = new Div();
        topRow.getStyle().set("display", "flex").set("justify-content", "space-between").set("align-items", "center");

        Span name = new Span(p.getProjectName());
        name.getStyle().set("font-weight", "500").set("color", "#1f1e24").set("overflow", "hidden").set("text-overflow", "ellipsis").set("white-space", "nowrap");

        Span val = new Span(String.format("%,d %s (%.1f%%)", p.getCount(), unit, p.getPercentageOfTotal()));
        val.getStyle().set("color", "#6e6d7a").set("font-size", "0.75rem").set("white-space", "nowrap");

        topRow.add(name, val);

        Div track = new Div();
        track.getStyle().set("background", "#f0f2f5").set("height", "6px").set("border-radius", "3px").set("overflow", "hidden");

        Div fill = new Div();
        fill.getStyle().set("width", p.getPercentageOfTotal() + "%").set("background", color).set("height", "100%");
        track.add(fill);

        row.add(topRow, track);
        return row;
    }
}
