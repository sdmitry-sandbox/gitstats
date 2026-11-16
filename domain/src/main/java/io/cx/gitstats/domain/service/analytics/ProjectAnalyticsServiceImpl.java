package io.cx.gitstats.domain.service.analytics;

import io.cx.gitstats.domain.model.analytics.ProjectOverviewAnalytics;
import io.cx.gitstats.domain.model.analytics.ProjectUserCodeStats;
import io.cx.gitstats.domain.model.analytics.ProjectUserCountStats;
import io.cx.gitstats.domain.model.analytics.ProjectUserStats;
import io.cx.gitstats.domain.model.analytics.UserCommentDailyViewRow;
import io.cx.gitstats.domain.model.analytics.UserMergeRequestDailyViewRow;
import io.cx.gitstats.domain.persistence.UserAnalyticsPersistenceStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectAnalyticsServiceImpl implements ProjectAnalyticsService {

    private final UserAnalyticsPersistenceStorage userAnalyticsPersistenceStorage;

    @Override
    public ProjectOverviewAnalytics getProjectOverviewAnalytics(Long projectId, LocalDate from, LocalDate to) {
        if (projectId == null) {
            return null;
        }
        var allMrs = userAnalyticsPersistenceStorage.findAllMergeRequests();
        var allComments = userAnalyticsPersistenceStorage.findAllComments();

        var codeUsers = calculateUserCodeStats(projectId, from, to, allMrs);
        var approvedUsers = calculateUserApprovalStats(projectId, from, to, allMrs);
        var commentedUsers = calculateUserCommentStats(projectId, from, to, allComments);

        var userStats = ProjectUserStats.builder()
                .codeUsers(codeUsers)
                .approvedUsers(approvedUsers)
                .commentedUsers(commentedUsers)
                .build();

        return ProjectOverviewAnalytics.builder()
                .userStats(userStats)
                .build();
    }

    private List<ProjectUserCodeStats> calculateUserCodeStats(
            Long projectId,
            LocalDate from,
            LocalDate to,
            List<UserMergeRequestDailyViewRow> allMrs
    ) {
        Map<Long, UserCodeStatsAccumulator> map = new HashMap<>();
        for (var mr : allMrs) {
            if (isEligibleMrAuthor(mr, projectId, from, to)) {
                var acc = map.computeIfAbsent(
                        mr.getAuthorId(),
                        id -> new UserCodeStatsAccumulator(id, mr.getAuthorUsername(), mr.getAuthorName())
                );
                acc.rowsAdded += mr.getRowsAdded() != null ? mr.getRowsAdded() : 0;
                acc.rowsDeleted += mr.getRowsDeleted() != null ? mr.getRowsDeleted() : 0;
                acc.totalCode += mr.getTotalCode() != null ? mr.getTotalCode() : 0;
            }
        }
        long sumCode = map.values().stream().mapToLong(a -> a.totalCode).sum();
        return map.values().stream()
                .map(a -> a.toProjectUserCodeStats(sumCode))
                .sorted(Comparator.comparing(ProjectUserCodeStats::getTotalCode, Comparator.reverseOrder()))
                .toList();
    }

    private boolean isEligibleMrAuthor(UserMergeRequestDailyViewRow mr, Long projectId, LocalDate from, LocalDate to) {
        return Objects.equals(mr.getProjectId(), projectId)
                && isDateBetween(mr.getContributionDate(), from, to)
                && mr.getAuthorId() != null
                && !Boolean.TRUE.equals(mr.getAuthorIsBot());
    }

    private List<ProjectUserCountStats> calculateUserApprovalStats(
            Long projectId,
            LocalDate from,
            LocalDate to,
            List<UserMergeRequestDailyViewRow> allMrs
    ) {
        Map<Long, UserCountAccumulator> map = new HashMap<>();
        for (var mr : allMrs) {
            if (isEligibleMrApprover(mr, projectId, from, to)) {
                var acc = map.computeIfAbsent(
                        mr.getApproverId(),
                        id -> new UserCountAccumulator(id, mr.getApproverUsername(), mr.getApproverName())
                );
                acc.count++;
            }
        }
        long sumCount = map.values().stream().mapToLong(a -> a.count).sum();
        return map.values().stream()
                .map(a -> a.toProjectUserCountStats(sumCount))
                .sorted(Comparator.comparing(ProjectUserCountStats::getCount, Comparator.reverseOrder()))
                .toList();
    }

    private boolean isEligibleMrApprover(UserMergeRequestDailyViewRow mr, Long projectId, LocalDate from, LocalDate to) {
        return Objects.equals(mr.getProjectId(), projectId)
                && isDateBetween(mr.getApprovedDate(), from, to)
                && mr.getApproverId() != null
                && !Boolean.TRUE.equals(mr.getApproverIsBot());
    }

    private List<ProjectUserCountStats> calculateUserCommentStats(
            Long projectId,
            LocalDate from,
            LocalDate to,
            List<UserCommentDailyViewRow> allComments
    ) {
        Map<Long, UserCountAccumulator> map = new HashMap<>();
        for (var c : allComments) {
            if (isEligibleCommentAuthor(c, projectId, from, to)) {
                var acc = map.computeIfAbsent(
                        c.getAuthorId(),
                        id -> new UserCountAccumulator(id, c.getAuthorUsername(), c.getAuthorName())
                );
                acc.count++;
            }
        }
        long sumCount = map.values().stream().mapToLong(a -> a.count).sum();
        return map.values().stream()
                .map(a -> a.toProjectUserCountStats(sumCount))
                .sorted(Comparator.comparing(ProjectUserCountStats::getCount, Comparator.reverseOrder()))
                .toList();
    }

    private boolean isEligibleCommentAuthor(UserCommentDailyViewRow c, Long projectId, LocalDate from, LocalDate to) {
        return Objects.equals(c.getProjectId(), projectId)
                && isDateBetween(c.getContributionDate(), from, to)
                && c.getAuthorId() != null
                && !Boolean.TRUE.equals(c.getAuthorIsBot());
    }

    private boolean isDateBetween(LocalDate date, LocalDate from, LocalDate to) {
        if (date == null) {
            return false;
        }
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }

    private static class UserCodeStatsAccumulator {
        final Long userId;
        final String username;
        final String name;
        long rowsAdded = 0;
        long rowsDeleted = 0;
        long totalCode = 0;

        UserCodeStatsAccumulator(Long userId, String username, String name) {
            this.userId = userId;
            this.username = username;
            this.name = name;
        }

        ProjectUserCodeStats toProjectUserCodeStats(long totalSum) {
            double pct = totalSum > 0 ? ((double) totalCode / totalSum) * 100.0 : 0.0;
            return ProjectUserCodeStats.builder()
                    .userId(userId)
                    .username(username)
                    .name(name)
                    .rowsAdded(rowsAdded)
                    .rowsDeleted(rowsDeleted)
                    .totalCode(totalCode)
                    .percentageOfTotal(pct)
                    .build();
        }
    }

    private static class UserCountAccumulator {
        final Long userId;
        final String username;
        final String name;
        long count = 0;

        UserCountAccumulator(Long userId, String username, String name) {
            this.userId = userId;
            this.username = username;
            this.name = name;
        }

        ProjectUserCountStats toProjectUserCountStats(long totalSum) {
            double pct = totalSum > 0 ? ((double) count / totalSum) * 100.0 : 0.0;
            return ProjectUserCountStats.builder()
                    .userId(userId)
                    .username(username)
                    .name(name)
                    .count(count)
                    .percentageOfTotal(pct)
                    .build();
        }
    }
}
