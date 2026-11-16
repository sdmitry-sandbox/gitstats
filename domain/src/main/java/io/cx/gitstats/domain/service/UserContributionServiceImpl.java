package io.cx.gitstats.domain.service;

import io.cx.gitstats.domain.model.ContributionThresholds;
import io.cx.gitstats.domain.model.DailyContribution;
import io.cx.gitstats.domain.persistence.UserContributionPersistenceStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserContributionServiceImpl implements UserContributionService {

    private final UserContributionPersistenceStorage userContributionPersistenceStorage;

    @Override
    public List<DailyContribution> getUserDailyContributions(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return userContributionPersistenceStorage.findDailyContributionsByUserId(userId);
    }

    @Override
    public List<DailyContribution> getUserDailyContributions(String username) {
        if (username == null || username.isBlank()) {
            return List.of();
        }
        return userContributionPersistenceStorage.findDailyContributionsByUsername(username);
    }

    @Override
    public int getMaxDailyContributions(boolean includeMr, boolean includeApprovals, boolean includeComments, boolean isBot, Long userId) {
        return userContributionPersistenceStorage.findMaxDailyContribution(includeMr, includeApprovals, includeComments, isBot, userId);
    }

    @Override
    public ContributionThresholds getContributionThresholds(boolean includeMr, boolean includeApprovals, boolean includeComments, boolean isBot, Long userId) {
        List<Integer> counts = userContributionPersistenceStorage.findDailyContributionCounts(
                includeMr, includeApprovals, includeComments, isBot, userId
        );
        return ContributionThresholds.calculate(counts);
    }
}
