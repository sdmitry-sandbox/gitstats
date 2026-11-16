package io.cx.gitstats.domain.service.analytics;

import io.cx.gitstats.domain.model.analytics.GeneralStats;
import io.cx.gitstats.domain.model.analytics.PercentileRankBadge;
import io.cx.gitstats.domain.model.analytics.UserSummaryData;

import java.util.List;
import java.util.Objects;
import java.util.function.ToLongFunction;

public final class PercentileRankCalculator {

    private static final double MIN_ACTIVITY_RATIO = 0.20;

    private PercentileRankCalculator() {}

    public static GeneralStats calculateGeneralStats(
            Long targetUserId,
            List<UserSummaryData> allUsers,
            int totalWorkingDaysInPeriod
    ) {
        var eligibleUsers = filterEligibleUsers(allUsers, totalWorkingDaysInPeriod);
        var targetUser = findTargetUser(targetUserId, allUsers);

        var codeRank = calculateMetricRank(
                targetUser, eligibleUsers, UserSummaryData::getTotalCode,
                targetUser != null ? targetUser.getRowsAdded() : 0L,
                targetUser != null ? targetUser.getRowsDeleted() : 0L
        );
        var mrRank = calculateMetricRank(
                targetUser, eligibleUsers, UserSummaryData::getMergedMrsCount, null, null
        );
        var reviewRank = calculateMetricRank(
                targetUser, eligibleUsers, u -> u.getTotalApprovals() + u.getTotalComments(), null, null
        );

        return GeneralStats.builder()
                .codeContributionRank(codeRank)
                .mergedMrsRank(mrRank)
                .reviewActivityRank(reviewRank)
                .build();
    }

    private static List<UserSummaryData> filterEligibleUsers(List<UserSummaryData> users, int totalWorkingDays) {
        if (users == null || users.isEmpty()) {
            return List.of();
        }
        return users.stream()
                .filter(u -> !Boolean.TRUE.equals(u.getIsBot()))
                .filter(u -> isUserActiveEnough(u, totalWorkingDays))
                .toList();
    }

    private static boolean isUserActiveEnough(UserSummaryData user, int totalWorkingDays) {
        if (totalWorkingDays <= 0) {
            return true;
        }
        double ratio = (double) user.getActiveWorkingDaysCount() / totalWorkingDays;
        return ratio >= MIN_ACTIVITY_RATIO;
    }

    private static UserSummaryData findTargetUser(Long targetUserId, List<UserSummaryData> users) {
        if (targetUserId == null || users == null) {
            return null;
        }
        return users.stream()
                .filter(u -> Objects.equals(u.getUserId(), targetUserId))
                .findFirst()
                .orElse(null);
    }

    private static PercentileRankBadge calculateMetricRank(
            UserSummaryData targetUser,
            List<UserSummaryData> eligibleUsers,
            ToLongFunction<UserSummaryData> valueExtractor,
            Long rowsAdded,
            Long rowsDeleted
    ) {
        long targetValue = targetUser != null ? valueExtractor.applyAsLong(targetUser) : 0L;
        if (eligibleUsers.isEmpty()) {
            return buildBadge(0, 0.0, targetValue, 0.0, 0.0, rowsAdded, rowsDeleted);
        }

        double sum = 0;
        long worseCount = 0;
        int totalColleagues = 0;

        for (var user : eligibleUsers) {
            long val = valueExtractor.applyAsLong(user);
            sum += val;
            if (!Objects.equals(user.getUserId(), targetUser != null ? targetUser.getUserId() : null)) {
                totalColleagues++;
                if (val < targetValue) {
                    worseCount++;
                }
            }
        }

        double avg = sum / eligibleUsers.size();
        double compPercent = avg > 0 ? ((targetValue - avg) / avg) * 100.0 : 0.0;
        double rawPercentile = totalColleagues > 0 ? (worseCount / (double) totalColleagues) * 100.0 : 100.0;
        int badgePercentile = Math.min(100, Math.max(0, (int) Math.round(rawPercentile)));

        return buildBadge(badgePercentile, rawPercentile, targetValue, avg, compPercent, rowsAdded, rowsDeleted);
    }

    private static PercentileRankBadge buildBadge(
            int percentile,
            double rawPercentile,
            long userValue,
            double average,
            double compPercent,
            Long rowsAdded,
            Long rowsDeleted
    ) {
        String details = rowsAdded != null && rowsDeleted != null
                ? "+" + rowsAdded + " / -" + rowsDeleted
                : String.valueOf(userValue);

        return PercentileRankBadge.builder()
                .percentile(percentile)
                .rawPercentile(rawPercentile)
                .userValue(userValue)
                .averageValue(average)
                .comparisonWithAveragePercent(compPercent)
                .detailsText(details)
                .rowsAdded(rowsAdded)
                .rowsDeleted(rowsDeleted)
                .build();
    }
}
