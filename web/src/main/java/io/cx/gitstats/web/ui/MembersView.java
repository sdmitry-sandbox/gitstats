package io.cx.gitstats.web.ui;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.cx.gitstats.domain.model.User;
import io.cx.gitstats.domain.service.UserService;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static io.cx.gitstats.web.ui.utils.UiUtils.DEFAULT_PAGE_WIDTH;

@PageTitle("Members | GitStats")
@Route(value = "members", layout = MainLayout.class)
public class MembersView extends VerticalLayout {

    private final UserService userService;
    private final Grid<User> grid = new Grid<>(User.class, false);

    public MembersView(UserService userService) {
        this.userService = userService;

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
        contentContainer.add(grid);
        contentContainer.expand(grid);

        add(contentContainer);
        expand(contentContainer);
        refreshGrid();
    }

    public MembersView() {
        this(null);
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT);

        grid.addComponentColumn(this::createUsernameLink)
                .setHeader("Username")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(User::getName)
                .setHeader("Name")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addComponentColumn(this::createBotBadge)
                .setHeader("Type")
                .setAutoWidth(true);
    }

    private Anchor createUsernameLink(User user) {
        String username = user.getUsername() != null ? user.getUsername() : "";
        Anchor link = new Anchor("user/" + username, username);
        link.getStyle()
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-weight", "500")
                .set("text-decoration", "none");
        return link;
    }

    private Span createBotBadge(User user) {
        boolean isBot = Boolean.TRUE.equals(user.getIsBot());
        Span badge = new Span(isBot ? "Bot" : "User");
        badge.getElement().getThemeList().add("badge");
        if (isBot) {
            badge.getElement().getThemeList().add("contrast");
        } else {
            badge.getElement().getThemeList().add("success");
        }
        return badge;
    }

    private void refreshGrid() {
        if (userService == null) {
            return;
        }
        List<User> users = userService.findActiveContributors();
        if (users != null) {
            List<User> sortedUsers = users.stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(
                            u -> u.getUsername() != null ? u.getUsername() : "",
                            String.CASE_INSENSITIVE_ORDER))
                    .toList();
            grid.setItems(sortedUsers);
        }
    }
}
