package io.cx.gitstats.domain.persistence;

import io.cx.gitstats.domain.model.DailyContribution;

import java.util.List;

public interface UserContributionPersistenceStorage {
    List<DailyContribution> findDailyContributionsByUserId(Long userId);
    List<DailyContribution> findDailyContributionsByUsername(String username);
    int findMaxDailyContribution(boolean includeMr, boolean includeApprovals, boolean includeComments, boolean isBot, Long userId);
    List<Integer> findDailyContributionCounts(boolean includeMr, boolean includeApprovals, boolean includeComments, boolean isBot, Long userId);
}
