package io.cx.gitstats.domain.service.analytics;

import java.time.*;

public final class WorkingDaysCalculator {

    public static final ZoneId MOSCOW_ZONE = ZoneId.of("Europe/Moscow");
    public static final LocalTime WORKDAY_START = LocalTime.of(10, 0);
    public static final LocalTime WORKDAY_END = LocalTime.of(19, 0);

    private WorkingDaysCalculator() {}

    public static int countWorkingDays(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            return 0;
        }
        int count = 0;
        LocalDate current = from;
        while (!current.isAfter(to)) {
            if (!isWeekend(current)) {
                count++;
            }
            current = current.plusDays(1);
        }
        return count;
    }

    public static boolean isWeekend(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    public static double calculateBusinessWorkingHours(Instant startInstant, Instant endInstant) {
        if (startInstant == null || endInstant == null || !endInstant.isAfter(startInstant)) {
            return 0.0;
        }
        ZonedDateTime start = startInstant.atZone(MOSCOW_ZONE);
        ZonedDateTime end = endInstant.atZone(MOSCOW_ZONE);

        long totalSeconds = 0;
        LocalDate current = start.toLocalDate();
        LocalDate lastDate = end.toLocalDate();

        while (!current.isAfter(lastDate)) {
            if (!isWeekend(current)) {
                totalSeconds += calculateDayOverlapSeconds(current, start, end);
            }
            current = current.plusDays(1);
        }
        return totalSeconds / 3600.0;
    }

    private static long calculateDayOverlapSeconds(LocalDate date, ZonedDateTime start, ZonedDateTime end) {
        ZonedDateTime dayWorkStart = date.atTime(WORKDAY_START).atZone(MOSCOW_ZONE);
        ZonedDateTime dayWorkEnd = date.atTime(WORKDAY_END).atZone(MOSCOW_ZONE);

        ZonedDateTime overlapStart = start.isAfter(dayWorkStart) ? start : dayWorkStart;
        ZonedDateTime overlapEnd = end.isBefore(dayWorkEnd) ? end : dayWorkEnd;

        if (overlapEnd.isAfter(overlapStart)) {
            return Duration.between(overlapStart, overlapEnd).getSeconds();
        }
        return 0;
    }
}
