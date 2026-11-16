package io.cx.gitstats.web.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.cx.gitstats.domain.model.DailyContribution;
import io.cx.gitstats.domain.model.User;
import io.cx.gitstats.domain.model.analytics.UserOverviewAnalytics;
import io.cx.gitstats.domain.service.UserActivityService;
import io.cx.gitstats.domain.service.UserContributionService;
import io.cx.gitstats.domain.service.UserService;
import io.cx.gitstats.domain.service.analytics.UserAnalyticsService;
import io.cx.gitstats.web.ui.components.ContributionCalendarComponent;
import io.cx.gitstats.web.ui.components.activity.UserActivityComponent;
import io.cx.gitstats.web.ui.components.analytics.UserOverviewAnalyticsComponent;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static io.cx.gitstats.web.ui.utils.UiUtils.DEFAULT_PAGE_WIDTH;

@PageTitle("User | GitStats")
@Route(value = "user", layout = MainLayout.class)
public class UserView extends VerticalLayout implements HasUrlParameter<String> {

    private final UserService userService;
    private final UserContributionService userContributionService;
    private final UserAnalyticsService userAnalyticsService;
    private final UserActivityService userActivityService;

    private User currentUser;
    private List<DailyContribution> userContributions;
    private boolean includeAiReview = false;

    private LocalDate activityFrom;
    private LocalDate activityTo;
    private boolean activityIncludeMr = true;
    private boolean activityIncludeApprovals = true;
    private boolean activityIncludeComments = true;

    private Div headerContainer;
    private Tabs navigationTabs;
    private Div contentContainer;

    private Tab overviewTab;
    private Tab activityTab;

    public UserView(
            UserService userService,
            UserContributionService userContributionService,
            UserAnalyticsService userAnalyticsService,
            UserActivityService userActivityService
    ) {
        this.userService = userService;
        this.userContributionService = userContributionService;
        this.userAnalyticsService = userAnalyticsService;
        this.userActivityService = userActivityService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);

        initLayout();
    }

    private void initLayout() {
        VerticalLayout pageContainer = new VerticalLayout();
        pageContainer.setWidthFull();
        pageContainer.setMaxWidth(DEFAULT_PAGE_WIDTH);
        pageContainer.setSizeFull();
        pageContainer.setPadding(false);
        pageContainer.setSpacing(true);

        headerContainer = new Div();
        contentContainer = new Div();
        contentContainer.setWidthFull();

        overviewTab = new Tab("Overview");
        activityTab = new Tab("Activity");

        navigationTabs = new Tabs(overviewTab, activityTab);
        navigationTabs.setWidthFull();
        navigationTabs.addSelectedChangeListener(e -> updateTabContent());

        pageContainer.add(headerContainer, navigationTabs, contentContainer);
        add(pageContainer);
        expand(pageContainer);
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        if (parameter == null || parameter.isBlank()) {
            showEmptyState("No user specified");
            return;
        }

        if (userService == null || userContributionService == null) {
            showEmptyState("User service unavailable");
            return;
        }

        currentUser = findUser(parameter).orElse(null);
        if (currentUser == null) {
            showEmptyState("User not found: " + parameter);
            return;
        }

        userContributions = userContributionService.getUserDailyContributions(currentUser.getId());
        renderUserInfo();
        updateTabContent();
    }

    private Optional<User> findUser(String parameter) {
        Optional<User> byUsername = userService.findByUsername(parameter);
        if (byUsername.isPresent()) {
            return byUsername;
        }
        try {
            Long id = Long.parseLong(parameter);
            return userService.findById(id);
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private void renderUserInfo() {
        headerContainer.removeAll();
        String displayName = currentUser.getName() != null && !currentUser.getName().isBlank()
                ? currentUser.getName()
                : currentUser.getUsername();

        H2 nameHeading = new H2(displayName);
        nameHeading.getStyle()
                .set("margin", "0")
                .set("font-size", "1.5rem")
                .set("font-weight", "600");

        Span usernameSpan = new Span("@" + currentUser.getUsername());
        usernameSpan.getStyle()
                .set("color", "#6e6d7a")
                .set("font-size", "0.95rem");

        HorizontalLayout userHeader = new HorizontalLayout(nameHeading, usernameSpan);
        userHeader.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        userHeader.setSpacing(true);

        if (Boolean.TRUE.equals(currentUser.getIsBot())) {
            Span botBadge = new Span("Bot");
            botBadge.getStyle()
                    .set("background-color", "#e8eeff")
                    .set("color", "#1068bf")
                    .set("font-size", "0.75rem")
                    .set("font-weight", "600")
                    .set("padding", "2px 6px")
                    .set("border-radius", "4px");
            userHeader.add(botBadge);
        }

        headerContainer.add(userHeader);
    }

    private void updateTabContent() {
        contentContainer.removeAll();
        if (currentUser == null) {
            return;
        }

        Tab selected = navigationTabs.getSelectedTab();
        if (selected == overviewTab) {
            contentContainer.add(createOverviewContent());
        } else if (selected == activityTab) {
            contentContainer.add(createActivityContent());
        }
    }

    private Component createOverviewContent() {
        VerticalLayout layout = new VerticalLayout();
        layout.setWidthFull();
        layout.setPadding(false);
        layout.setSpacing(true);

        ContributionCalendarComponent calendar = new ContributionCalendarComponent(
                currentUser,
                userContributions,
                userContributionService,
                true,
                true,
                true,
                true,
                true
        );

        Div analyticsContainer = new Div();
        analyticsContainer.setWidthFull();

        calendar.addPeriodChangeListener((from, to) -> updateAnalyticsView(analyticsContainer, from, to));
        calendar.addDayClickListener(this::switchToActivityTab);
        updateAnalyticsView(analyticsContainer, calendar.getCurrentFilterStartDate(), calendar.getCurrentFilterEndDate());

        layout.add(calendar, analyticsContainer);
        return layout;
    }

    private void switchToActivityTab(LocalDate day, boolean mr, boolean app, boolean com) {
        this.activityFrom = day;
        this.activityTo = day;
        this.activityIncludeMr = mr;
        this.activityIncludeApprovals = app;
        this.activityIncludeComments = com;
        navigationTabs.setSelectedTab(activityTab);
    }

    private void updateAnalyticsView(Div container, LocalDate from, LocalDate to) {
        if (currentUser == null || userAnalyticsService == null) {
            return;
        }
        LocalDate start = from != null && from.equals(LocalDate.MIN) ? LocalDate.now().minusYears(10) : from;
        UserOverviewAnalytics analytics = userAnalyticsService.getUserOverviewAnalytics(
                currentUser.getId(),
                start,
                to,
                includeAiReview
        );
        container.removeAll();
        if (analytics != null) {
            container.add(new UserOverviewAnalyticsComponent(analytics, includeAiReview, isAi -> {
                this.includeAiReview = isAi;
                updateAnalyticsView(container, from, to);
            }));
        }
    }

    private Component createActivityContent() {
        return new UserActivityComponent(
                currentUser,
                userContributionService,
                userActivityService,
                userContributions,
                activityFrom,
                activityTo,
                activityIncludeMr,
                activityIncludeApprovals,
                activityIncludeComments
        );
    }

    private void showEmptyState(String message) {
        headerContainer.removeAll();
        contentContainer.removeAll();
        contentContainer.add(new Span(message));
    }
}
