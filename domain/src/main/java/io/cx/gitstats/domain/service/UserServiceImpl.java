package io.cx.gitstats.domain.service;

import io.cx.gitstats.domain.model.User;
import io.cx.gitstats.domain.persistence.UserPersistenceStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserPersistenceStorage userPersistenceStorage;

    @Override
    public Optional<User> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return userPersistenceStorage.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return userPersistenceStorage.findByUsername(username);
    }

    @Override
    public List<User> findActiveContributors() {
        return userPersistenceStorage.findActiveContributors();
    }

    @Override
    public List<User> findAll() {
        return userPersistenceStorage.findAll();
    }

    @Override
    public List<User> searchByUsernameOrName(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return userPersistenceStorage.searchByUsernameOrName(query);
    }
}
