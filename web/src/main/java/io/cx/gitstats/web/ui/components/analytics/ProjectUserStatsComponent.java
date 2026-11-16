package io.cx.gitstats.web.ui.components.analytics;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.cx.gitstats.domain.model.analytics.ProjectUserCodeStats;
import io.cx.gitstats.domain.model.analytics.ProjectUserCountStats;
import io.cx.gitstats.domain.model.analytics.ProjectUserStats;

import java.util.List;

public class ProjectUserStatsComponent extends VerticalLayout {

    public ProjectUserStatsComponent(ProjectUserStats stats) {
        setWidthFull();
        setMaxWidth("100%");
        setPadding(false);
        setSpacing(true);
        getStyle().set("box-sizing", "border-box");

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
                .set("grid-template-columns", "repeat(3, minmax(0, 1fr))")
                .set("gap", "16px")
                .set("width", "100%")
                .set("max-width", "100%")
                .set("box-sizing", "border-box")
                .set("margin-top", "8px");

        grid.add(
                createCard("Most Written Users (Code volume)", buildCodeUsersList(stats.getCodeUsers())),
                createCard("Most Approved Users", buildCountUsersList(stats.getApprovedUsers(), "approvals", "#2da44e")),
                createCard("Most Commented Users", buildCountUsersList(stats.getCommentedUsers(), "comments", "#0969da"))
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
                .set("gap", "12px")
                .set("min-width", "0")
                .set("width", "100%")
                .set("box-sizing", "border-box");

        H4 t = new H4(title);
        t.getStyle().set("margin", "0").set("font-size", "0.92rem").set("font-weight", "600").set("color", "#1f1e24");

        card.add(t, content);
        return card;
    }

    private Div buildCodeUsersList(List<ProjectUserCodeStats> users) {
        Div container = new Div();
        container.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "8px")
                .set("width", "100%")
                .set("min-width", "0")
                .set("box-sizing", "border-box");

        if (users == null || users.isEmpty()) {
            container.add(new Span("No user activity"));
            return container;
        }

        for (var u : users) {
            container.add(createCodeUserRow(u));
        }
        return container;
    }

    private Div createCodeUserRow(ProjectUserCodeStats u) {
        Div row = new Div();
        row.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "4px")
                .set("font-size", "0.82rem")
                .set("width", "100%")
                .set("min-width", "0")
                .set("box-sizing", "border-box");

        Div topRow = new Div();
        topRow.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "center")
                .set("gap", "8px")
                .set("width", "100%")
                .set("min-width", "0")
                .set("box-sizing", "border-box");

        String displayUsername = u.getUsername() != null && !u.getUsername().isBlank() ? u.getUsername() : "-";
        Span name = new Span(displayUsername);
        name.getElement().setAttribute("title", displayUsername);
        name.getStyle()
                .set("font-weight", "500")
                .set("color", "#1f1e24")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis")
                .set("white-space", "nowrap")
                .set("min-width", "0")
                .set("flex-shrink", "1");

        Span val = new Span(String.format("%,d (+%,d / -%,d)  %.1f%%", u.getTotalCode(), u.getRowsAdded(), u.getRowsDeleted(), u.getPercentageOfTotal()));
        val.getStyle()
                .set("color", "#6e6d7a")
                .set("font-size", "0.75rem")
                .set("white-space", "nowrap")
                .set("flex-shrink", "0");

        topRow.add(name, val);

        Div track = new Div();
        track.getStyle()
                .set("background", "#f0f2f5")
                .set("height", "6px")
                .set("border-radius", "3px")
                .set("overflow", "hidden")
                .set("width", "100%");

        Div fill = new Div();
        fill.getStyle().set("width", u.getPercentageOfTotal() + "%").set("background", "#8250df").set("height", "100%");
        track.add(fill);

        row.add(topRow, track);
        return row;
    }

    private Div buildCountUsersList(List<ProjectUserCountStats> users, String unit, String color) {
        Div container = new Div();
        container.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "8px")
                .set("width", "100%")
                .set("min-width", "0")
                .set("box-sizing", "border-box");

        if (users == null || users.isEmpty()) {
            container.add(new Span("No user activity"));
            return container;
        }

        for (var u : users) {
            container.add(createCountUserRow(u, unit, color));
        }
        return container;
    }

    private Div createCountUserRow(ProjectUserCountStats u, String unit, String color) {
        Div row = new Div();
        row.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "4px")
                .set("font-size", "0.82rem")
                .set("width", "100%")
                .set("min-width", "0")
                .set("box-sizing", "border-box");

        Div topRow = new Div();
        topRow.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "center")
                .set("gap", "8px")
                .set("width", "100%")
                .set("min-width", "0")
                .set("box-sizing", "border-box");

        String displayUsername = u.getUsername() != null && !u.getUsername().isBlank() ? u.getUsername() : "-";
        Span name = new Span(displayUsername);
        name.getElement().setAttribute("title", displayUsername);
        name.getStyle()
                .set("font-weight", "500")
                .set("color", "#1f1e24")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis")
                .set("white-space", "nowrap")
                .set("min-width", "0")
                .set("flex-shrink", "1");

        Span val = new Span(String.format("%,d %s (%.1f%%)", u.getCount(), unit, u.getPercentageOfTotal()));
        val.getStyle()
                .set("color", "#6e6d7a")
                .set("font-size", "0.75rem")
                .set("white-space", "nowrap")
                .set("flex-shrink", "0");

        topRow.add(name, val);

        Div track = new Div();
        track.getStyle()
                .set("background", "#f0f2f5")
                .set("height", "6px")
                .set("border-radius", "3px")
                .set("overflow", "hidden")
                .set("width", "100%");

        Div fill = new Div();
        fill.getStyle().set("width", u.getPercentageOfTotal() + "%").set("background", color).set("height", "100%");
        track.add(fill);

        row.add(topRow, track);
        return row;
    }
}
