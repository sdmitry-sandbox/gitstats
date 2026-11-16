package io.cx.gitstats.domain.model.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMrHourlyViewRow {
    private Long userId;
    private String username;
    private Boolean isBot;
    private LocalDate contributionDate;
    private Integer dayOfWeek;
    private Integer hourOfDay;
    private Long mrCount;
}
