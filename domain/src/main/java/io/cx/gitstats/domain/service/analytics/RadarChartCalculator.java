package io.cx.gitstats.domain.service.analytics;

import io.cx.gitstats.domain.model.analytics.RadarChartStats;
import io.cx.gitstats.domain.model.analytics.RadarMetricItem;
import io.cx.gitstats.domain.model.analytics.UserCommentDailyViewRow;
import io.cx.gitstats.domain.model.analytics.UserMergeRequestDailyViewRow;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

public final class RadarChartCalculator {

    private static final double MIN_ACTIVITY_RATIO = 0.20;

    private RadarChartCalculator() {
    }

    public static RadarChartStats calculate(
            Long currentUserId,
            LocalDate from,
            LocalDate to,
            boolean includeAiReview,
            List<UserMergeRequestDailyViewRow> allMrs,
            List<UserCommentDailyViewRow> allComments
    ) {
        if (currentUserId == null || allMrs == null) {
            return RadarChartStats.builder().build();
        }

        int totalWorkingDays = WorkingDaysCalculator.countWorkingDays(from, to);
        Map<Long, UserMetricAgg> allAggs = calculateAllUsersAggs(from, to, includeAiReview, allMrs, allComments);
        List<UserMetricAgg> eligibleUsers = filterEligibleUsers(allAggs.values(), totalWorkingDays);
        UserMetricAgg currentUser = allAggs.getOrDefault(currentUserId, new UserMetricAgg());

        List<RadarMetricItem> left = buildContributionMetrics(currentUser, eligibleUsers);
        List<RadarMetricItem> right = buildReviewMetrics(currentUser, eligibleUsers);

        List<RadarMetricItem> all = new ArrayList<>();
        all.addAll(left);
        all.addAll(right);

        return RadarChartStats.builder()
                .contributionMetrics(left)
                .reviewMetrics(right)
                .allMetrics(all)
                .build();
    }

    private static List<RadarMetricItem> buildContributionMetrics(
            UserMetricAgg currentUser,
            List<UserMetricAgg> eligible
    ) {
        List<RadarMetricItem> list = new ArrayList<>();
        list.add(createCodeWrittenMetric(currentUser, eligible));
        list.add(createMergedMrsMetric(currentUser, eligible));
        list.add(createReceivedCommentsMetric(currentUser, eligible));
        list.add(createAvgMrSizeMetric(currentUser, eligible));
        return list;
    }

    private static List<RadarMetricItem> buildReviewMetrics(
            UserMetricAgg currentUser,
            List<UserMetricAgg> eligible
    ) {
        List<RadarMetricItem> list = new ArrayList<>();
        list.add(createApprovalsMetric(currentUser, eligible));
        list.add(createReactionTimeMetric(currentUser, eligible));
        list.add(createProducedCommentsMetric(currentUser, eligible));
        list.add(createCommentDensityMetric(currentUser, eligible));
        return list;
    }

    private static RadarMetricItem createCodeWrittenMetric(
            UserMetricAgg user,
            List<UserMetricAgg> eligible
    ) {
        double avg = calcAvg(eligible, u -> u.totalCode);
        int score = calcHigherBetterScore(user.totalCode, eligible, u -> u.totalCode);
        return RadarMetricItem.builder()
                .name("Code written")
                .userValue(user.totalCode)
                .avgValue(avg)
                .scorePercent(score)
                .formattedUserValue(String.format("%,d lines", (long) user.totalCode))
                .formattedAvgValue(String.format("%,d lines", (long) Math.round(avg)))
                .description("Total lines of code written (added + removed) for the period")
                .build();
    }

    private static RadarMetricItem createMergedMrsMetric(
            UserMetricAgg user,
            List<UserMetricAgg> eligible
    ) {
        double avg = calcAvg(eligible, u -> u.mergedMrsCount);
        int score = calcHigherBetterScore(user.mergedMrsCount, eligible, u -> u.mergedMrsCount);
        return RadarMetricItem.builder()
                .name("Merged MRs")
                .userValue(user.mergedMrsCount)
                .avgValue(avg)
                .scorePercent(score)
                .formattedUserValue(String.format("%,d", (long) user.mergedMrsCount))
                .formattedAvgValue(String.format("%.1f", avg))
                .description("Total number of merged Merge Requests for the period")
                .build();
    }

    private static RadarMetricItem createReceivedCommentsMetric(
            UserMetricAgg user,
            List<UserMetricAgg> eligible
    ) {
        double userVal = user.getReceivedCommentsPer1k();
        double avg = calcAvg(eligible, UserMetricAgg::getReceivedCommentsPer1k);
        int score = calcLowerBetterScore(userVal, eligible, UserMetricAgg::getReceivedCommentsPer1k);
        return RadarMetricItem.builder()
                .name("Received comments")
                .userValue(userVal)
                .avgValue(avg)
                .scorePercent(score)
                .formattedUserValue(String.format("%.1f / 1k loc", userVal))
                .formattedAvgValue(String.format("%.1f / 1k loc", avg))
                .description("Received comments per 1,000 lines of own code (lower is better quality)")
                .build();
    }

    private static RadarMetricItem createAvgMrSizeMetric(
            UserMetricAgg user,
            List<UserMetricAgg> eligible
    ) {
        double userSize = user.getAvgMrSize();
        double userDelta = user.getMrSizeDelta();
        double avgSize = calcAvg(eligible, UserMetricAgg::getAvgMrSize);
        int score = calcLowerBetterScore(userDelta, eligible, UserMetricAgg::getMrSizeDelta);
        return RadarMetricItem.builder()
                .name("Avg MR size")
                .userValue(userSize)
                .avgValue(avgSize)
                .scorePercent(score)
                .formattedUserValue(String.format("%.0f lines", userSize))
                .formattedAvgValue(String.format("%.0f lines", avgSize))
                .description("Average MR size (target: 400 lines; deviation reduces score)")
                .build();
    }

    private static RadarMetricItem createApprovalsMetric(
            UserMetricAgg user,
            List<UserMetricAgg> eligible
    ) {
        double avg = calcAvg(eligible, u -> u.approvalsCount);
        int score = calcHigherBetterScore(user.approvalsCount, eligible, u -> u.approvalsCount);
        return RadarMetricItem.builder()
                .name("MR Approvals")
                .userValue(user.approvalsCount)
                .avgValue(avg)
                .scorePercent(score)
                .formattedUserValue(String.format("%,d", (long) user.approvalsCount))
                .formattedAvgValue(String.format("%.1f", avg))
                .description("Total number of Merge Request approvals")
                .build();
    }

    private static RadarMetricItem createReactionTimeMetric(
            UserMetricAgg user,
            List<UserMetricAgg> eligible
    ) {
        Double userHours = user.getAvgReactionTimeHours();
        double displayUserHours = userHours != null ? userHours : 0.0;
        double avgHours = calcAvg(eligible, u -> u.getAvgReactionTimeHours() != null ? u.getAvgReactionTimeHours() : 8.0);
        int score = calcLowerBetterScore(displayUserHours > 0 ? displayUserHours : 8.0,
                eligible, u -> u.getAvgReactionTimeHours() != null ? u.getAvgReactionTimeHours() : 8.0);
        return RadarMetricItem.builder()
                .name("Reaction Time")
                .userValue(displayUserHours)
                .avgValue(avgHours)
                .scorePercent(score)
                .formattedUserValue(formatReactionTime(userHours))
                .formattedAvgValue(formatReactionTime(avgHours))
                .description("Average time to first review response on Merge Requests (lower is better)")
                .build();
    }

    private static RadarMetricItem createProducedCommentsMetric(
            UserMetricAgg user,
            List<UserMetricAgg> eligible
    ) {
        double userVal = user.getProducedCommentsPer1k();
        double avg = calcAvg(eligible, UserMetricAgg::getProducedCommentsPer1k);
        int score = calcHigherBetterScore(userVal, eligible, UserMetricAgg::getProducedCommentsPer1k);
        return RadarMetricItem.builder()
                .name("Produced comments")
                .userValue(userVal)
                .avgValue(avg)
                .scorePercent(score)
                .formattedUserValue(String.format("%.1f / 1k loc", userVal))
                .formattedAvgValue(String.format("%.1f / 1k loc", avg))
                .description("Comments made per 1,000 lines of reviewed code as a reviewer")
                .build();
    }

    private static RadarMetricItem createCommentDensityMetric(
            UserMetricAgg user,
            List<UserMetricAgg> eligible
    ) {
        double userScore = user.calculateDensityRating();
        double avg = calcAvg(eligible, UserMetricAgg::calculateDensityRating);
        int score = calcHigherBetterScore(userScore, eligible, UserMetricAgg::calculateDensityRating);
        return RadarMetricItem.builder()
                .name("Review density by MR size")
                .userValue(userScore)
                .avgValue(avg)
                .scorePercent(score)
                .formattedUserValue(String.format("%.0f%%", userScore))
                .formattedAvgValue(String.format("%.0f%%", avg))
                .description("Comment density on large MRs relative to small ones (less attention drop is better)")
                .build();
    }

    private static int calcHigherBetterScore(
            double userVal,
            List<UserMetricAgg> eligible,
            ToDoubleFunction<UserMetricAgg> extractor
    ) {
        List<Double> values = eligible.stream().map(extractor::applyAsDouble).toList();
        return calculatePercentileScore(userVal, values, true);
    }

    private static int calcLowerBetterScore(
            double userVal,
            List<UserMetricAgg> eligible,
            ToDoubleFunction<UserMetricAgg> extractor
    ) {
        List<Double> values = eligible.stream().map(extractor::applyAsDouble).toList();
        return calculatePercentileScore(userVal, values, false);
    }

    private static int calculatePercentileScore(
            double userValue,
            List<Double> allValues,
            boolean higherIsBetter
    ) {
        if (allValues.isEmpty()) {
            return 0;
        }
        int strictCount = 0;
        int equalCount = 0;
        for (double v : allValues) {
            if (higherIsBetter) {
                if (userValue > v) strictCount++;
                else if (Double.compare(userValue, v) == 0) equalCount++;
            } else {
                if (userValue < v) strictCount++;
                else if (Double.compare(userValue, v) == 0) equalCount++;
            }
        }
        double rank = (strictCount + 0.5 * equalCount) / allValues.size() * 100.0;
        int rounded = (int) Math.round(rank);
        return Math.max(0, Math.min(100, rounded));
    }

    private static double calcAvg(
            List<UserMetricAgg> eligible,
            ToDoubleFunction<UserMetricAgg> extractor
    ) {
        if (eligible.isEmpty()) {
            return 0.0;
        }
        return eligible.stream().mapToDouble(extractor).average().orElse(0.0);
    }

    private static List<UserMetricAgg> filterEligibleUsers(
            Collection<UserMetricAgg> users,
            int totalWorkingDays
    ) {
        if (users == null || users.isEmpty()) {
            return List.of();
        }
        return users.stream()
                .filter(u -> !u.isBot)
                .filter(u -> isUserActiveEnough(u, totalWorkingDays))
                .toList();
    }

    private static boolean isUserActiveEnough(UserMetricAgg user, int totalWorkingDays) {
        if (totalWorkingDays <= 0) {
            return true;
        }
        double ratio = (double) user.activeWorkingDays.size() / totalWorkingDays;
        return ratio >= MIN_ACTIVITY_RATIO;
    }

    private static String formatReactionTime(Double hours) {
        if (hours == null) return "N/A";
        long totalMinutes = Math.round(hours * 60.0);
        long hh = totalMinutes / 60;
        long mm = totalMinutes % 60;
        return String.format("%02d:%02d", hh, mm);
    }

    private static Map<Long, UserMetricAgg> calculateAllUsersAggs(
            LocalDate from,
            LocalDate to,
            boolean includeAiReview,
            List<UserMergeRequestDailyViewRow> allMrs,
            List<UserCommentDailyViewRow> allComments
    ) {
        Map<Long, UserMetricAgg> map = new HashMap<>();

        Map<Long, List<UserMergeRequestDailyViewRow>> authorMrsMap = allMrs.stream()
                .filter(mr -> mr.getAuthorId() != null && isDateBetween(mr.getContributionDate(), from, to))
                .collect(Collectors.groupingBy(UserMergeRequestDailyViewRow::getAuthorId));

        Map<Long, List<UserCommentDailyViewRow>> allCommentsByMr = allComments.stream()
                .filter(c -> c.getMergeRequestId() != null)
                .collect(Collectors.groupingBy(UserCommentDailyViewRow::getMergeRequestId));

        Map<Long, List<UserCommentDailyViewRow>> reviewerCommentsMap = allComments.stream()
                .filter(c -> c.getAuthorId() != null && isDateBetween(c.getContributionDate(), from, to))
                .collect(Collectors.groupingBy(UserCommentDailyViewRow::getAuthorId));

        Set<Long> allUserIds = collectAllUserIds(allMrs, allComments);
        for (Long uid : allUserIds) {
            UserMetricAgg agg = map.computeIfAbsent(uid, id -> new UserMetricAgg());
            populateUserMetrics(agg, uid, from, to, includeAiReview, allMrs, authorMrsMap, allCommentsByMr, reviewerCommentsMap);
        }

        return map;
    }

    private static Set<Long> collectAllUserIds(
            List<UserMergeRequestDailyViewRow> allMrs,
            List<UserCommentDailyViewRow> allComments
    ) {
        Set<Long> ids = new HashSet<>();
        for (var mr : allMrs) {
            if (mr.getAuthorId() != null) ids.add(mr.getAuthorId());
            if (mr.getApproverId() != null) ids.add(mr.getApproverId());
        }
        for (var c : allComments) {
            if (c.getAuthorId() != null) ids.add(c.getAuthorId());
        }
        return ids;
    }

    private static void populateUserMetrics(
            UserMetricAgg agg,
            Long userId,
            LocalDate from,
            LocalDate to,
            boolean includeAiReview,
            List<UserMergeRequestDailyViewRow> allMrs,
            Map<Long, List<UserMergeRequestDailyViewRow>> authorMrsMap,
            Map<Long, List<UserCommentDailyViewRow>> allCommentsByMr,
            Map<Long, List<UserCommentDailyViewRow>> reviewerCommentsMap
    ) {
        agg.userId = userId;
        populateContribution(agg, userId, includeAiReview, authorMrsMap.getOrDefault(userId, List.of()), allCommentsByMr);
        populateReview(agg, userId, from, to, allMrs, allCommentsByMr, reviewerCommentsMap.getOrDefault(userId, List.of()));
        populateWorkingDaysAndBot(agg, userId, from, to, allMrs, reviewerCommentsMap.getOrDefault(userId, List.of()));
    }

    private static void populateContribution(
            UserMetricAgg agg,
            Long userId,
            boolean includeAiReview,
            List<UserMergeRequestDailyViewRow> userAuthorMrs,
            Map<Long, List<UserCommentDailyViewRow>> allCommentsByMr
    ) {
        long added = 0;
        long deleted = 0;
        long receivedComments = 0;

        for (var mr : userAuthorMrs) {
            added += mr.getRowsAdded() != null ? mr.getRowsAdded() : 0;
            deleted += mr.getRowsDeleted() != null ? mr.getRowsDeleted() : 0;
            var mrComments = allCommentsByMr.getOrDefault(mr.getMrId(), List.of());
            for (var c : mrComments) {
                if (!Objects.equals(c.getAuthorId(), userId)) {
                    if (includeAiReview || !Boolean.TRUE.equals(c.getAuthorIsBot())) {
                        receivedComments++;
                    }
                }
            }
        }

        agg.totalCode = added + deleted;
        agg.mergedMrsCount = userAuthorMrs.size();
        agg.commentsReceived = receivedComments;
    }

    private static void populateReview(
            UserMetricAgg agg,
            Long userId,
            LocalDate from,
            LocalDate to,
            List<UserMergeRequestDailyViewRow> allMrs,
            Map<Long, List<UserCommentDailyViewRow>> allCommentsByMr,
            List<UserCommentDailyViewRow> userComments
    ) {
        Set<Long> commentedMrIds = userComments.stream()
                .map(UserCommentDailyViewRow::getMergeRequestId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<UserMergeRequestDailyViewRow> reviewerMrs = allMrs.stream()
                .filter(mr -> (Objects.equals(mr.getApproverId(), userId) || commentedMrIds.contains(mr.getMrId()))
                        && isDateBetween(mr.getContributionDate(), from, to))
                .toList();

        long approvals = reviewerMrs.stream().filter(mr -> Objects.equals(mr.getApproverId(), userId)).count();
        long commentsCount = userComments.size();

        long reviewedCode = reviewerMrs.stream().mapToLong(mr -> (long) (mr.getRowsAdded() != null ? mr.getRowsAdded() : 0) + (mr.getRowsDeleted() != null ? mr.getRowsDeleted() : 0)).sum();

        agg.approvalsCount = approvals;
        agg.commentsProduced = commentsCount;
        agg.reviewedCode = reviewedCode;

        calculateReactionTimeAndDensity(agg, reviewerMrs, userComments);
    }

    private static void calculateReactionTimeAndDensity(
            UserMetricAgg agg,
            List<UserMergeRequestDailyViewRow> reviewerMrs,
            List<UserCommentDailyViewRow> userComments
    ) {
        List<Double> validHours = new ArrayList<>();
        Map<Long, Long> commentsPerMr = userComments.stream()
                .filter(c -> c.getMergeRequestId() != null)
                .collect(Collectors.groupingBy(UserCommentDailyViewRow::getMergeRequestId, Collectors.counting()));

        long[] totalLines = new long[5];
        long[] totalComments = new long[5];

        for (var mr : reviewerMrs) {
            Double hours = getValidReactionHours(mr);
            if (hours != null) {
                validHours.add(hours);
            }
            int size = (mr.getRowsAdded() != null ? mr.getRowsAdded() : 0) + (mr.getRowsDeleted() != null ? mr.getRowsDeleted() : 0);
            int idx = getBucketIndex(size);
            totalLines[idx] += size;
            totalComments[idx] += commentsPerMr.getOrDefault(mr.getMrId(), 0L);
        }

        agg.avgReactionTimeHours = validHours.isEmpty() ? null
                : validHours.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        agg.bucketLines = totalLines;
        agg.bucketComments = totalComments;
    }

    private static Double getValidReactionHours(UserMergeRequestDailyViewRow mr) {
        Instant start = mr.getReadyForReviewAt() != null ? mr.getReadyForReviewAt()
                : (mr.getReviewRequestedAt() != null ? mr.getReviewRequestedAt() : mr.getCreatedAt());
        Instant end = mr.getReviewFinishedAt() != null ? mr.getReviewFinishedAt() : mr.getApprovedAt();
        if (start != null && end != null && end.isAfter(start)) {
            double hours = WorkingDaysCalculator.calculateBusinessWorkingHours(start, end);
            if (hours >= 0.0 && hours <= 8.0) {
                return hours;
            }
        }
        return null;
    }

    private static int getBucketIndex(int size) {
        if (size <= 50) return 0;
        if (size <= 200) return 1;
        if (size <= 600) return 2;
        if (size <= 1000) return 3;
        return 4;
    }

    private static void populateWorkingDaysAndBot(
            UserMetricAgg agg,
            Long userId,
            LocalDate from,
            LocalDate to,
            List<UserMergeRequestDailyViewRow> allMrs,
            List<UserCommentDailyViewRow> userComments
    ) {
        for (var mr : allMrs) {
            if (Objects.equals(mr.getAuthorId(), userId)) {
                agg.isBot = Boolean.TRUE.equals(mr.getAuthorIsBot());
                if (isDateBetween(mr.getContributionDate(), from, to) && mr.getContributionDate().getDayOfWeek().getValue() < 6) {
                    agg.activeWorkingDays.add(mr.getContributionDate());
                }
            }
            if (Objects.equals(mr.getApproverId(), userId)) {
                agg.isBot = Boolean.TRUE.equals(mr.getApproverIsBot());
                if (isDateBetween(mr.getApprovedDate(), from, to) && mr.getApprovedDate().getDayOfWeek().getValue() < 6) {
                    agg.activeWorkingDays.add(mr.getApprovedDate());
                }
            }
        }
        for (var c : userComments) {
            agg.isBot = Boolean.TRUE.equals(c.getAuthorIsBot());
            if (c.getContributionDate().getDayOfWeek().getValue() < 6) {
                agg.activeWorkingDays.add(c.getContributionDate());
            }
        }
    }

    private static boolean isDateBetween(LocalDate date, LocalDate from, LocalDate to) {
        if (date == null) return false;
        if (from != null && date.isBefore(from)) return false;
        return to == null || !date.isAfter(to);
    }

    public static class UserMetricAgg {
        Long userId;
        boolean isBot = false;
        Set<LocalDate> activeWorkingDays = new HashSet<>();

        double totalCode = 0;
        double mergedMrsCount = 0;
        double commentsReceived = 0;

        double approvalsCount = 0;
        double commentsProduced = 0;
        double reviewedCode = 0;
        Double avgReactionTimeHours = null;

        long[] bucketLines = new long[5];
        long[] bucketComments = new long[5];

        public double getReceivedCommentsPer1k() {
            if (totalCode <= 0) return 0.0;
            return (commentsReceived / totalCode) * 1000.0;
        }

        public double getAvgMrSize() {
            if (mergedMrsCount <= 0) return 0.0;
            return totalCode / mergedMrsCount;
        }

        public double getMrSizeDelta() {
            if (mergedMrsCount <= 0) return 400.0;
            return Math.abs(getAvgMrSize() - 400.0);
        }

        public Double getAvgReactionTimeHours() {
            return avgReactionTimeHours;
        }

        public double getProducedCommentsPer1k() {
            if (reviewedCode <= 0) return 0.0;
            return (commentsProduced / reviewedCode) * 1000.0;
        }

        public double calculateDensityRating() {
            double baseRate = bucketLines[0] > 0 ? ((double) bucketComments[0] / bucketLines[0]) * 1000.0 : 0.0;
            double largeRate = (bucketLines[3] + bucketLines[4]) > 0
                    ? ((double) (bucketComments[3] + bucketComments[4]) / (bucketLines[3] + bucketLines[4])) * 1000.0
                    : 0.0;

            if (baseRate <= 0.0) {
                return (commentsProduced > 0) ? 50.0 : 0.0;
            }
            double ratio = largeRate / baseRate;
            double score = Math.min(1.0, ratio) * 100.0;
            return Math.max(0.0, score);
        }
    }
}
