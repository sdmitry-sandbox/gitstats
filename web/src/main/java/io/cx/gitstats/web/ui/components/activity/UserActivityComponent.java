package io.cx.gitstats.web.ui.components.activity;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.cx.gitstats.domain.model.DailyContribution;
import io.cx.gitstats.domain.model.User;
import io.cx.gitstats.domain.model.activity.UserActivityItem;
import io.cx.gitstats.domain.model.activity.UserActivityType;
import io.cx.gitstats.domain.service.UserActivityService;
import io.cx.gitstats.domain.service.UserContributionService;
import io.cx.gitstats.web.ui.components.ContributionCalendarComponent;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class UserActivityComponent extends VerticalLayout {

    private static final DateTimeFormatter MOSCOW_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

    private final User user;
    private final UserActivityService userActivityService;
    private final UserContributionService userContributionService;
    private final List<DailyContribution> contributions;

    private LocalDate filterFrom;
    private LocalDate filterTo;
    private boolean includeMr;
    private boolean includeApprovals;
    private boolean includeComments;

    private Div activityCard;

    public UserActivityComponent(
            User user,
            UserContributionService userContributionService,
            UserActivityService userActivityService,
            List<DailyContribution> contributions,
            LocalDate initialStart,
            LocalDate initialEnd,
            boolean initialMr,
            boolean initialApprovals,
            boolean initialComments
    ) {
        this.user = user;
        this.userContributionService = userContributionService;
        this.userActivityService = userActivityService;
        this.contributions = contributions != null ? contributions : List.of();
        this.filterFrom = initialStart;
        this.filterTo = initialEnd;
        this.includeMr = initialMr;
        this.includeApprovals = initialApprovals;
        this.includeComments = initialComments;

        setWidthFull();
        setPadding(false);
        setSpacing(true);

        initComponent();
    }

    private void initComponent() {
        ContributionCalendarComponent calendar = new ContributionCalendarComponent(
                user,
                contributions,
                userContributionService,
                true,
                true,
                true,
                true,
                true
        );

        if (filterFrom == null && filterTo == null) {
            filterFrom = calendar.getCurrentFilterStartDate();
            filterTo = calendar.getCurrentFilterEndDate();
        }

        calendar.addPeriodChangeListener((from, to) -> {
            this.filterFrom = from;
            this.filterTo = to;
            renderActivities();
        });

        calendar.addDayClickListener((day, mr, app, com) -> {
            this.filterFrom = day;
            this.filterTo = day;
            this.includeMr = mr;
            this.includeApprovals = app;
            this.includeComments = com;
            renderActivities();
        });

        calendar.addSettingsChangeListener((mr, app, com) -> {
            this.includeMr = mr;
            this.includeApprovals = app;
            this.includeComments = com;
            renderActivities();
        });

        activityCard = new Div();
        styleActivityCard(activityCard);

        add(calendar, activityCard);
        renderActivities();
    }

    private void styleActivityCard(Div card) {
        card.setWidthFull();
        card.getStyle()
                .set("background", "#ffffff")
                .set("border", "1px solid #dcdcde")
                .set("border-radius", "6px")
                .set("padding", "16px 20px")
                .set("box-sizing", "border-box");
    }

    private void renderActivities() {
        activityCard.removeAll();
        if (user == null || userActivityService == null) {
            return;
        }

        LocalDate from = filterFrom != null && filterFrom.equals(LocalDate.MIN) ? LocalDate.now().minusYears(10) : filterFrom;
        List<UserActivityItem> items = userActivityService.getUserActivities(
                user.getId(),
                from,
                filterTo,
                includeMr,
                includeApprovals,
                includeComments
        );

        if (items.isEmpty()) {
            Span emptySpan = new Span("No activities found for the selected period and settings.");
            emptySpan.getStyle().set("color", "#6e6d7a").set("font-size", "14px");
            activityCard.add(emptySpan);
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            activityCard.add(buildActivityRow(items.get(i), i == items.size() - 1));
        }
    }

    private Div buildActivityRow(UserActivityItem item, boolean isLast) {
        Div row = new Div();
        row.setWidthFull();
        row.getStyle()
                .set("display", "flex")
                .set("align-items", "flex-start")
                .set("padding", "10px 0")
                .set("gap", "12px");

        if (!isLast) {
            row.getStyle().set("border-bottom", "1px solid #f0f0f1");
        }

        Div iconWrapper = buildIconWrapper(item.getType());
        Div contentWrapper = buildContentWrapper(item);
        Span timeSpan = buildTimeSpan(item.getActivityTime());

        row.add(iconWrapper, contentWrapper, timeSpan);
        return row;
    }

    private Div buildIconWrapper(UserActivityType type) {
        Div wrapper = new Div();
        wrapper.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("width", "20px")
                .set("flex-shrink", "0")
                .set("margin-top", "2px");

        if (type == UserActivityType.MERGE_REQUEST) {
            var icon = VaadinIcon.SPECIALIST.create();
            icon.setSize("16px");
            icon.setColor("#108548");
            wrapper.add(icon);
        } else if (type == UserActivityType.APPROVAL) {
            var icon = VaadinIcon.CHECK.create();
            icon.setSize("15px");
            icon.setColor("#6e6d7a");
            wrapper.add(icon);
        } else {
            var icon = VaadinIcon.COMMENT_O.create();
            icon.setSize("15px");
            icon.setColor("#6e6d7a");
            wrapper.add(icon);
        }
        return wrapper;
    }

    private Div buildContentWrapper(UserActivityItem item) {
        Div content = new Div();
        content.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("flex-grow", "1")
                .set("gap", "4px");

        HorizontalLayout actionLine = buildActionLine(item);
        content.add(actionLine);

        if (item.getType() == UserActivityType.COMMENT && item.getCommentMessage() != null && !item.getCommentMessage().isBlank()) {
            Div commentPreview = new Div();
            commentPreview.setText(item.getCommentMessage());
            commentPreview.getStyle()
                    .set("color", "#4c4b54")
                    .set("font-size", "13px")
                    .set("margin-top", "2px")
                    .set("background-color", "#fbfafd")
                    .set("padding", "6px 12px")
                    .set("border-left", "3px solid #dcdcde")
                    .set("border-radius", "0 4px 4px 0")
                    .set("white-space", "pre-wrap")
                    .set("word-break", "break-word");
            content.add(commentPreview);
        }
        return content;
    }

    private HorizontalLayout buildActionLine(UserActivityItem item) {
        HorizontalLayout line = new HorizontalLayout();
        line.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        line.setSpacing(true);
        line.getStyle().set("gap", "6px").set("flex-wrap", "wrap").set("font-size", "14px");

        Span actionSpan = new Span(getActionText(item.getType()));
        actionSpan.getStyle().set("color", "#1f1e24").set("font-weight", "500");
        line.add(actionSpan);

        if (item.getType() == UserActivityType.MERGE_REQUEST) {
            line.add(buildRowsDiffSpan(item.getRowsAdded(), item.getRowsDeleted()));
        } else if (item.getReactionTime() != null) {
            line.add(buildReactionTimeBadge(item.getReactionTime()));
        }

        line.add(buildMrLink(item));

        if (item.getProjectName() != null && !item.getProjectName().isBlank()) {
            Span atSpan = new Span("at");
            atSpan.getStyle().set("color", "#6e6d7a");
            Span projectSpan = new Span(item.getProjectName());
            projectSpan.getStyle().set("color", "#1068bf").set("font-weight", "500");
            line.add(atSpan, projectSpan);
        }

        return line;
    }

    private String getActionText(UserActivityType type) {
        return switch (type) {
            case MERGE_REQUEST -> "Accepted merge request";
            case APPROVAL -> "Approved merge request";
            case COMMENT -> "Commented on merge request";
        };
    }

    private Span buildRowsDiffSpan(Integer added, Integer deleted) {
        int a = added != null ? added : 0;
        int d = deleted != null ? deleted : 0;

        Span diffSpan = new Span();
        diffSpan.getStyle().set("font-size", "13px").set("display", "inline-flex").set("gap", "4px");

        Span addedSpan = new Span("+" + a);
        addedSpan.getStyle().set("color", "#108548").set("font-weight", "600");

        Span deletedSpan = new Span("-" + d);
        deletedSpan.getStyle().set("color", "#dd2b0e").set("font-weight", "600");

        diffSpan.add(addedSpan, deletedSpan);
        return diffSpan;
    }

    private Span buildReactionTimeBadge(String reactionTime) {
        Span badge = new Span(reactionTime);
        badge.getStyle()
                .set("background-color", "#f0f0f2")
                .set("color", "#4c4b54")
                .set("font-size", "12px")
                .set("font-weight", "500")
                .set("padding", "1px 6px")
                .set("border-radius", "4px");
        return badge;
    }

    private Anchor buildMrLink(UserActivityItem item) {
        long gitlabId = item.getMrGitlabId() != null ? item.getMrGitlabId() : (item.getGitlabId() != null ? item.getGitlabId() : 0L);
        String title = item.getMrTitle() != null ? item.getMrTitle() : "";
        String text = "!" + gitlabId + " \"" + title + "\"";
        String url = item.getMrWebUrl() != null && !item.getMrWebUrl().isBlank() ? item.getMrWebUrl() : "#";

        Anchor link = new Anchor(url, text);
        link.setTarget("_blank");
        link.getStyle()
                .set("color", "#1068bf")
                .set("text-decoration", "none")
                .set("font-weight", "500");
        return link;
    }

    private Span buildTimeSpan(Instant activityTime) {
        Span span = new Span(formatRelativeTime(activityTime));
        span.getStyle()
                .set("color", "#8c8b96")
                .set("font-size", "13px")
                .set("white-space", "nowrap")
                .set("margin-left", "auto");

        span.getElement().setAttribute("title", formatMoscowDateTime(activityTime));
        return span;
    }

    public static String formatRelativeTime(Instant instant) {
        if (instant == null) {
            return "";
        }
        Duration duration = Duration.between(instant, Instant.now());
        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return "just now";
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes == 1 ? "1 minute ago" : minutes + " minutes ago";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours == 1 ? "1 hour ago" : hours + " hours ago";
        }
        long days = hours / 24;
        if (days < 7) {
            return days == 1 ? "1 day ago" : days + " days ago";
        }
        long weeks = days / 7;
        if (weeks < 4) {
            return weeks == 1 ? "1 week ago" : weeks + " weeks ago";
        }
        long months = days / 30;
        if (months < 12) {
            return months == 1 ? "1 month ago" : months + " months ago";
        }
        long years = days / 365;
        return years == 1 ? "1 year ago" : years + " years ago";
    }

    public static String formatMoscowDateTime(Instant instant) {
        if (instant == null) {
            return "";
        }
        return MOSCOW_FORMATTER.format(instant.atZone(ZoneId.of("Europe/Moscow")));
    }
}
