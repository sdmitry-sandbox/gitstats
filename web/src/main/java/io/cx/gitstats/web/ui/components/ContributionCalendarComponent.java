package io.cx.gitstats.web.ui.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.cx.gitstats.domain.model.ContributionThresholds;
import io.cx.gitstats.domain.model.DailyContribution;
import io.cx.gitstats.domain.model.User;
import io.cx.gitstats.domain.service.UserContributionService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ContributionCalendarComponent extends VerticalLayout {

    private static final String[] COLORS = {
            "#ededed", // Level 0: 0
            "#acd5f2", // Level 1
            "#7fa8d1", // Level 2
            "#4977b4", // Level 3
            "#1b4f8a"  // Level 4
    };

    private enum TimeFilterType {
        CURRENT_YEAR,
        SPECIFIC_YEAR,
        ALL_THE_TIME,
        LAST_YEAR,
        LAST_HALF_YEAR,
        LAST_MONTH,
        LAST_WEEK
    }

    private record TimeFilterOption(String label, TimeFilterType type, int year) {}

    private record CalendarRange(
            LocalDate gridStartDate,
            LocalDate gridEndDate,
            int numWeeks,
            LocalDate filterStartDate,
            LocalDate filterEndDate,
            boolean isYearMode,
            int year,
            String titleSuffix
    ) {}

    private final Map<LocalDate, DailyContribution> contributionsMap = new HashMap<>();

    private final boolean showSettings;
    private final boolean showMrCheckbox;
    private boolean includeMr;
    private boolean includeApprovals;
    private boolean includeComments;

    private Span titleSpan;
    private Div calendarCard;
    private Div periodPopup;
    private Div settingsPopup;
    private Div dropdownBackdrop;
    private Button periodBtn;
    private TimeFilterOption activeFilter;
    private final List<PeriodChangeListener> periodChangeListeners = new ArrayList<>();
    private final List<DayClickListener> dayClickListeners = new ArrayList<>();
    private final List<SettingsChangeListener> settingsChangeListeners = new ArrayList<>();

    @FunctionalInterface
    public interface PeriodChangeListener {
        void onPeriodChanged(LocalDate filterStartDate, LocalDate filterEndDate);
    }

    @FunctionalInterface
    public interface DayClickListener {
        void onDayClicked(LocalDate day, boolean includeMr, boolean includeApprovals, boolean includeComments);
    }

    @FunctionalInterface
    public interface SettingsChangeListener {
        void onSettingsChanged(boolean includeMr, boolean includeApprovals, boolean includeComments);
    }

    public ContributionCalendarComponent(
            List<DailyContribution> contributions,
            boolean showSettings,
            boolean showMrCheckbox,
            boolean initialMr,
            boolean initialApprovals,
            boolean initialComments
    ) {
        this(null, contributions, null, showSettings, showMrCheckbox, initialMr, initialApprovals, initialComments);
    }

    public ContributionCalendarComponent(
            User user,
            List<DailyContribution> contributions,
            UserContributionService userContributionService,
            boolean showSettings,
            boolean showMrCheckbox,
            boolean initialMr,
            boolean initialApprovals,
            boolean initialComments
    ) {
        this.showSettings = showSettings;
        this.showMrCheckbox = showMrCheckbox;
        this.includeMr = initialMr;
        this.includeApprovals = initialApprovals;
        this.includeComments = initialComments;

        if (contributions != null) {
            for (DailyContribution dc : contributions) {
                if (dc.getDate() != null) {
                    contributionsMap.put(dc.getDate(), dc);
                }
            }
        }

        List<TimeFilterOption> options = createFilterOptions();
        this.activeFilter = options.stream()
                .filter(opt -> opt.type() == TimeFilterType.LAST_YEAR)
                .findFirst()
                .orElse(options.isEmpty() ? null : options.get(0));

        setWidthFull();
        setPadding(false);
        setSpacing(true);

        initComponent();
    }

    private void initComponent() {
        calendarCard = new Div();
        styleCalendarCard(calendarCard);

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

        HorizontalLayout headerLayout = buildHeaderLayout();
        add(headerLayout, calendarCard, dropdownBackdrop);

        renderCalendar();
    }

    private void closePopups() {
        if (periodPopup != null) {
            periodPopup.setVisible(false);
        }
        if (settingsPopup != null) {
            settingsPopup.setVisible(false);
        }
        if (dropdownBackdrop != null) {
            dropdownBackdrop.setVisible(false);
        }
    }

    private HorizontalLayout buildHeaderLayout() {
        titleSpan = new Span();
        titleSpan.getStyle()
                .set("font-size", "1.1rem")
                .set("font-weight", "600")
                .set("color", "#1f1e24");

        HorizontalLayout controlsLayout = new HorizontalLayout();
        controlsLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        controlsLayout.setSpacing(true);

        controlsLayout.add(buildPeriodDropdown());
        if (showSettings) {
            controlsLayout.add(buildSettingsDropdown());
        }

        HorizontalLayout headerLayout = new HorizontalLayout(titleSpan, controlsLayout);
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        return headerLayout;
    }

    private Div buildPeriodDropdown() {
        Div container = new Div();
        container.getStyle()
                .set("position", "relative")
                .set("z-index", "1000");

        String label = activeFilter != null ? activeFilter.label() : "Period";
        periodBtn = new Button(label, VaadinIcon.CHEVRON_DOWN.create());
        periodBtn.setIconAfterText(true);
        periodBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        styleDropdownButton(periodBtn);

        periodPopup = createPeriodPopup();
        periodBtn.addClickListener(e -> {
            boolean nextVisible = !periodPopup.isVisible();
            periodPopup.setVisible(nextVisible);
            if (nextVisible) {
                if (settingsPopup != null) {
                    settingsPopup.setVisible(false);
                }
                dropdownBackdrop.setVisible(true);
            } else {
                dropdownBackdrop.setVisible(false);
            }
        });

        container.add(periodBtn, periodPopup);
        return container;
    }

    private Div buildSettingsDropdown() {
        Div container = new Div();
        container.getStyle()
                .set("position", "relative")
                .set("z-index", "1000");

        Button settingsBtn = new Button("Contribution settings", VaadinIcon.CHEVRON_DOWN.create());
        settingsBtn.setIconAfterText(true);
        settingsBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        styleDropdownButton(settingsBtn);

        settingsPopup = createSettingsPopup();
        settingsBtn.addClickListener(e -> {
            boolean nextVisible = !settingsPopup.isVisible();
            settingsPopup.setVisible(nextVisible);
            if (nextVisible) {
                if (periodPopup != null) {
                    periodPopup.setVisible(false);
                }
                dropdownBackdrop.setVisible(true);
            } else {
                dropdownBackdrop.setVisible(false);
            }
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

    private Div createPeriodPopup() {
        Div popup = new Div();
        popup.setVisible(false);
        popup.getStyle()
                .set("position", "absolute")
                .set("left", "0")
                .set("top", "100%")
                .set("background", "#ffffff")
                .set("border", "1px solid #dcdcde")
                .set("border-radius", "6px")
                .set("box-shadow", "0 4px 12px rgba(0,0,0,0.1)")
                .set("padding", "6px")
                .set("z-index", "1000")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "2px")
                .set("min-width", "130px");

        List<TimeFilterOption> options = createFilterOptions();
        for (TimeFilterOption opt : options) {
            Button itemBtn = new Button(opt.label());
            itemBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            itemBtn.getStyle()
                    .set("font-size", "12px")
                    .set("color", "#1f1e24")
                    .set("justify-content", "flex-start")
                    .set("text-align", "left")
                    .set("padding-left", "8px")
                    .set("padding-right", "8px")
                    .set("width", "100%")
                    .set("cursor", "pointer");

            itemBtn.addClickListener(e -> {
                selectFilter(opt);
                closePopups();
            });
            popup.add(itemBtn);
        }
        return popup;
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

        populateSettingsCheckboxes(popup);
        return popup;
    }

    private void populateSettingsCheckboxes(Div popup) {
        if (showMrCheckbox) {
            Checkbox mrCb = createSettingCheckbox("merge requests", includeMr);
            mrCb.addValueChangeListener(e -> {
                includeMr = e.getValue();
                renderCalendar();
                notifySettingsChanged();
            });
            popup.add(mrCb);
        }

        Checkbox appCb = createSettingCheckbox("approvals", includeApprovals);
        appCb.addValueChangeListener(e -> {
            includeApprovals = e.getValue();
            renderCalendar();
            notifySettingsChanged();
        });
        popup.add(appCb);

        Checkbox comCb = createSettingCheckbox("comments", includeComments);
        comCb.addValueChangeListener(e -> {
            includeComments = e.getValue();
            renderCalendar();
            notifySettingsChanged();
        });
        popup.add(comCb);
    }

    private Checkbox createSettingCheckbox(String label, boolean initialValue) {
        Checkbox cb = new Checkbox(label, initialValue);
        cb.getStyle().set("font-size", "12px");
        return cb;
    }

    private List<TimeFilterOption> createFilterOptions() {
        List<TimeFilterOption> options = new ArrayList<>();
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();

        int oldestYear = currentYear;
        if (!contributionsMap.isEmpty()) {
            oldestYear = contributionsMap.keySet().stream()
                    .min(Comparator.naturalOrder())
                    .map(LocalDate::getYear)
                    .orElse(currentYear);
        }

        options.add(new TimeFilterOption(String.valueOf(currentYear), TimeFilterType.CURRENT_YEAR, currentYear));
        for (int y = currentYear - 1; y >= oldestYear; y--) {
            options.add(new TimeFilterOption(String.valueOf(y), TimeFilterType.SPECIFIC_YEAR, y));
        }

        options.add(new TimeFilterOption("All the time", TimeFilterType.ALL_THE_TIME, 0));
        options.add(new TimeFilterOption("Last year", TimeFilterType.LAST_YEAR, 0));
        options.add(new TimeFilterOption("Last half year", TimeFilterType.LAST_HALF_YEAR, 0));
        options.add(new TimeFilterOption("Last month", TimeFilterType.LAST_MONTH, 0));
        options.add(new TimeFilterOption("Last week", TimeFilterType.LAST_WEEK, 0));
        return options;
    }

    private void selectFilter(TimeFilterOption option) {
        this.activeFilter = option;
        if (periodBtn != null) {
            periodBtn.setText(option.label());
        }
        renderCalendar();
    }

    private void styleCalendarCard(Div card) {
        card.setWidthFull();
        card.getStyle()
                .set("background", "#ffffff")
                .set("border", "1px solid #dcdcde")
                .set("border-radius", "6px")
                .set("padding", "16px 20px")
                .set("box-sizing", "border-box")
                .set("overflow-x", "auto");
    }

    public void addPeriodChangeListener(PeriodChangeListener listener) {
        if (listener != null) {
            periodChangeListeners.add(listener);
        }
    }

    public void addDayClickListener(DayClickListener listener) {
        if (listener != null) {
            dayClickListeners.add(listener);
        }
    }

    public void addSettingsChangeListener(SettingsChangeListener listener) {
        if (listener != null) {
            settingsChangeListeners.add(listener);
        }
    }

    private void notifySettingsChanged() {
        for (SettingsChangeListener listener : settingsChangeListeners) {
            listener.onSettingsChanged(includeMr, includeApprovals, includeComments);
        }
    }

    private void notifyDayClick(LocalDate day) {
        for (DayClickListener listener : dayClickListeners) {
            listener.onDayClicked(day, includeMr, includeApprovals, includeComments);
        }
    }

    public boolean isIncludeMr() {
        return includeMr;
    }

    public boolean isIncludeApprovals() {
        return includeApprovals;
    }

    public boolean isIncludeComments() {
        return includeComments;
    }

    public LocalDate getCurrentFilterStartDate() {
        return calculateDateRange(LocalDate.now()).filterStartDate();
    }

    public LocalDate getCurrentFilterEndDate() {
        return calculateDateRange(LocalDate.now()).filterEndDate();
    }

    private void renderCalendar() {
        calendarCard.removeAll();
        LocalDate today = LocalDate.now();

        CalendarRange range = calculateDateRange(today);
        int total = calculateTotalContributions(range.filterStartDate(), range.filterEndDate());

        titleSpan.setText(total + " contributions" + range.titleSuffix());

        ContributionThresholds thresholds = calculateThresholds();

        Div monthsRow = buildMonthsRow(range.gridStartDate(), range.numWeeks(), range.isYearMode(), range.year());
        Div heatmapGrid = buildHeatmapGrid(range, today, thresholds);
        HorizontalLayout bottomRow = buildBottomRow(thresholds);

        calendarCard.add(monthsRow, heatmapGrid, bottomRow);

        for (PeriodChangeListener listener : periodChangeListeners) {
            listener.onPeriodChanged(range.filterStartDate(), range.filterEndDate());
        }
    }

    private ContributionThresholds calculateThresholds() {
        List<Integer> counts = new ArrayList<>();
        for (DailyContribution dc : contributionsMap.values()) {
            counts.add(dc.getCount(includeMr, includeApprovals, includeComments));
        }
        return ContributionThresholds.calculate(counts);
    }

    private CalendarRange calculateDateRange(LocalDate today) {
        if (activeFilter == null) {
            return calculateYearDateRange(today.getYear());
        }
        return switch (activeFilter.type()) {
            case CURRENT_YEAR -> calculateYearDateRange(activeFilter.year());
            case SPECIFIC_YEAR -> calculateYearDateRange(activeFilter.year());
            case ALL_THE_TIME -> calculateRollingDateRange(LocalDate.MIN, today, " in all the time");
            case LAST_YEAR -> calculateRollingDateRange(today.minusWeeks(52), today, " in the last year");
            case LAST_HALF_YEAR -> calculateRollingDateRange(today.minusMonths(6), today, " in the last half year");
            case LAST_MONTH -> calculateRollingDateRange(today.minusMonths(1), today, " in the last month");
            case LAST_WEEK -> calculateRollingDateRange(today.minusWeeks(1), today, " in the last week");
        };
    }

    private CalendarRange calculateYearDateRange(int year) {
        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate gridStart = startOfYear.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate gridEnd = LocalDate.of(year, 12, 31);
        int numWeeks = calculateWeeks(gridStart, gridEnd);
        return new CalendarRange(
                gridStart, gridEnd, numWeeks, startOfYear, gridEnd, true, year, " in " + year
        );
    }

    private CalendarRange calculateRollingDateRange(LocalDate filterStart, LocalDate today, String suffix) {
        LocalDate gridStart = today.minusWeeks(52).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate gridEnd = gridStart.plusWeeks(53).minusDays(1);
        return new CalendarRange(
                gridStart, gridEnd, 53, filterStart, today, false, 0, suffix
        );
    }

    private int calculateWeeks(LocalDate start, LocalDate end) {
        int count = 0;
        LocalDate cur = start;
        while (!cur.isAfter(end)) {
            count++;
            cur = cur.plusWeeks(1);
        }
        return Math.max(count, 53);
    }

    private int calculateTotalContributions(LocalDate filterStartDate, LocalDate filterEndDate) {
        int total = 0;
        for (Map.Entry<LocalDate, DailyContribution> entry : contributionsMap.entrySet()) {
            LocalDate d = entry.getKey();
            if (!d.isBefore(filterStartDate) && !d.isAfter(filterEndDate)) {
                total += entry.getValue().getCount(includeMr, includeApprovals, includeComments);
            }
        }
        return total;
    }

    private int getContributionCountForDay(LocalDate day) {
        DailyContribution dc = contributionsMap.get(day);
        if (dc == null) return 0;
        return dc.getCount(includeMr, includeApprovals, includeComments);
    }

    private Div buildMonthsRow(LocalDate startDate, int numWeeks, boolean isYearMode, int year) {
        Div monthsRow = new Div();
        monthsRow.getStyle()
                .set("display", "flex")
                .set("padding-left", "32px")
                .set("height", "18px")
                .set("align-items", "center")
                .set("position", "relative");

        int lastMonth = -1;
        for (int w = 0; w < numWeeks; w++) {
            LocalDate weekStart = startDate.plusWeeks(w);
            if (isYearMode && weekStart.getYear() != year && weekStart.plusDays(6).getYear() != year) {
                continue;
            }
            int monthValue = (isYearMode && weekStart.getYear() != year)
                    ? weekStart.plusDays(6).getMonthValue()
                    : weekStart.getMonthValue();
            if (monthValue != lastMonth) {
                lastMonth = monthValue;
                String monthName = (isYearMode && weekStart.getYear() != year)
                        ? weekStart.plusDays(6).getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                        : weekStart.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                Span monthLabel = new Span(monthName);
                monthLabel.getStyle()
                        .set("font-size", "11px")
                        .set("color", "#6e6d7a")
                        .set("position", "absolute")
                        .set("left", (32 + w * 15) + "px");
                monthsRow.add(monthLabel);
            }
        }
        return monthsRow;
    }

    private Div buildHeatmapGrid(CalendarRange range, LocalDate today, ContributionThresholds thresholds) {
        Div grid = new Div();
        grid.getStyle()
                .set("display", "flex")
                .set("gap", "4px")
                .set("align-items", "flex-start")
                .set("margin-top", "4px");

        grid.add(buildDayLabelsColumn());

        for (int w = 0; w < range.numWeeks(); w++) {
            LocalDate weekMonday = range.gridStartDate().plusWeeks(w);
            grid.add(buildWeekColumn(weekMonday, range, today, thresholds));
        }
        return grid;
    }

    private Div buildDayLabelsColumn() {
        Div col = new Div();
        col.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "4px")
                .set("width", "28px")
                .set("text-align", "right")
                .set("padding-right", "4px");

        String[] labels = {"", "Mon", "", "Wed", "", "Fri", ""};
        for (String label : labels) {
            Span s = new Span(label);
            s.getStyle()
                    .set("font-size", "10px")
                    .set("color", "#8c8b96")
                    .set("height", "11px")
                    .set("line-height", "11px");
            col.add(s);
        }
        return col;
    }

    private Div buildWeekColumn(
            LocalDate weekMonday,
            CalendarRange range,
            LocalDate today,
            ContributionThresholds thresholds
    ) {
        Div col = new Div();
        col.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "4px");

        for (int d = 0; d < 7; d++) {
            LocalDate day = weekMonday.plusDays(d);
            col.add(buildDayCell(day, range, today, thresholds));
        }
        return col;
    }

    private Div buildDayCell(
            LocalDate day,
            CalendarRange range,
            LocalDate today,
            ContributionThresholds thresholds
    ) {
        Div cell = new Div();
        if (range.isYearMode()) {
            if (day.getYear() != range.year()) {
                styleHiddenCell(cell);
            } else if (day.isAfter(today)) {
                styleDashedCell(cell);
            } else {
                int count = getContributionCountForDay(day);
                styleFilledCell(cell, count, thresholds, day);
            }
        } else {
            if (day.isAfter(today)) {
                styleHiddenCell(cell);
            } else if (day.isBefore(range.filterStartDate())) {
                styleDashedCell(cell);
            } else {
                int count = getContributionCountForDay(day);
                styleFilledCell(cell, count, thresholds, day);
            }
        }
        return cell;
    }

    private void styleFilledCell(Div cell, int count, ContributionThresholds thresholds, LocalDate day) {
        int level = thresholds != null ? thresholds.getLevel(count) : 0;
        cell.getStyle()
                .set("width", "11px")
                .set("height", "11px")
                .set("border-radius", "2px")
                .set("background-color", COLORS[level])
                .set("cursor", "pointer");
        String tooltip = count == 0
                ? "No contributions on " + day
                : count + " contribution" + (count > 1 ? "s" : "") + " on " + day;
        cell.getElement().setAttribute("title", tooltip);
        cell.addClickListener(e -> notifyDayClick(day));
    }

    private void styleDashedCell(Div cell) {
        cell.getStyle()
                .set("width", "11px")
                .set("height", "11px")
                .set("border", "1px dashed #dcdcde")
                .set("box-sizing", "border-box")
                .set("border-radius", "2px")
                .set("background-color", "transparent");
    }

    private void styleHiddenCell(Div cell) {
        cell.getStyle()
                .set("width", "11px")
                .set("height", "11px")
                .set("visibility", "hidden");
    }

    private HorizontalLayout buildBottomRow(ContributionThresholds thresholds) {
        HorizontalLayout bottomRow = new HorizontalLayout();
        bottomRow.setWidthFull();
        bottomRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        bottomRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        bottomRow.getStyle().set("margin-top", "12px");

        bottomRow.add(buildLegend(thresholds));

        Span text = new Span(formatSelectedContributions());
        text.getStyle().set("font-size", "11px").set("color", "#8c8b96");
        bottomRow.add(text);
        return bottomRow;
    }

    private HorizontalLayout buildLegend(ContributionThresholds thresholds) {
        HorizontalLayout legend = new HorizontalLayout();
        legend.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        legend.setSpacing(true);
        legend.getStyle().set("gap", "3px");

        Span less = new Span("Less");
        less.getStyle().set("font-size", "11px").set("color", "#8c8b96");
        legend.add(less);

        for (int i = 0; i < 5; i++) {
            Div box = new Div();
            box.getStyle()
                    .set("width", "10px")
                    .set("height", "10px")
                    .set("border-radius", "2px")
                    .set("background-color", COLORS[i]);
            box.getElement().setAttribute("title", getLegendTooltip(i, thresholds));
            legend.add(box);
        }

        Span more = new Span("More");
        more.getStyle().set("font-size", "11px").set("color", "#8c8b96");
        legend.add(more);
        return legend;
    }

    private String getLegendTooltip(int level, ContributionThresholds thresholds) {
        if (thresholds == null) return "";
        return switch (level) {
            case 0 -> "0 contributions";
            case 1 -> "1 - " + thresholds.getLevel1Max() + " contributions";
            case 2 -> (thresholds.getLevel1Max() + 1) + " - " + thresholds.getLevel2Max() + " contributions";
            case 3 -> (thresholds.getLevel2Max() + 1) + " - " + thresholds.getLevel3Max() + " contributions";
            case 4 -> (thresholds.getLevel3Max() + 1) + "+ contributions";
            default -> "";
        };
    }

    private String formatSelectedContributions() {
        List<String> selected = new ArrayList<>();
        if (includeMr) selected.add("merge requests");
        if (includeApprovals) selected.add("approvals");
        if (includeComments) selected.add("comments");

        if (selected.isEmpty()) {
            return "No contributions selected.";
        }
        if (selected.size() == 1) {
            return capitalize(selected.get(0)) + ".";
        }
        return capitalize(String.join(", ", selected.subList(0, selected.size() - 1)))
                + ", and " + selected.get(selected.size() - 1) + ".";
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
