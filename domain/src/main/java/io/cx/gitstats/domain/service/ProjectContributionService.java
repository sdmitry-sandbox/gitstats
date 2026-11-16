package io.cx.gitstats.domain.service;

import io.cx.gitstats.domain.model.DailyContribution;

import java.util.List;

public interface ProjectContributionService {
    List<DailyContribution> getProjectDailyContributions(Long projectId);
}
