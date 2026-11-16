package io.cx.gitstats.persistence.storage;

import io.cx.gitstats.domain.model.Project;
import io.cx.gitstats.domain.model.ProjectStatus;
import io.cx.gitstats.domain.persistence.ProjectPersistenceStorage;
import io.cx.gitstats.persistence.entity.ProjectEntity;
import io.cx.gitstats.persistence.mapper.ProjectPersistenceMapper;
import io.cx.gitstats.persistence.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProjectPersistencePostgresStorage implements ProjectPersistenceStorage {

    private final ProjectRepository projectRepository;
    private final ProjectPersistenceMapper mapper;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public Project save(Project project) {
        ProjectEntity entity = mapper.toEntity(project);
        ProjectEntity saved = projectRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void saveAll(List<Project> projects) {
        if (projects == null || projects.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO projects (gitlab_id, name, web_url, status, created_at, updated_at)
                VALUES (:gitlabId, :name, :webUrl, :status, :createdAt, :updatedAt)
                ON CONFLICT (gitlab_id) DO NOTHING
                """;

        var batchParams = new ArrayList<SqlParameterSource>();
        for (var project : projects) {
            var now = Instant.now();
            var createdAt = project.getCreatedAt() != null ? project.getCreatedAt() : now;
            var updatedAt = project.getUpdatedAt() != null ? project.getUpdatedAt() : now;
            var params = new MapSqlParameterSource()
                    .addValue("gitlabId", project.getGitlabId())
                    .addValue("name", project.getName())
                    .addValue("webUrl", project.getWebUrl())
                    .addValue("status", project.getStatus() != null ? project.getStatus().name() : ProjectStatus.CREATING.name())
                    .addValue("createdAt", Timestamp.from(createdAt))
                    .addValue("updatedAt", Timestamp.from(updatedAt));
            batchParams.add(params);
        }

        jdbcTemplate.batchUpdate(sql, batchParams.toArray(new SqlParameterSource[0]));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Project> findById(Long id) {
        return projectRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Project> findByGitlabId(Long gitlabId) {
        return projectRepository.findByGitlabId(gitlabId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Project getByGitlabId(Long gitlabId) {
        return findByGitlabId(gitlabId)
                .orElseThrow(() -> new IllegalStateException("Project not found for gitlabId: " + gitlabId));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> findExistingGitlabIds(Collection<Long> gitlabIds) {
        if (gitlabIds == null || gitlabIds.isEmpty()) {
            return Set.of();
        }
        return projectRepository.findExistingGitlabIds(gitlabIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> findAll() {
        return mapper.toDomainList(projectRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> findAllByStatus(ProjectStatus status) {
        return mapper.toDomainList(projectRepository.findAllByStatus(status));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> searchByName(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return mapper.toDomainList(projectRepository.searchByName(query.trim()));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        projectRepository.deleteById(id);
    }
}
