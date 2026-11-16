package io.cx.gitstats.web.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
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
import io.cx.gitstats.domain.model.Project;
import io.cx.gitstats.domain.model.analytics.ProjectOverviewAnalytics;
import io.cx.gitstats.domain.service.ProjectActivityService;
import io.cx.gitstats.domain.service.ProjectContributionService;
import io.cx.gitstats.domain.service.ProjectService;
import io.cx.gitstats.domain.service.analytics.ProjectAnalyticsService;
import io.cx.gitstats.web.ui.components.ContributionCalendarComponent;
import io.cx.gitstats.web.ui.components.activity.ProjectActivityComponent;
import io.cx.gitstats.web.ui.components.analytics.ProjectOverviewAnalyticsComponent;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static io.cx.gitstats.web.ui.utils.UiUtils.DEFAULT_PAGE_WIDTH;

@PageTitle("Project | GitStats")
@Route(value = "project", layout = MainLayout.class)
public class ProjectView extends VerticalLayout implements HasUrlParameter<String> {

    private final ProjectService projectService;
    private final ProjectContributionService projectContributionService;
    private final ProjectAnalyticsService projectAnalyticsService;
    private final ProjectActivityService projectActivityService;

    private Project currentProject;
    private List<DailyContribution> projectContributions;

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

    public ProjectView(
            ProjectService projectService,
            ProjectContributionService projectContributionService,
            ProjectAnalyticsService projectAnalyticsService,
            ProjectActivityService projectActivityService
    ) {
        this.projectService = projectService;
        this.projectContributionService = projectContributionService;
        this.projectAnalyticsService = projectAnalyticsService;
        this.projectActivityService = projectActivityService;

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
            showEmptyState("No project specified");
            return;
        }

        if (projectService == null || projectContributionService == null) {
            showEmptyState("Project service unavailable");
            return;
        }

        currentProject = findProject(parameter).orElse(null);
        if (currentProject == null) {
            showEmptyState("Project not found: " + parameter);
            return;
        }

        projectContributions = projectContributionService.getProjectDailyContributions(currentProject.getId());
        renderProjectInfo();
        updateTabContent();
    }

    private Optional<Project> findProject(String parameter) {
        try {
            Long id = Long.parseLong(parameter);
            Optional<Project> byId = projectService.findById(id);
            if (byId.isPresent()) {
                return byId;
            }
            Optional<Project> byGitlabId = projectService.findByGitlabId(id);
            if (byGitlabId.isPresent()) {
                return byGitlabId;
            }
        } catch (NumberFormatException ignored) {
        }
        List<Project> matches = projectService.searchByName(parameter);
        if (!matches.isEmpty()) {
            return matches.stream()
                    .filter(p -> parameter.equalsIgnoreCase(p.getName()))
                    .findFirst()
                    .or(() -> Optional.of(matches.get(0)));
        }
        return Optional.empty();
    }

    private void renderProjectInfo() {
        headerContainer.removeAll();
        String displayName = currentProject.getName() != null && !currentProject.getName().isBlank()
                ? currentProject.getName()
                : "Project " + currentProject.getId();

        H2 nameHeading = new H2(displayName);
        nameHeading.getStyle()
                .set("margin", "0")
                .set("font-size", "1.5rem")
                .set("font-weight", "600");

        if (currentProject.getWebUrl() != null && !currentProject.getWebUrl().isBlank()) {
            Anchor webLink = new Anchor(currentProject.getWebUrl(), "");
            webLink.setTarget("_blank");
            webLink.getStyle()
                    .set("text-decoration", "none")
                    .set("color", "inherit");
            webLink.add(nameHeading);
            headerContainer.add(webLink);
        } else {
            headerContainer.add(nameHeading);
        }
    }

    private void updateTabContent() {
        contentContainer.removeAll();
        if (currentProject == null) {
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
                projectContributions,
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
        if (currentProject == null || projectAnalyticsService == null) {
            return;
        }
        LocalDate start = from != null && from.equals(LocalDate.MIN) ? LocalDate.now().minusYears(10) : from;
        ProjectOverviewAnalytics analytics = projectAnalyticsService.getProjectOverviewAnalytics(
                currentProject.getId(),
                start,
                to
        );
        container.removeAll();
        if (analytics != null) {
            container.add(new ProjectOverviewAnalyticsComponent(analytics));
        }
    }

    private Component createActivityContent() {
        return new ProjectActivityComponent(
                currentProject,
                projectContributionService,
                projectActivityService,
                projectContributions,
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
