package io.cx.gitstats.domain.persistence;

import io.cx.gitstats.domain.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserPersistenceStorage {
    User save(User user);
    void saveAll(List<User> users);
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    Optional<User> findByGitlabId(Long gitlabId);
    List<User> findAllByGitlabIdIn(Collection<Long> gitlabIds);
    List<User> findAll();
    List<User> findActiveContributors();
    List<User> searchByUsernameOrName(String query);
    void deleteById(Long id);
}
