package io.cx.gitstats.persistence.repository;

import io.cx.gitstats.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByGitlabId(Long gitlabId);
    Optional<UserEntity> findByUsername(String username);
    List<UserEntity> findAllByGitlabIdIn(Collection<Long> gitlabIds);

    @Query("""
            SELECT u FROM UserEntity u
            WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
               OR (u.name IS NOT NULL AND LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY u.username ASC
            """)
    List<UserEntity> searchByUsernameOrName(@Param("query") String query);

    @Query(value = """
            SELECT DISTINCT u.* FROM users u
            WHERE EXISTS (
                SELECT 1 FROM merge_requests mr
                WHERE (mr.author_id = u.id OR mr.approver_id = u.id)
                  AND mr.gitlab_state = 'MERGED'
            )
            OR EXISTS (
                SELECT 1 FROM merge_request_comments c
                JOIN merge_requests mr ON c.merge_request_id = mr.id
                WHERE c.author_id = u.id
                  AND mr.gitlab_state = 'MERGED'
            )
            ORDER BY u.username ASC
            """, nativeQuery = true)
    List<UserEntity> findActiveContributors();
}
