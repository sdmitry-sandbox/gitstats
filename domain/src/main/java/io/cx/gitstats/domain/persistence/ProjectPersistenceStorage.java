package io.cx.gitstats.domain.persistence;

import io.cx.gitstats.domain.model.Project;
import io.cx.gitstats.domain.model.ProjectStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProjectPersistenceStorage {
    Project save(Project project);
    void saveAll(List<Project> projects);
    Optional<Project> findById(Long id);
    Optional<Project> findByGitlabId(Long gitlabId);
    Project getByGitlabId(Long gitlabId);
    Set<Long> findExistingGitlabIds(Collection<Long> gitlabIds);
    List<Project> findAll();
    List<Project> findAllByStatus(ProjectStatus status);
    List<Project> searchByName(String query);
    void deleteById(Long id);
}
