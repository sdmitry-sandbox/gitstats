package io.cx.gitstats.domain.service;

import io.cx.gitstats.domain.model.DailyContribution;
import io.cx.gitstats.domain.persistence.ProjectContributionPersistenceStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectContributionServiceImpl implements ProjectContributionService {

    private final ProjectContributionPersistenceStorage projectContributionPersistenceStorage;

    @Override
    public List<DailyContribution> getProjectDailyContributions(Long projectId) {
        if (projectId == null) {
            return List.of();
        }
        return projectContributionPersistenceStorage.findDailyContributionsByProjectId(projectId);
    }
}
