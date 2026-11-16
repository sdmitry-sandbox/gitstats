package io.cx.gitstats.domain.service;

import io.cx.gitstats.domain.model.ContributionThresholds;
import io.cx.gitstats.domain.model.DailyContribution;

import java.util.List;

public interface UserContributionService {
    List<DailyContribution> getUserDailyContributions(Long userId);
    List<DailyContribution> getUserDailyContributions(String username);
    int getMaxDailyContributions(boolean includeMr, boolean includeApprovals, boolean includeComments, boolean isBot, Long userId);
    ContributionThresholds getContributionThresholds(boolean includeMr, boolean includeApprovals, boolean includeComments, boolean isBot, Long userId);
}
