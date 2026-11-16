package io.cx.gitstats.domain.service.analytics;

import io.cx.gitstats.domain.model.analytics.*;
import io.cx.gitstats.domain.persistence.UserAnalyticsPersistenceStorage;
import io.cx.gitstats.domain.persistence.UserPersistenceStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAnalyticsServiceImpl implements UserAnalyticsService {

    private final UserAnalyticsPersistenceStorage userAnalyticsPersistenceStorage;
    private final UserPersistenceStorage userPersistenceStorage;

    @Override
    public UserOverviewAnalytics getUserOverviewAnalytics(String username, LocalDate from, LocalDate to) {
        return getUserOverviewAnalytics(username, from, to, false);
    }

    @Override
    public UserOverviewAnalytics getUserOverviewAnalytics(String username, LocalDate from, LocalDate to, boolean includeAiReview) {
        if (username == null || username.isBlank()) {
            return null;
        }
        var userOpt = userPersistenceStorage.findByUsername(username);
        return userOpt.map(user -> getUserOverviewAnalytics(user.getId(), from, to, includeAiReview)).orElse(null);
    }

    @Override
    public UserOverviewAnalytics getUserOverviewAnalytics(Long userId, LocalDate from, LocalDate to) {
        return getUserOverviewAnalytics(userId, from, to, false);
    }

    @Override
    public UserOverviewAnalytics getUserOverviewAnalytics(Long userId, LocalDate from, LocalDate to, boolean includeAiReview) {
        if (userId == null) {
            return null;
        }
        var user = userPersistenceStorage.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }

        LocalDate periodTo = to != null ? to : LocalDate.now(WorkingDaysCalculator.MOSCOW_ZONE);
        LocalDate periodFrom = from != null ? from : periodTo.minusDays(30);

        List<UserMergeRequestDailyViewRow> allMrs = userAnalyticsPersistenceStorage.findAllMergeRequests();
        List<UserCommentDailyViewRow> allComments = userAnalyticsPersistenceStorage.findAllComments();
        List<UserMrHourlyViewRow> userHourly = userAnalyticsPersistenceStorage.findHourlyData(userId);

        return UserOverviewAnalytics.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .periodFrom(periodFrom)
                .periodTo(periodTo)
                .generalStats(buildGeneralStats(userId, periodFrom, periodTo, includeAiReview, allMrs, allComments))
                .codeContributionStats(buildCodeContributionStats(userId, periodFrom, periodTo, includeAiReview, allMrs, allComments))
                .codeReviewStats(buildCodeReviewStats(userId, periodFrom, periodTo, allMrs, allComments))
                .projectStats(buildProjectStats(userId, periodFrom, periodTo, allMrs, allComments))
                .activityStats(buildActivityStats(periodFrom, periodTo, userHourly))
                .build();
    }

    private GeneralStats buildGeneralStats(
            Long userId,
            LocalDate from,
            LocalDate to,
            boolean includeAiReview,
            List<UserMergeRequestDailyViewRow> allMrs,
            List<UserCommentDailyViewRow> allComments
    ) {
        int workingDays = WorkingDaysCalculator.countWorkingDays(from, to);
        List<UserSummaryData> allUsers = calculateAllUsersSummary(from, to, allMrs, allComments);
        GeneralStats stats = PercentileRankCalculator.calculateGeneralStats(userId, allUsers, workingDays);
        RadarChartStats radarStats = RadarChartCalculator.calculate(userId, from, to, includeAiReview, allMrs, allComments);
        stats.setRadarChartStats(radarStats);
        return stats;
    }

    private List<UserSummaryData> calculateAllUsersSummary(
            LocalDate from,
            LocalDate to,
            List<UserMergeRequestDailyViewRow> allMrs,
            List<UserCommentDailyViewRow> allComments
    ) {
        Map<Long, UserSummaryAccumulator> accumulators = new HashMap<>();
        accumulateMrSummaries(from, to, allMrs, accumulators);
        accumulateCommentSummaries(from, to, allComments, accumulators);

        return accumulators.values().stream()
                .map(UserSummaryAccumulator::toUserSummaryData)
                .toList();
    }

    private void accumulateMrSummaries(
            LocalDate from,
            LocalDate to,
            List<UserMergeRequestDailyViewRow> allMrs,
            Map<Long, UserSummaryAccumulator> accumulators
    ) {
        for (var mr : allMrs) {
            if (mr.getAuthorId() != null) {
                var acc = getOrCreateAccumulator(accumulators, mr.getAuthorId(), mr.getAuthorUsername(), mr.getAuthorIsBot());
                if (isDateBetween(mr.getContributionDate(), from, to)) {
                    acc.rowsAdded += mr.getRowsAdded() != null ? mr.getRowsAdded() : 0;
                    acc.rowsDeleted += mr.getRowsDeleted() != null ? mr.getRowsDeleted() : 0;
                    acc.totalCode += mr.getTotalCode() != null ? mr.getTotalCode() : 0;
                    acc.mergedMrsCount++;
                    if (mr.getContributionDate().getDayOfWeek().getValue() < 6) {
                        acc.activeWorkingDays.add(mr.getContributionDate());
                    }
                }
            }
            if (mr.getApproverId() != null) {
                var acc = getOrCreateAccumulator(accumulators, mr.getApproverId(), mr.getApproverUsername(), mr.getApproverIsBot());
                if (isDateBetween(mr.getApprovedDate(), from, to)) {
                    acc.totalApprovals++;
                    if (mr.getApprovedDate().getDayOfWeek().getValue() < 6) {
                        acc.activeWorkingDays.add(mr.getApprovedDate());
                    }
                }
            }
        }
    }

    private void accumulateCommentSummaries(
            LocalDate from,
            LocalDate to,
            List<UserCommentDailyViewRow> allComments,
            Map<Long, UserSummaryAccumulator> accumulators
    ) {
        for (var c : allComments) {
            if (c.getAuthorId() != null) {
                var acc = getOrCreateAccumulator(accumulators, c.getAuthorId(), c.getAuthorUsername(), c.getAuthorIsBot());
                if (isDateBetween(c.getContributionDate(), from, to)) {
                    acc.totalComments++;
                    if (c.getContributionDate().getDayOfWeek().getValue() < 6) {
                        acc.activeWorkingDays.add(c.getContributionDate());
                    }
                }
            }
        }
    }

    private UserSummaryAccumulator getOrCreateAccumulator(
            Map<Long, UserSummaryAccumulator> map,
            Long userId,
            String username,
            Boolean isBot
    ) {
        return map.computeIfAbsent(userId, id -> {
            var acc = new UserSummaryAccumulator();
            acc.userId = id;
            acc.username = username;
            acc.isBot = Boolean.TRUE.equals(isBot);
            return acc;
        });
    }

    private CodeContributionStats buildCodeContributionStats(
            Long userId,
            LocalDate from,
            LocalDate to,
            boolean includeAiReview,
            List<UserMergeRequestDailyViewRow> allMrs,
            List<UserCommentDailyViewRow> allComments
    ) {
        List<UserMergeRequestDailyViewRow> userAuthorMrs = allMrs.stream()
                .filter(mr -> Objects.equals(mr.getAuthorId(), userId) && isDateBetween(mr.getContributionDate(), from, to))
                .sorted(Comparator.comparing(UserMergeRequestDailyViewRow::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        Set<Long> userAuthorMrIds = userAuthorMrs.stream()
                .map(UserMergeRequestDailyViewRow::getMrId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, List<UserCommentDailyViewRow>> receivedCommentsByMr = allComments.stream()
                .filter(c -> c.getMergeRequestId() != null && userAuthorMrIds.contains(c.getMergeRequestId()))
                .filter(c -> !Objects.equals(c.getAuthorId(), userId))
                .filter(c -> includeAiReview || !Boolean.TRUE.equals(c.getAuthorIsBot()))
                .collect(Collectors.groupingBy(UserCommentDailyViewRow::getMergeRequestId));

        var dailyChanges = calculateDailyCodeChanges(userId, from, to, userAuthorMrs);
        long totalAdded = dailyChanges.stream().mapToLong(TimeCodeChangesPoint::getAdded).sum();
        long totalDeleted = dailyChanges.stream().mapToLong(TimeCodeChangesPoint::getDeleted).sum();
        long totalCode = totalAdded + totalDeleted;

        LocalDate firstDate = findUserFirstContributionDate(userId, allMrs, allComments).orElse(from);
        LocalDate adjustedStart = firstDate.isAfter(from) ? firstDate : from;
        int workingDays = WorkingDaysCalculator.countWorkingDays(adjustedStart, to);
        double avgPerDay = workingDays > 0 ? (double) totalCode / workingDays : (double) totalCode;
        double avgPerMr = !userAuthorMrs.isEmpty() ? (double) totalCode / userAuthorMrs.size() : 0.0;

        long commentsReceived = receivedCommentsByMr.values().stream().mapToLong(List::size).sum();
        double commentsReceivedPer1k = totalCode > 0 ? ((double) commentsReceived / totalCode) * 1000.0 : 0.0;

        List<MrDetailsForAnalytics> authorMrDetails = userAuthorMrs.stream()
                .map(mr -> toAuthorMrDetails(mr, receivedCommentsByMr.getOrDefault(mr.getMrId(), List.of())))
                .toList();

        return CodeContributionStats.builder()
                .totalAdded(totalAdded)
                .totalDeleted(totalDeleted)
                .totalCode(totalCode)
                .averageCodePerWorkingDay(avgPerDay)
                .adjustedPeriodStart(adjustedStart)
                .averageCodePerMergeRequest(avgPerMr)
                .commentsReceivedPer1kLines(commentsReceivedPer1k)
                .stackedCodeChanges(dailyChanges)
                .mrQuantityBySize(buildMrQuantityBySize(authorMrDetails))
                .receivedCommentsBySize(buildReceivedCommentsBySize(authorMrDetails))
                .topReviewers(calculateTopReviewers(userAuthorMrs, receivedCommentsByMr))
                .build();
    }

    private MrDetailsForAnalytics toAuthorMrDetails(
            UserMergeRequestDailyViewRow mr,
            List<UserCommentDailyViewRow> receivedComments
    ) {
        int count = receivedComments.size();
        return MrDetailsForAnalytics.builder()
                .id(mr.getMrId())
                .gitlabId(mr.getGitlabId())
                .projectId(mr.getProjectId())
                .projectName(mr.getProjectName())
                .authorId(mr.getAuthorId())
                .authorUsername(mr.getAuthorUsername())
                .authorName(mr.getAuthorName())
                .approverId(mr.getApproverId())
                .rowsAdded(mr.getRowsAdded())
                .rowsDeleted(mr.getRowsDeleted())
                .createdAt(mr.getCreatedAt())
                .mergedAt(mr.getMergedAt())
                .approvedAt(mr.getApprovedAt())
                .readyForReviewAt(mr.getReadyForReviewAt())
                .reviewRequestedAt(mr.getReviewRequestedAt())
                .reviewFinishedAt(mr.getReviewFinishedAt())
                .commentsCount(count)
                .meaningfulCommentsCount(count)
                .build();
    }

    private List<ReviewedAuthorPoint> calculateTopReviewers(
            List<UserMergeRequestDailyViewRow> userAuthorMrs,
            Map<Long, List<UserCommentDailyViewRow>> receivedCommentsByMr
    ) {
        Map<Long, Integer> mrCodeMap = userAuthorMrs.stream()
                .collect(Collectors.toMap(UserMergeRequestDailyViewRow::getMrId, UserMergeRequestDailyViewRow::getTotalCode, (a, b) -> a));

        Map<Long, ReviewerSummary> reviewerMap = new HashMap<>();
        for (var entry : receivedCommentsByMr.entrySet()) {
            Long mrId = entry.getKey();
            for (var c : entry.getValue()) {
                var r = reviewerMap.computeIfAbsent(c.getAuthorId(), id -> new ReviewerSummary(id, c.getAuthorUsername(), c.getAuthorName()));
                r.totalComments++;
                r.reviewedMrIds.add(mrId);
            }
        }
        return reviewerMap.values().stream()
                .map(r -> r.toReviewedAuthorPoint(mrCodeMap))
                .sorted(Comparator.comparing(ReviewedAuthorPoint::getTotalComments, Comparator.reverseOrder()))
                .toList();
    }

    private List<TimeCodeChangesPoint> calculateDailyCodeChanges(
            Long userId,
            LocalDate from,
            LocalDate to,
            List<UserMergeRequestDailyViewRow> userAuthorMrs
    ) {
        Map<LocalDate, long[]> dailyMap = new TreeMap<>();
        for (var mr : userAuthorMrs) {
            if (Objects.equals(mr.getAuthorId(), userId) && isDateBetween(mr.getContributionDate(), from, to)) {
                long[] stats = dailyMap.computeIfAbsent(mr.getContributionDate(), d -> new long[2]);
                stats[0] += mr.getRowsAdded() != null ? mr.getRowsAdded() : 0;
                stats[1] += mr.getRowsDeleted() != null ? mr.getRowsDeleted() : 0;
            }
        }
        List<TimeCodeChangesPoint> points = new ArrayList<>();
        for (var entry : dailyMap.entrySet()) {
            long added = entry.getValue()[0];
            long deleted = entry.getValue()[1];
            points.add(TimeCodeChangesPoint.builder()
                    .date(entry.getKey())
                    .added(added)
                    .deleted(deleted)
                    .total(added + deleted)
                    .build());
        }
        return points;
    }

    private Optional<LocalDate> findUserFirstContributionDate(
            Long userId,
            List<UserMergeRequestDailyViewRow> allMrs,
            List<UserCommentDailyViewRow> allComments
    ) {
        LocalDate minDate = null;
        for (var mr : allMrs) {
            if (Objects.equals(mr.getAuthorId(), userId) && mr.getContributionDate() != null) {
                minDate = minDate == null || mr.getContributionDate().isBefore(minDate) ? mr.getContributionDate() : minDate;
            }
            if (Objects.equals(mr.getApproverId(), userId) && mr.getApprovedDate() != null) {
                minDate = minDate == null || mr.getApprovedDate().isBefore(minDate) ? mr.getApprovedDate() : minDate;
            }
        }
        for (var c : allComments) {
            if (Objects.equals(c.getAuthorId(), userId) && c.getContributionDate() != null) {
                minDate = minDate == null || c.getContributionDate().isBefore(minDate) ? c.getContributionDate() : minDate;
            }
        }
        return Optional.ofNullable(minDate);
    }

    private ActivityStats buildActivityStats(LocalDate from, LocalDate to, List<UserMrHourlyViewRow> userHourly) {
        Map<DayOfWeek, Long> dowMap = new EnumMap<>(DayOfWeek.class);
        for (var d : DayOfWeek.values()) {
            dowMap.put(d, 0L);
        }
        Map<Integer, Long> hourMap = new LinkedHashMap<>();
        for (int h = 0; h < 24; h++) {
            hourMap.put(h, 0L);
        }

        for (var row : userHourly) {
            if (row.getHourOfDay() != null && row.getMrCount() != null) {
                hourMap.merge(row.getHourOfDay(), row.getMrCount(), Long::sum);
            }
            if (isDateBetween(row.getContributionDate(), from, to) && row.getDayOfWeek() != null && row.getMrCount() != null) {
                dowMap.merge(DayOfWeek.of(row.getDayOfWeek()), row.getMrCount(), Long::sum);
            }
        }

        return ActivityStats.builder()
                .heatmapByDayOfWeek(dowMap)
                .heatmapByHourOfDay(hourMap)
                .build();
    }

    private List<MrQuantityBySizePoint> buildMrQuantityBySize(List<MrDetailsForAnalytics> authorMrs) {
        long[] counts = new long[5];
        for (var mr : authorMrs) {
            int size = (mr.getRowsAdded() != null ? mr.getRowsAdded() : 0) + (mr.getRowsDeleted() != null ? mr.getRowsDeleted() : 0);
            int idx = getBucketIndex(size);
            counts[idx]++;
        }
        long totalMrs = authorMrs.size();
        String[] bucketNames = {"0–50", "50–200", "200–600", "600-1000", "1000+"};
        List<MrQuantityBySizePoint> result = new ArrayList<>(5);
        for (int i = 0; i < 5; i++) {
            double pct = totalMrs > 0 ? (counts[i] * 100.0) / totalMrs : 0.0;
            result.add(new MrQuantityBySizePoint(bucketNames[i], counts[i], pct));
        }
        return result;
    }

    private List<MrCommentsBySizePoint> buildReceivedCommentsBySize(List<MrDetailsForAnalytics> authorMrs) {
        long[] totalLines = new long[5];
        long[] totalComments = new long[5];
        for (var mr : authorMrs) {
            int size = (mr.getRowsAdded() != null ? mr.getRowsAdded() : 0) + (mr.getRowsDeleted() != null ? mr.getRowsDeleted() : 0);
            int idx = getBucketIndex(size);
            totalLines[idx] += size;
            totalComments[idx] += mr.getCommentsCount();
        }
        double[] commentsPer1k = new double[5];
        double maxCommentsPer1k = 0.0;
        for (int i = 0; i < 5; i++) {
            commentsPer1k[i] = totalLines[i] > 0 ? ((double) totalComments[i] / totalLines[i]) * 1000.0 : 0.0;
            if (commentsPer1k[i] > maxCommentsPer1k) {
                maxCommentsPer1k = commentsPer1k[i];
            }
        }
        String[] bucketNames = {"0–50", "50–200", "200–600", "600-1000", "1000+"};
        List<MrCommentsBySizePoint> result = new ArrayList<>(5);
        for (int i = 0; i < 5; i++) {
            double ratio = maxCommentsPer1k > 0 ? (commentsPer1k[i] / maxCommentsPer1k) * 100.0 : 0.0;
            result.add(new MrCommentsBySizePoint(bucketNames[i], commentsPer1k[i], ratio));
        }
        return result;
    }

    private static int getBucketIndex(int size) {
        if (size <= 50) return 0;
        if (size <= 200) return 1;
        if (size <= 600) return 2;
        if (size <= 1000) return 3;
        return 4;
    }

    private CodeReviewStats buildCodeReviewStats(
            Long userId,
            LocalDate from,
            LocalDate to,
            List<UserMergeRequestDailyViewRow> allMrs,
            List<UserCommentDailyViewRow> allComments
    ) {
        var comments = findReviewerComments(userId, from, to, allComments);
        var reviewerMrs = findReviewerMrs(userId, from, to, allMrs, comments, allComments);

        long approvals = reviewerMrs.stream().filter(mr -> Objects.equals(mr.getApproverId(), userId)).count();
        long commentsCount = comments.size();

        long reviewedCode = reviewerMrs.stream().mapToLong(mr -> (long) (mr.getRowsAdded() != null ? mr.getRowsAdded() : 0) + (mr.getRowsDeleted() != null ? mr.getRowsDeleted() : 0)).sum();
        double avgCommentsPer1000 = reviewedCode > 0 ? ((double) commentsCount / reviewedCode) * 1000.0 : 0.0;

        Double avgReactionHours = calculateAverageReactionTime(reviewerMrs);
        var reviewActivity = calculateUserDailyReviewActivity(userId, from, to, allMrs, allComments);

        return CodeReviewStats.builder()
                .totalApprovals(approvals)
                .totalComments(commentsCount)
                .averageCommentsPer1000Lines(avgCommentsPer1000)
                .averageReactionTimeHours(avgReactionHours)
                .ttfrByMrSize(buildTtfrByMrSize(reviewerMrs))
                .producedCommentsBySize(buildProducedCommentsBySize(reviewerMrs, comments))
                .reviewedAuthors(buildReviewedAuthors(userId, comments, reviewerMrs))
                .reviewActivityOverTime(reviewActivity)
                .build();
    }

    private List<CommentDetailsForAnalytics> findReviewerComments(
            Long userId,
            LocalDate from,
            LocalDate to,
            List<UserCommentDailyViewRow> allComments
    ) {
        return allComments.stream()
                .filter(c -> Objects.equals(c.getAuthorId(), userId) && isDateBetween(c.getContributionDate(), from, to))
                .sorted(Comparator.comparing(UserCommentDailyViewRow::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(c -> CommentDetailsForAnalytics.builder()
                        .id(c.getCommentId())
                        .gitlabId(c.getGitlabId())
                        .mergeRequestId(c.getMergeRequestId())
                        .mrAuthorId(c.getMrAuthorId())
                        .mrAuthorUsername(c.getMrAuthorUsername())
                        .mrAuthorName(c.getMrAuthorName())
                        .authorId(c.getAuthorId())
                        .createdAt(c.getCreatedAt())
                        .build())
                .toList();
    }

    private List<MrDetailsForAnalytics> findReviewerMrs(
            Long userId,
            LocalDate from,
            LocalDate to,
            List<UserMergeRequestDailyViewRow> allMrs,
            List<CommentDetailsForAnalytics> userComments,
            List<UserCommentDailyViewRow> allComments
    ) {
        Set<Long> commentedMrIds = userComments.stream()
                .map(CommentDetailsForAnalytics::getMergeRequestId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, List<UserCommentDailyViewRow>> commentsByMr = allComments.stream()
                .filter(c -> c.getMergeRequestId() != null)
                .collect(Collectors.groupingBy(UserCommentDailyViewRow::getMergeRequestId));

        return allMrs.stream()
                .filter(mr -> (Objects.equals(mr.getApproverId(), userId) || commentedMrIds.contains(mr.getMrId()))
                        && isDateBetween(mr.getContributionDate(), from, to))
                .sorted(Comparator.comparing(UserMergeRequestDailyViewRow::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(mr -> toReviewerMrDetails(mr, commentsByMr.getOrDefault(mr.getMrId(), List.of())))
                .toList();
    }

    private MrDetailsForAnalytics toReviewerMrDetails(
            UserMergeRequestDailyViewRow mr,
            List<UserCommentDailyViewRow> comments
    ) {
        int totalComments = comments.size();
        int meaningful = 0;
        for (var c : comments) {
            if (!Objects.equals(c.getAuthorId(), mr.getAuthorId())) {
                meaningful++;
            }
        }
        return MrDetailsForAnalytics.builder()
                .id(mr.getMrId())
                .gitlabId(mr.getGitlabId())
                .projectId(mr.getProjectId())
                .projectName(mr.getProjectName())
                .authorId(mr.getAuthorId())
                .authorUsername(mr.getAuthorUsername())
                .authorName(mr.getAuthorName())
                .approverId(mr.getApproverId())
                .rowsAdded(mr.getRowsAdded())
                .rowsDeleted(mr.getRowsDeleted())
                .createdAt(mr.getCreatedAt())
                .mergedAt(mr.getMergedAt())
                .approvedAt(mr.getApprovedAt())
                .readyForReviewAt(mr.getReadyForReviewAt())
                .reviewRequestedAt(mr.getReviewRequestedAt())
                .reviewFinishedAt(mr.getReviewFinishedAt())
                .commentsCount(totalComments)
                .meaningfulCommentsCount(meaningful)
                .build();
    }

    private List<TimeReviewActivityPoint> calculateUserDailyReviewActivity(
            Long userId,
            LocalDate from,
            LocalDate to,
            List<UserMergeRequestDailyViewRow> allMrs,
            List<UserCommentDailyViewRow> allComments
    ) {
        Map<LocalDate, Long> approvalCounts = new HashMap<>();
        for (var mr : allMrs) {
            if (Objects.equals(mr.getApproverId(), userId) && isDateBetween(mr.getApprovedDate(), from, to)) {
                approvalCounts.merge(mr.getApprovedDate(), 1L, Long::sum);
            }
        }
        Map<LocalDate, Long> commentCounts = new HashMap<>();
        for (var c : allComments) {
            if (Objects.equals(c.getAuthorId(), userId) && isDateBetween(c.getContributionDate(), from, to)) {
                commentCounts.merge(c.getContributionDate(), 1L, Long::sum);
            }
        }
        Set<LocalDate> allDates = new TreeSet<>();
        allDates.addAll(approvalCounts.keySet());
        allDates.addAll(commentCounts.keySet());

        List<TimeReviewActivityPoint> points = new ArrayList<>();
        for (var date : allDates) {
            long app = approvalCounts.getOrDefault(date, 0L);
            long comm = commentCounts.getOrDefault(date, 0L);
            points.add(TimeReviewActivityPoint.builder()
                    .date(date)
                    .approvals(app)
                    .comments(comm)
                    .total(app + comm)
                    .build());
        }
        return points;
    }

    private Double calculateAverageReactionTime(List<MrDetailsForAnalytics> mrs) {
        var validHours = new ArrayList<Double>();
        for (var mr : mrs) {
            Double hours = getValidReactionHours(mr);
            if (hours != null) {
                validHours.add(hours);
            }
        }
        if (validHours.isEmpty()) {
            return null;
        }
        return validHours.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private Double getValidReactionHours(MrDetailsForAnalytics mr) {
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

    private List<TtfrBySizePoint> buildTtfrByMrSize(List<MrDetailsForAnalytics> reviewerMrs) {
        Map<Integer, List<Double>> bucketHours = collectBucketReactionHours(reviewerMrs);
        Double[] averages = new Double[5];
        Double bestTime = null;
        for (int i = 0; i < 5; i++) {
            var list = bucketHours.get(i);
            if (!list.isEmpty()) {
                double avg = list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                averages[i] = avg;
                if (bestTime == null || avg < bestTime) {
                    bestTime = avg;
                }
            }
        }
        return createTtfrBySizePoints(averages, bestTime);
    }

    private Map<Integer, List<Double>> collectBucketReactionHours(List<MrDetailsForAnalytics> reviewerMrs) {
        Map<Integer, List<Double>> bucketHours = new HashMap<>();
        for (int i = 0; i < 5; i++) {
            bucketHours.put(i, new ArrayList<>());
        }
        for (var mr : reviewerMrs) {
            int size = (mr.getRowsAdded() != null ? mr.getRowsAdded() : 0) + (mr.getRowsDeleted() != null ? mr.getRowsDeleted() : 0);
            int idx = getBucketIndex(size);
            Double reactionHours = getValidReactionHours(mr);
            if (reactionHours != null) {
                bucketHours.get(idx).add(reactionHours);
            }
        }
        return bucketHours;
    }

    private List<TtfrBySizePoint> createTtfrBySizePoints(Double[] averages, Double bestTime) {
        String[] bucketNames = {"0–50", "50–200", "200–600", "600-1000", "1000+"};
        List<TtfrBySizePoint> result = new ArrayList<>(5);
        for (int i = 0; i < 5; i++) {
            Double avg = averages[i];
            if (avg != null && bestTime != null) {
                double pct = (8.0 - bestTime > 0.0001)
                        ? ((8.0 - avg) / (8.0 - bestTime)) * 100.0
                        : 100.0;
                pct = Math.max(0.0, Math.min(100.0, pct));
                result.add(new TtfrBySizePoint(bucketNames[i], avg, formatHoursToHhMm(avg), pct));
            } else {
                result.add(new TtfrBySizePoint(bucketNames[i], null, "—", 0.0));
            }
        }
        return result;
    }

    private String formatHoursToHhMm(Double hours) {
        if (hours == null) {
            return "N/A";
        }
        long totalMinutes = Math.round(hours * 60.0);
        long hh = totalMinutes / 60;
        long mm = totalMinutes % 60;
        return String.format("%02d:%02d", hh, mm);
    }

    private List<MrCommentsBySizePoint> buildProducedCommentsBySize(
            List<MrDetailsForAnalytics> reviewerMrs,
            List<CommentDetailsForAnalytics> comments
    ) {
        Map<Long, Long> commentsPerMr = comments.stream()
                .filter(c -> c.getMergeRequestId() != null)
                .collect(Collectors.groupingBy(CommentDetailsForAnalytics::getMergeRequestId, Collectors.counting()));

        long[] totalLines = new long[5];
        long[] totalComments = new long[5];
        for (var mr : reviewerMrs) {
            int size = (mr.getRowsAdded() != null ? mr.getRowsAdded() : 0) + (mr.getRowsDeleted() != null ? mr.getRowsDeleted() : 0);
            int idx = getBucketIndex(size);
            totalLines[idx] += size;
            totalComments[idx] += commentsPerMr.getOrDefault(mr.getId(), 0L);
        }

        double[] commentsPer1k = new double[5];
        double maxCommentsPer1k = 0.0;
        for (int i = 0; i < 5; i++) {
            commentsPer1k[i] = totalLines[i] > 0 ? ((double) totalComments[i] / totalLines[i]) * 1000.0 : 0.0;
            if (commentsPer1k[i] > maxCommentsPer1k) {
                maxCommentsPer1k = commentsPer1k[i];
            }
        }

        String[] bucketNames = {"0–50", "50–200", "200–600", "600-1000", "1000+"};
        List<MrCommentsBySizePoint> result = new ArrayList<>(5);
        for (int i = 0; i < 5; i++) {
            double ratio = maxCommentsPer1k > 0 ? (commentsPer1k[i] / maxCommentsPer1k) * 100.0 : 0.0;
            result.add(new MrCommentsBySizePoint(bucketNames[i], commentsPer1k[i], ratio));
        }
        return result;
    }

    private List<ReviewedAuthorPoint> buildReviewedAuthors(
            Long userId,
            List<CommentDetailsForAnalytics> comments,
            List<MrDetailsForAnalytics> reviewerMrs
    ) {
        Map<Long, Integer> mrCodeMap = createMrCodeMap(reviewerMrs);
        Map<Long, Long> authorCommentsCount = new HashMap<>();
        Map<Long, String> usernames = new HashMap<>();
        Map<Long, String> names = new HashMap<>();
        Map<Long, Set<Long>> authorMrIds = new HashMap<>();

        collectAuthorCommentData(userId, comments, authorCommentsCount, usernames, names, authorMrIds);

        var result = new ArrayList<ReviewedAuthorPoint>();
        for (var entry : authorCommentsCount.entrySet()) {
            Long authorId = entry.getKey();
            long totalComments = entry.getValue();
            long totalLines = calculateAuthorLines(authorMrIds.get(authorId), mrCodeMap);
            double commentsPer1k = totalLines > 0 ? ((double) totalComments / totalLines) * 1000.0 : 0.0;

            result.add(ReviewedAuthorPoint.builder()
                    .authorId(authorId)
                    .authorUsername(usernames.get(authorId))
                    .authorName(names.get(authorId))
                    .totalComments(totalComments)
                    .commentsPer1kLines(commentsPer1k)
                    .build());
        }
        result.sort(Comparator.comparing(ReviewedAuthorPoint::getTotalComments, Comparator.reverseOrder()));
        return result;
    }

    private Map<Long, Integer> createMrCodeMap(List<MrDetailsForAnalytics> reviewerMrs) {
        Map<Long, Integer> mrCodeMap = new HashMap<>();
        if (reviewerMrs != null) {
            for (var mr : reviewerMrs) {
                int code = (mr.getRowsAdded() != null ? mr.getRowsAdded() : 0)
                        + (mr.getRowsDeleted() != null ? mr.getRowsDeleted() : 0);
                mrCodeMap.put(mr.getId(), code);
            }
        }
        return mrCodeMap;
    }

    private void collectAuthorCommentData(
            Long userId,
            List<CommentDetailsForAnalytics> comments,
            Map<Long, Long> authorCommentsCount,
            Map<Long, String> usernames,
            Map<Long, String> names,
            Map<Long, Set<Long>> authorMrIds
    ) {
        if (comments == null) return;
        for (var c : comments) {
            if (c.getMrAuthorId() == null || Objects.equals(c.getMrAuthorId(), userId)) continue;
            authorCommentsCount.merge(c.getMrAuthorId(), 1L, Long::sum);
            usernames.putIfAbsent(c.getMrAuthorId(), c.getMrAuthorUsername());
            names.putIfAbsent(c.getMrAuthorId(), c.getMrAuthorName());
            if (c.getMergeRequestId() != null) {
                authorMrIds.computeIfAbsent(c.getMrAuthorId(), k -> new HashSet<>()).add(c.getMergeRequestId());
            }
        }
    }

    private long calculateAuthorLines(Set<Long> mrIds, Map<Long, Integer> mrCodeMap) {
        if (mrIds == null || mrIds.isEmpty()) return 0L;
        long totalLines = 0L;
        for (Long mrId : mrIds) {
            totalLines += mrCodeMap.getOrDefault(mrId, 0);
        }
        return totalLines;
    }

    private ProjectStats buildProjectStats(
            Long userId,
            LocalDate from,
            LocalDate to,
            List<UserMergeRequestDailyViewRow> allMrs,
            List<UserCommentDailyViewRow> allComments
    ) {
        var codeProjects = calculateProjectCodeStats(userId, from, to, allMrs);
        var approvedProjects = calculateProjectApprovalStats(userId, from, to, allMrs);
        var commentedProjects = calculateProjectCommentStats(userId, from, to, allComments);

        return ProjectStats.builder()
                .codeProjects(codeProjects)
                .approvedProjects(approvedProjects)
                .commentedProjects(commentedProjects)
                .build();
    }

    private List<ProjectCodeStats> calculateProjectCodeStats(
            Long userId,
            LocalDate from,
            LocalDate to,
            List<UserMergeRequestDailyViewRow> allMrs
    ) {
        Map<Long, ProjectCodeStatsAccumulator> map = new HashMap<>();
        for (var mr : allMrs) {
            if (Objects.equals(mr.getAuthorId(), userId) && isDateBetween(mr.getContributionDate(), from, to) && mr.getProjectId() != null) {
                var acc = map.computeIfAbsent(mr.getProjectId(), id -> new ProjectCodeStatsAccumulator(id, mr.getProjectName()));
                acc.rowsAdded += mr.getRowsAdded() != null ? mr.getRowsAdded() : 0;
                acc.rowsDeleted += mr.getRowsDeleted() != null ? mr.getRowsDeleted() : 0;
                acc.totalCode += mr.getTotalCode() != null ? mr.getTotalCode() : 0;
            }
        }
        long sumCode = map.values().stream().mapToLong(a -> a.totalCode).sum();
        return map.values().stream()
                .map(a -> a.toProjectCodeStats(sumCode))
                .sorted(Comparator.comparing(ProjectCodeStats::getTotalCode, Comparator.reverseOrder()))
                .toList();
    }

    private List<ProjectCountStats> calculateProjectApprovalStats(
            Long userId,
            LocalDate from,
            LocalDate to,
            List<UserMergeRequestDailyViewRow> allMrs
    ) {
        Map<Long, ProjectCountAccumulator> map = new HashMap<>();
        for (var mr : allMrs) {
            if (Objects.equals(mr.getApproverId(), userId) && isDateBetween(mr.getApprovedDate(), from, to) && mr.getProjectId() != null) {
                var acc = map.computeIfAbsent(mr.getProjectId(), id -> new ProjectCountAccumulator(id, mr.getProjectName()));
                acc.count++;
            }
        }
        long sumCount = map.values().stream().mapToLong(a -> a.count).sum();
        return map.values().stream()
                .map(a -> a.toProjectCountStats(sumCount))
                .sorted(Comparator.comparing(ProjectCountStats::getCount, Comparator.reverseOrder()))
                .toList();
    }

    private List<ProjectCountStats> calculateProjectCommentStats(
            Long userId,
            LocalDate from,
            LocalDate to,
            List<UserCommentDailyViewRow> allComments
    ) {
        Map<Long, ProjectCountAccumulator> map = new HashMap<>();
        for (var c : allComments) {
            if (Objects.equals(c.getAuthorId(), userId) && isDateBetween(c.getContributionDate(), from, to) && c.getProjectId() != null) {
                var acc = map.computeIfAbsent(c.getProjectId(), id -> new ProjectCountAccumulator(id, c.getProjectName()));
                acc.count++;
            }
        }
        long sumCount = map.values().stream().mapToLong(a -> a.count).sum();
        return map.values().stream()
                .map(a -> a.toProjectCountStats(sumCount))
                .sorted(Comparator.comparing(ProjectCountStats::getCount, Comparator.reverseOrder()))
                .toList();
    }

    private boolean isDateBetween(LocalDate date, LocalDate from, LocalDate to) {
        if (date == null) return false;
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }

    private static class UserSummaryAccumulator {
        Long userId;
        String username;
        Boolean isBot = false;
        long rowsAdded = 0;
        long rowsDeleted = 0;
        long totalCode = 0;
        long mergedMrsCount = 0;
        long totalApprovals = 0;
        long totalComments = 0;
        Set<LocalDate> activeWorkingDays = new HashSet<>();

        UserSummaryData toUserSummaryData() {
            return UserSummaryData.builder()
                    .userId(userId)
                    .username(username)
                    .isBot(isBot)
                    .rowsAdded(rowsAdded)
                    .rowsDeleted(rowsDeleted)
                    .totalCode(totalCode)
                    .mergedMrsCount(mergedMrsCount)
                    .totalApprovals(totalApprovals)
                    .totalComments(totalComments)
                    .activeWorkingDaysCount(activeWorkingDays.size())
                    .build();
        }
    }

    private static class ReviewerSummary {
        final Long authorId;
        final String authorUsername;
        final String authorName;
        long totalComments = 0;
        final Set<Long> reviewedMrIds = new HashSet<>();

        ReviewerSummary(Long authorId, String authorUsername, String authorName) {
            this.authorId = authorId;
            this.authorUsername = authorUsername;
            this.authorName = authorName;
        }

        ReviewedAuthorPoint toReviewedAuthorPoint(Map<Long, Integer> mrCodeMap) {
            long totalLines = 0;
            for (Long mrId : reviewedMrIds) {
                totalLines += mrCodeMap.getOrDefault(mrId, 0);
            }
            double commentsPer1k = totalLines > 0 && totalLines <= 1000 ? (double) totalComments :
                    totalLines > 1000 ? ((double) totalComments / totalLines) * 1000.0 : 0.0;
            return ReviewedAuthorPoint.builder()
                    .authorId(authorId)
                    .authorUsername(authorUsername)
                    .authorName(authorName)
                    .totalComments(totalComments)
                    .commentsPer1kLines(commentsPer1k)
                    .build();
        }
    }

    private static class ProjectCodeStatsAccumulator {
        final Long projectId;
        final String projectName;
        long rowsAdded = 0;
        long rowsDeleted = 0;
        long totalCode = 0;

        ProjectCodeStatsAccumulator(Long projectId, String projectName) {
            this.projectId = projectId;
            this.projectName = projectName;
        }

        ProjectCodeStats toProjectCodeStats(long totalSum) {
            double pct = totalSum > 0 ? ((double) totalCode / totalSum) * 100.0 : 0.0;
            return ProjectCodeStats.builder()
                    .projectId(projectId)
                    .projectName(projectName)
                    .rowsAdded(rowsAdded)
                    .rowsDeleted(rowsDeleted)
                    .totalCode(totalCode)
                    .percentageOfTotal(pct)
                    .build();
        }
    }

    private static class ProjectCountAccumulator {
        final Long projectId;
        final String projectName;
        long count = 0;

        ProjectCountAccumulator(Long projectId, String projectName) {
            this.projectId = projectId;
            this.projectName = projectName;
        }

        ProjectCountStats toProjectCountStats(long totalSum) {
            double pct = totalSum > 0 ? ((double) count / totalSum) * 100.0 : 0.0;
            return ProjectCountStats.builder()
                    .projectId(projectId)
                    .projectName(projectName)
                    .count(count)
                    .percentageOfTotal(pct)
                    .build();
        }
    }
}
