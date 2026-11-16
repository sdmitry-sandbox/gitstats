package io.cx.gitstats.domain.model.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityStats {
    private Map<DayOfWeek, Long> heatmapByDayOfWeek;
    private Map<Integer, Long> heatmapByHourOfDay;
}
