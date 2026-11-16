package io.cx.gitstats.domain.service;

import io.cx.gitstats.domain.model.Project;

import java.util.List;
import java.util.Optional;

public interface ProjectService {
    Project createAndScheduleSync(Long gitlabId);
    Optional<Project> findById(Long id);
    Optional<Project> findByGitlabId(Long gitlabId);
    List<Project> findAll();
    List<Project> searchByName(String query);
    Project update(Project project);
    void deleteById(Long id);
}
