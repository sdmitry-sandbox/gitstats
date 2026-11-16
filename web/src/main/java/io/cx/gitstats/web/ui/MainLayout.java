package io.cx.gitstats.web.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.spring.annotation.UIScope;
import io.cx.gitstats.domain.model.Project;
import io.cx.gitstats.domain.model.User;
import io.cx.gitstats.domain.service.ProjectService;
import io.cx.gitstats.domain.service.UserService;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static io.cx.gitstats.web.ui.utils.UiUtils.DEFAULT_PAGE_WIDTH;

@UIScope
@Component
public class MainLayout extends AppLayout implements BeforeEnterObserver {

    private final ProjectService projectService;
    private final UserService userService;

    private SideNavItem projectsItem;
    private SideNavItem membersItem;

    public MainLayout(ProjectService projectService, UserService userService) {
        this.projectService = projectService;
        this.userService = userService;
        createHeader();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        createDrawer();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Class<?> target = event.getNavigationTarget();
        String path = event.getLocation().getPath();
        boolean isUserPage = UserView.class.equals(target) || path.startsWith("user");
        boolean isProjectPage = ProjectView.class.equals(target) || path.startsWith("project");

        if (projectsItem != null) {
            projectsItem.setExpanded(!isUserPage);
        }
        if (membersItem != null) {
            membersItem.setExpanded(!isProjectPage);
        }
    }

    private void createHeader() {
        HorizontalLayout brandBlock = createBrandBlock();
        HorizontalLayout navContent = createNavContent();

        HorizontalLayout header = new HorizontalLayout(brandBlock, navContent);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.setHeight("var(--lumo-size-xl)");
        header.setSpacing(false);
        header.setPadding(false);
        header.addClassNames("py-0", "px-0");
        header.getStyle()
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                .set("background-color", "var(--lumo-base-color)");

        addToNavbar(header);
    }

    private HorizontalLayout createBrandBlock() {
        DrawerToggle drawerToggle = new DrawerToggle();
        drawerToggle.getStyle()
                .set("color", "#ffffff")
                .set("margin-right", "var(--lumo-space-s)");

        H1 logo = new H1("GitStats");
        logo.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("margin", "0")
                .set("color", "#ffffff")
                .set("font-weight", "600");

        HorizontalLayout brandBlock = new HorizontalLayout(drawerToggle, logo);
        brandBlock.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        brandBlock.setWidth("16rem");
        brandBlock.setHeightFull();
        brandBlock.setPadding(false);
        brandBlock.setSpacing(false);
        brandBlock.getStyle()
                .set("background-color", "#292961")
                .set("padding-left", "var(--lumo-space-m)")
                .set("padding-right", "var(--lumo-space-m)")
                .set("box-sizing", "border-box")
                .set("display", "flex")
                .set("align-items", "center");
        return brandBlock;
    }

    private HorizontalLayout createNavContent() {
        HorizontalLayout searchWrapper = new HorizontalLayout(createSearchComponent());
        searchWrapper.setWidthFull();
        searchWrapper.setMaxWidth(DEFAULT_PAGE_WIDTH);
        searchWrapper.setPadding(false);
        searchWrapper.setSpacing(false);
        searchWrapper.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        HorizontalLayout navContent = new HorizontalLayout(searchWrapper);
        navContent.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        navContent.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        navContent.setHeightFull();
        navContent.getStyle()
                .set("flex-grow", "1")
                .set("padding-left", "var(--lumo-space-l)")
                .set("padding-right", "var(--lumo-space-l)")
                .set("box-sizing", "border-box");
        return navContent;
    }

    private Div createSearchComponent() {
        Div searchContainer = new Div();
        searchContainer.getStyle()
                .set("position", "relative")
                .set("width", "360px")
                .set("max-width", "100%")
                .set("display", "flex")
                .set("align-items", "center");

        TextField searchField = new TextField();
        searchField.setPlaceholder("Search projects or members...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.setWidthFull();
        searchField.addThemeVariants(TextFieldVariant.LUMO_SMALL);

        Div resultsPopup = new Div();
        styleResultsPopup(resultsPopup);
        Div searchBackdrop = createSearchBackdrop(resultsPopup);

        searchField.addValueChangeListener(e ->
                handleSearchInput(e.getValue(), searchField, resultsPopup, searchBackdrop));

        searchContainer.add(searchField, resultsPopup, searchBackdrop);
        return searchContainer;
    }

    private void styleResultsPopup(Div popup) {
        popup.setVisible(false);
        popup.getStyle()
                .set("position", "absolute")
                .set("top", "calc(100% + 4px)")
                .set("left", "0")
                .set("width", "100%")
                .set("max-height", "350px")
                .set("overflow-y", "auto")
                .set("background", "#ffffff")
                .set("border", "1px solid #dcdcde")
                .set("border-radius", "6px")
                .set("box-shadow", "0 4px 16px rgba(0,0,0,0.15)")
                .set("z-index", "1000")
                .set("padding", "6px 0");
    }

    private Div createSearchBackdrop(Div resultsPopup) {
        Div backdrop = new Div();
        backdrop.setVisible(false);
        backdrop.getStyle()
                .set("position", "fixed")
                .set("top", "0")
                .set("left", "0")
                .set("width", "100vw")
                .set("height", "100vh")
                .set("z-index", "999")
                .set("background", "transparent");
        backdrop.addClickListener(e -> {
            resultsPopup.setVisible(false);
            backdrop.setVisible(false);
        });
        return backdrop;
    }

    private void handleSearchInput(String query, TextField searchField, Div resultsPopup, Div backdrop) {
        String trimmed = query != null ? query.trim() : "";
        if (trimmed.length() < 3) {
            resultsPopup.removeAll();
            resultsPopup.setVisible(false);
            backdrop.setVisible(false);
            return;
        }

        List<Project> foundProjects = projectService != null ? projectService.searchByName(trimmed) : List.of();
        List<User> foundUsers = userService != null ? userService.searchByUsernameOrName(trimmed) : List.of();

        List<Project> sortedProjects = foundProjects.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(p -> p.getName() != null ? p.getName() : "", String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<User> sortedUsers = foundUsers.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(u -> u.getUsername() != null ? u.getUsername() : "", String.CASE_INSENSITIVE_ORDER))
                .toList();

        populateSearchResults(resultsPopup, searchField, backdrop, sortedProjects, sortedUsers);
    }

    private void populateSearchResults(Div resultsPopup, TextField searchField, Div backdrop,
                                      List<Project> projects, List<User> users) {
        resultsPopup.removeAll();
        if (projects.isEmpty() && users.isEmpty()) {
            resultsPopup.add(createNoResultsItem());
        } else {
            if (!projects.isEmpty()) {
                resultsPopup.add(createSearchSectionHeader("Projects"));
                for (Project p : projects) {
                    resultsPopup.add(createProjectResultItem(p, searchField, resultsPopup, backdrop));
                }
            }
            if (!users.isEmpty()) {
                resultsPopup.add(createSearchSectionHeader("Members"));
                for (User u : users) {
                    resultsPopup.add(createUserResultItem(u, searchField, resultsPopup, backdrop));
                }
            }
        }
        resultsPopup.setVisible(true);
        backdrop.setVisible(true);
    }

    private Div createSearchSectionHeader(String title) {
        Div header = new Div(new Span(title));
        header.getStyle()
                .set("font-size", "11px")
                .set("font-weight", "600")
                .set("color", "#6e6d7a")
                .set("text-transform", "uppercase")
                .set("padding", "6px 12px 2px 12px")
                .set("letter-spacing", "0.5px");
        return header;
    }

    private Div createProjectResultItem(Project project, TextField searchField, Div resultsPopup, Div backdrop) {
        String name = project.getName() != null && !project.getName().isBlank()
                ? project.getName()
                : (project.getId() != null ? "Project " + project.getId() : "Project");
        Div item = new Div();
        item.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("padding", "6px 12px")
                .set("cursor", "pointer")
                .set("font-size", "13px")
                .set("gap", "8px")
                .set("color", "#1f1e24");
        item.add(VaadinIcon.FOLDER_O.create(), new Span(name));
        item.getElement().addEventListener("mouseover", e -> item.getStyle().set("background-color", "#f0f4ff"));
        item.getElement().addEventListener("mouseout", e -> item.getStyle().set("background-color", "transparent"));
        item.addClickListener(e -> {
            searchField.clear();
            resultsPopup.setVisible(false);
            backdrop.setVisible(false);
            String path = project.getId() != null ? "project/" + project.getId() : "project";
            getUI().ifPresent(ui -> ui.navigate(path));
        });
        return item;
    }

    private Div createUserResultItem(User user, TextField searchField, Div resultsPopup, Div backdrop) {
        String username = user.getUsername() != null ? user.getUsername() : "";
        Div item = new Div();
        item.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("padding", "6px 12px")
                .set("cursor", "pointer")
                .set("font-size", "13px")
                .set("gap", "8px")
                .set("color", "#1f1e24");
        item.add(VaadinIcon.USER.create(), new Span(username));
        item.getElement().addEventListener("mouseover", e -> item.getStyle().set("background-color", "#f0f4ff"));
        item.getElement().addEventListener("mouseout", e -> item.getStyle().set("background-color", "transparent"));
        item.addClickListener(e -> {
            searchField.clear();
            resultsPopup.setVisible(false);
            backdrop.setVisible(false);
            getUI().ifPresent(ui -> ui.navigate("user/" + username));
        });
        return item;
    }

    private Div createNoResultsItem() {
        Div item = new Div(new Span("No results found"));
        item.getStyle()
                .set("padding", "8px 12px")
                .set("font-size", "13px")
                .set("color", "#6e6d7a");
        return item;
    }

    private void createDrawer() {
        SideNav nav = new SideNav();
        nav.setWidthFull();
        styleSideNav(nav);

        projectsItem = new SideNavItem("Projects", ProjectsView.class, VaadinIcon.FOLDER_O.create());
        styleParentItem(projectsItem);

        membersItem = new SideNavItem("Members", MembersView.class, VaadinIcon.USERS.create());
        styleParentItem(membersItem);

        if (projectService != null) {
            populateProjectItems(projectsItem);
        }
        if (userService != null) {
            populateMemberItems(membersItem);
        }

        nav.addItem(projectsItem);
        nav.addItem(membersItem);

        VerticalLayout drawerLayout = new VerticalLayout(nav);
        drawerLayout.setSizeFull();
        drawerLayout.setPadding(false);
        drawerLayout.setSpacing(false);
        drawerLayout.getStyle()
                .set("background-color", "#fbfafd")
                .set("color", "#1f1e24")
                .set("padding", "8px 10px")
                .set("border-right", "1px solid var(--lumo-contrast-10pct)");

        addToDrawer(drawerLayout);
    }

    private void styleSideNav(SideNav nav) {
        nav.getStyle()
                .set("--vaadin-side-nav-item-active-background", "#e8eeff")
                .set("--vaadin-side-nav-item-active-color", "#1068bf")
                .set("--vaadin-side-nav-item-color", "#1f1e24")
                .set("--vaadin-side-nav-item-icon-color", "#6e6d7a")
                .set("--vaadin-side-nav-item-hover-background", "#f0f4ff")
                .set("--vaadin-side-nav-item-border-radius", "6px")
                .set("--vaadin-side-nav-item-min-height", "20px")
                .set("font-weight", "500");
    }

    private void styleParentItem(SideNavItem item) {
        item.getStyle()
                .set("font-size", "13px")
                .set("font-weight", "600")
                .set("min-height", "28px")
                .set("line-height", "28px");
    }

    private void styleSubItem(SideNavItem item) {
        item.getStyle()
                .set("font-size", "12px")
                .set("font-weight", "400")
                .set("min-height", "20px")
                .set("line-height", "20px")
                .set("margin", "0");
    }

    private void populateProjectItems(SideNavItem parentItem) {
        List<Project> projects = projectService.findAll();
        if (projects == null) {
            return;
        }
        List<Project> sortedProjects = projects.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(
                        p -> p.getName() != null ? p.getName() : "",
                        String.CASE_INSENSITIVE_ORDER))
                .toList();

        for (Project project : sortedProjects) {
            String name = project.getName() != null && !project.getName().isBlank()
                    ? project.getName()
                    : (project.getId() != null ? "Project " + project.getId() : "Project");
            String path = project.getId() != null ? "project/" + project.getId() : "project";
            SideNavItem projectSubItem = new SideNavItem(name, path);
            styleSubItem(projectSubItem);
            parentItem.addItem(projectSubItem);
        }
    }

    private void populateMemberItems(SideNavItem parentItem) {
        List<User> activeUsers = userService.findActiveContributors();
        if (activeUsers == null || activeUsers.isEmpty()) {
            return;
        }
        List<User> sortedUsers = activeUsers.stream()
                .filter(Objects::nonNull)
                .filter(u -> u.getUsername() != null && !u.getUsername().isBlank())
                .sorted(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER))
                .toList();

        for (User user : sortedUsers) {
            SideNavItem userSubItem = new SideNavItem(user.getUsername(), "user/" + user.getUsername());
            styleSubItem(userSubItem);
            parentItem.addItem(userSubItem);
        }
    }
}
