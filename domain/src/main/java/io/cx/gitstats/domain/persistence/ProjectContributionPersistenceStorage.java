package io.cx.gitstats.domain.persistence;

import io.cx.gitstats.domain.model.DailyContribution;

import java.util.List;

public interface ProjectContributionPersistenceStorage {
    List<DailyContribution> findDailyContributionsByProjectId(Long projectId);
}
