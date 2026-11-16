package io.cx.gitstats.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyContribution {
    private Long userId;
    private String username;
    private Boolean isBot;
    private LocalDate date;
    private int mrCount;
    private int approvalCount;
    private int commentCount;

    public int getCount(boolean includeMr, boolean includeApprovals, boolean includeComments) {
        int total = 0;
        if (includeMr) {
            total += mrCount;
        }
        if (includeApprovals) {
            total += approvalCount;
        }
        if (includeComments) {
            total += commentCount;
        }
        return total;
    }
}
