package io.cx.gitstats.domain.service;

import io.cx.gitstats.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    List<User> findActiveContributors();
    List<User> findAll();
    List<User> searchByUsernameOrName(String query);
}
