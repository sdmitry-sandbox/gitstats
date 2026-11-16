package io.cx.gitstats.persistence.repository;

import io.cx.gitstats.domain.model.ProjectStatus;
import io.cx.gitstats.persistence.entity.ProjectEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {

    @EntityGraph(attributePaths = {"mergeRequests"})
    Optional<ProjectEntity> findByGitlabId(Long gitlabId);

    @Query("SELECT p.gitlabId FROM ProjectEntity p WHERE p.gitlabId IN :gitlabIds")
    Set<Long> findExistingGitlabIds(@Param("gitlabIds") Collection<Long> gitlabIds);

    @Override
    @EntityGraph(attributePaths = {"mergeRequests"})
    @Query("SELECT DISTINCT p FROM ProjectEntity p")
    List<ProjectEntity> findAll();

    @EntityGraph(attributePaths = {"mergeRequests"})
    List<ProjectEntity> findAllByStatus(ProjectStatus status);

    @EntityGraph(attributePaths = {"mergeRequests"})
    @Query("""
            SELECT DISTINCT p FROM ProjectEntity p
            WHERE p.name IS NOT NULL
              AND LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY p.name ASC
            """)
    List<ProjectEntity> searchByName(@Param("query") String query);
}
