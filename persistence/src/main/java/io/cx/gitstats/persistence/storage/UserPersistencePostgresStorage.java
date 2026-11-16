package io.cx.gitstats.persistence.storage;

import io.cx.gitstats.domain.model.User;
import io.cx.gitstats.domain.persistence.UserPersistenceStorage;
import io.cx.gitstats.persistence.entity.UserEntity;
import io.cx.gitstats.persistence.mapper.UserPersistenceMapper;
import io.cx.gitstats.persistence.repository.UserRepository;
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

@Service
@RequiredArgsConstructor
public class UserPersistencePostgresStorage implements UserPersistenceStorage {

    private final UserRepository userRepository;
    private final UserPersistenceMapper mapper;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public User save(User user) {
        UserEntity entity = mapper.toEntity(user);
        UserEntity saved = userRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void saveAll(List<User> users) {
        if (users == null || users.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO users (gitlab_id, username, name, gitlab_state, is_bot, avatar_url, web_url, created_at)
                VALUES (:gitlabId, :username, :name, :gitlabState, :isBot, :avatarUrl, :webUrl, :createdAt)
                ON CONFLICT (gitlab_id) DO NOTHING
                """;

        var batchParams = new ArrayList<SqlParameterSource>();
        for (var user : users) {
            var createdAt = user.getCreatedAt() != null ? user.getCreatedAt() : Instant.now();
            var params = new MapSqlParameterSource()
                    .addValue("gitlabId", user.getGitlabId())
                    .addValue("username", user.getUsername())
                    .addValue("name", user.getName())
                    .addValue("gitlabState", user.getGitlabState().name())
                    .addValue("isBot", user.getIsBot() != null ? user.getIsBot() : false)
                    .addValue("avatarUrl", user.getAvatarUrl())
                    .addValue("webUrl", user.getWebUrl())
                    .addValue("createdAt", Timestamp.from(createdAt));
            batchParams.add(params);
        }

        jdbcTemplate.batchUpdate(sql, batchParams.toArray(new SqlParameterSource[0]));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByGitlabId(Long gitlabId) {
        return userRepository.findByGitlabId(gitlabId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAllByGitlabIdIn(Collection<Long> gitlabIds) {
        if (gitlabIds == null || gitlabIds.isEmpty()) {
            return List.of();
        }
        return mapper.toDomainList(userRepository.findAllByGitlabIdIn(gitlabIds));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return mapper.toDomainList(userRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findActiveContributors() {
        return mapper.toDomainList(userRepository.findActiveContributors());
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> searchByUsernameOrName(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return mapper.toDomainList(userRepository.searchByUsernameOrName(query.trim()));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }
}
