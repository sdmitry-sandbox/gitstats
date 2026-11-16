package io.cx.gitstats.persistence.storage;

import io.cx.gitstats.domain.model.UserGitlabState;
import io.cx.gitstats.persistence.AbstractPersistenceTest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.cx.gitstats.persistence.utils.QueryCountAssertion.assertQueryCountAtMost;
import static io.cx.gitstats.persistence.utils.QueryCountAssertion.assertQueryCountEqual;

@RequiredArgsConstructor
class UserPersistencePostgresStorageQueryCountTest extends AbstractPersistenceTest {
    private final UserPersistencePostgresStorage storage;

    @Test
    void save_whenNewUser_thenSingleDbQuery() {
        // given
        var user = testHelper.buildUser(201L, "user201", "User 201", UserGitlabState.ACTIVE);

        // when & then
        assertQueryCountEqual(1, () -> storage.save(user));
    }

    @Test
    void saveAll_whenMultipleNewUsers_thenSingleDbQuery() {
        // given
        var user1 = testHelper.buildUser(202L, "user202", "User 202", UserGitlabState.ACTIVE);
        var user2 = testHelper.buildUser(203L, "user203", "User 203", UserGitlabState.BLOCKED);

        // when & then
        assertQueryCountEqual(1, () -> storage.saveAll(List.of(user1, user2)));
    }

    @Test
    void findById_whenUserExists_thenSingleDbQuery() {
        // given
        var entity = testHelper.createAndSaveUserEntity(204L, "user204", "User 204", UserGitlabState.ACTIVE);

        // when & then
        assertQueryCountEqual(1, () -> storage.findById(entity.getId()));
    }

    @Test
    void findByGitlabId_whenUserExists_thenSingleDbQuery() {
        // given
        testHelper.createAndSaveUserEntity(205L, "user205", "User 205", UserGitlabState.ACTIVE);

        // when & then
        assertQueryCountEqual(1, () -> storage.findByGitlabId(205L));
    }

    @Test
    void findAllByGitlabIdIn_whenUsersExist_thenSingleDbQuery() {
        // given
        testHelper.createAndSaveUserEntity(206L, "user206", "User 206", UserGitlabState.ACTIVE);
        testHelper.createAndSaveUserEntity(207L, "user207", "User 207", UserGitlabState.ACTIVE);

        // when & then
        assertQueryCountEqual(1, () -> storage.findAllByGitlabIdIn(List.of(206L, 207L)));
    }

    @Test
    void findAll_whenExists_thenSingleDbQuery() {
        // given
        testHelper.createAndSaveUserEntity(208L, "user208", "User 208", UserGitlabState.ACTIVE);

        // when & then
        assertQueryCountEqual(1, () -> storage.findAll());
    }

    @Test
    void deleteById_whenExists_thenAtMostTwoDbQueries() {
        // given
        var entity = testHelper.createAndSaveUserEntity(209L, "user209", "User 209", UserGitlabState.ACTIVE);

        // when & then
        assertQueryCountAtMost(2, () -> storage.deleteById(entity.getId()));
    }
}
