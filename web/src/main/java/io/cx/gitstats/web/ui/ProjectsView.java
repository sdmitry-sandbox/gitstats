package io.cx.gitstats.web.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import io.cx.gitstats.domain.model.Project;
import io.cx.gitstats.domain.service.ProjectService;
import io.cx.gitstats.web.mapper.WebMapper;
import io.cx.gitstats.web.messages.WebMessages;
import io.cx.gitstats.web.model.ProjectWebModel;
import io.cx.gitstats.web.service.DefaultProjectWebService;
import io.cx.gitstats.web.ui.components.activity.ProjectActivityComponent;
import io.cx.gitstats.web.ui.utils.UiUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static io.cx.gitstats.web.ui.utils.UiUtils.DEFAULT_PAGE_WIDTH;

@PageTitle("Projects | GitStats")
@Route(value = "projects", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
public class ProjectsView extends VerticalLayout {

    private final ProjectService projectService;
    private final DefaultProjectWebService defaultProjectWebService;
    private final WebMapper webMapper;
    private final Grid<ProjectWebModel> grid = new Grid<>(ProjectWebModel.class, false);

    public ProjectsView(ProjectService projectService,
                        DefaultProjectWebService defaultProjectWebService,
                        WebMapper webMapper) {
        this.projectService = projectService;
        this.defaultProjectWebService = defaultProjectWebService;
        this.webMapper = webMapper;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);

        VerticalLayout contentContainer = new VerticalLayout();
        contentContainer.setWidthFull();
        contentContainer.setMaxWidth(DEFAULT_PAGE_WIDTH);
        contentContainer.setSizeFull();
        contentContainer.setPadding(false);
        contentContainer.setSpacing(true);

        configureGrid();
        HorizontalLayout addProjectLayout = createAddProjectLayout();

        contentContainer.add(grid, addProjectLayout);
        contentContainer.expand(grid);

        add(contentContainer);
        expand(contentContainer);
        refreshGrid();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);

        grid.addColumn(ProjectWebModel::gitlabId)
                .setHeader("GitLab ID")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addComponentColumn(this::createNameLink).setHeader("Name").setSortable(true).setAutoWidth(true);
        grid.addComponentColumn(this::createStatusBadge).setHeader("Status").setSortable(true).setAutoWidth(true);
        grid.addComponentColumn(this::createLastSyncedAtSpan).setHeader("LastSynced At").setSortable(true).setAutoWidth(true);
        grid.addComponentColumn(this::createDeleteButton).setHeader("Actions").setAutoWidth(true);
    }

    private com.vaadin.flow.component.Component createNameLink(ProjectWebModel project) {
        String name = project.name() != null && !project.name().isBlank() ? project.name() : "-";
        if (project.id() != null) {
            Anchor link = new Anchor("project/" + project.id(), name);
            link.getStyle()
                    .set("color", "var(--lumo-primary-text-color)")
                    .set("font-weight", "500")
                    .set("text-decoration", "none");
            return link;
        }
        return new Span(name);
    }

    private Span createStatusBadge(ProjectWebModel project) {
        Span statusBadge = new Span(project.status() != null ? project.status().name() : "");
        statusBadge.getElement().getThemeList().add("badge");
        if (project.status() != null) {
            switch (project.status()) {
                case SYNCED -> statusBadge.getElement().getThemeList().add("success");
                case CREATING -> statusBadge.getElement().getThemeList().add("warning");
            }
        }
        return statusBadge;
    }

    private Span createLastSyncedAtSpan(ProjectWebModel project) {
        if (project.lastSyncedAt() == null) {
            return new Span("");
        }
        Span span = new Span(ProjectActivityComponent.formatRelativeTime(project.lastSyncedAt()));
        span.getStyle()
                .set("color", "#8c8b96")
                .set("font-size", "13px")
                .set("white-space", "nowrap");
        span.getElement().setAttribute("title", ProjectActivityComponent.formatMoscowDateTime(project.lastSyncedAt()));
        return span;
    }

    private Button createDeleteButton(ProjectWebModel project) {
        Button deleteBtn = new Button(VaadinIcon.TRASH.create());
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        deleteBtn.addClickListener(e -> {
            projectService.deleteById(project.id());
            UiUtils.showSuccessNotification(WebMessages.PROJECT_DELETED);
            refreshGrid();
        });
        return deleteBtn;
    }

    private HorizontalLayout createAddProjectLayout() {
        TextField gitlabIdField = new TextField("GitLab ID");
        gitlabIdField.setPlaceholder("e.g. 12345");
        gitlabIdField.setClearButtonVisible(true);

        Button addButton = new Button("Add Project", VaadinIcon.PLUS.create());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> handleAddProject(gitlabIdField));

        Button addDefaultProjectsButton = new Button("Add defailt projects");
        addDefaultProjectsButton.addClickListener(e -> {
            defaultProjectWebService.addDefaultProjects();
            refreshGrid();
        });

        Button refreshButton = new Button("Refresh", VaadinIcon.REFRESH.create());
        refreshButton.addClickListener(e -> refreshGrid());

        HorizontalLayout layout = new HorizontalLayout(
                gitlabIdField, addButton, addDefaultProjectsButton, refreshButton
        );
        layout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        layout.setSpacing(true);
        return layout;
    }

    private void handleAddProject(TextField gitlabIdField) {
        String value = gitlabIdField.getValue();
        if (value == null || value.trim().isEmpty()) {
            UiUtils.showErrorNotification(WebMessages.PLEASE_ENTER_VALID_GITLAB_ID);
            return;
        }

        try {
            Long gitlabId = Long.parseLong(value.trim());
            projectService.createAndScheduleSync(gitlabId);
            gitlabIdField.clear();
            UiUtils.showSuccessNotification(WebMessages.PROJECT_CREATED_AND_SYNC_SCHEDULED);
            refreshGrid();
        } catch (NumberFormatException ex) {
            UiUtils.showErrorNotification(WebMessages.GITLAB_ID_MUST_BE_A_NUMBER);
        }
    }

    private void refreshGrid() {
        List<Project> projects = projectService.findAll();
        List<ProjectWebModel> webModels = projects.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(
                        p -> p.getName() != null ? p.getName() : "",
                        String.CASE_INSENSITIVE_ORDER))
                .map(webMapper::toWebModel)
                .toList();
        grid.setItems(webModels);
    }
}
