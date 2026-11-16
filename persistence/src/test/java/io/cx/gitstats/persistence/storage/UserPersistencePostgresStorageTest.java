package io.cx.gitstats.persistence.storage;

import io.cx.gitstats.domain.model.User;
import io.cx.gitstats.domain.model.UserGitlabState;
import io.cx.gitstats.persistence.AbstractPersistenceTest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@RequiredArgsConstructor
class UserPersistencePostgresStorageTest extends AbstractPersistenceTest {
    private final UserPersistencePostgresStorage storage;

    @Test
    void save_whenNewUser_thenSavedAndIdAssigned() {
        // given
        var user = testHelper.buildUser(101L, "user101", "User 101", UserGitlabState.ACTIVE);

        // when
        var saved = storage.save(user);

        // then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getGitlabId()).isEqualTo(101L);
        assertThat(saved.getUsername()).isEqualTo("user101");
        assertThat(saved.getName()).isEqualTo("User 101");
        assertThat(saved.getGitlabState()).isEqualTo(UserGitlabState.ACTIVE);

        var entityInDb = testHelper.findUserById(saved.getId());
        assertThat(entityInDb).isPresent();
        assertThat(entityInDb.get().getGitlabId()).isEqualTo(101L);
        assertThat(entityInDb.get().getUsername()).isEqualTo("user101");
    }

    @Test
    void saveAll_whenNewUsers_thenAllSaved() {
        // given
        var user1 = testHelper.buildUser(102L, "user102", "User 102", UserGitlabState.ACTIVE);
        var user2 = testHelper.buildUser(103L, "user103", "User 103", UserGitlabState.BLOCKED);

        // when
        storage.saveAll(List.of(user1, user2));

        // then
        var allInDb = testHelper.findAllUsers();
        assertThat(allInDb).hasSize(2)
                .extracting(e -> e.getGitlabId())
                .containsExactlyInAnyOrder(102L, 103L);
    }

    @Test
    void saveAll_whenDuplicateUsers_thenDoNothingOnConflict() {
        // given
        var existing = testHelper.createAndSaveUserEntity(104L, "user104", "Original Name", UserGitlabState.ACTIVE);

        var duplicateUser = testHelper.buildUser(104L, "user104", "New Name Should Be Ignored", UserGitlabState.BLOCKED);
        var newUser = testHelper.buildUser(105L, "user105", "User 105", UserGitlabState.ACTIVE);

        // when
        storage.saveAll(List.of(duplicateUser, newUser));

        // then
        var allInDb = testHelper.findAllUsers();
        assertThat(allInDb).hasSize(2);

        var existingInDb = testHelper.findUserById(existing.getId());
        assertThat(existingInDb).isPresent();
        assertThat(existingInDb.get().getName()).isEqualTo("Original Name");
        assertThat(existingInDb.get().getGitlabState()).isEqualTo(UserGitlabState.ACTIVE);
    }

    @Test
    void findById_whenUserExists_thenReturnUser() {
        // given
        var entity = testHelper.createAndSaveUserEntity(106L, "user106", "User 106", UserGitlabState.ACTIVE);

        // when
        var result = storage.findById(entity.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(entity.getId());
        assertThat(result.get().getGitlabId()).isEqualTo(106L);
        assertThat(result.get().getUsername()).isEqualTo("user106");
    }

    @Test
    void findById_whenDoesNotExist_thenReturnEmpty() {
        // when
        var result = storage.findById(999999L);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void findByUsername_whenUserExists_thenReturnUser() {
        // given
        var entity = testHelper.createAndSaveUserEntity(1065L, "user1065", "User 1065", UserGitlabState.ACTIVE);

        // when
        var result = storage.findByUsername("user1065");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(entity.getId());
        assertThat(result.get().getUsername()).isEqualTo("user1065");
    }

    @Test
    void findByUsername_whenDoesNotExist_thenReturnEmpty() {
        // when
        var result = storage.findByUsername("nonexistent");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void findActiveContributors_whenUsersHaveActivity_thenReturnActiveOnly() {
        // given
        var project = testHelper.createAndSaveProjectEntity(901L, "Proj1", io.cx.gitstats.domain.model.ProjectStatus.SYNCED);
        var activeUser1 = testHelper.createAndSaveUserEntity(201L, "alice", "Alice", UserGitlabState.ACTIVE);
        var activeUser2 = testHelper.createAndSaveUserEntity(202L, "bob", "Bob", UserGitlabState.ACTIVE);
        var unmergedUser = testHelper.createAndSaveUserEntity(203L, "charlie", "Charlie", UserGitlabState.ACTIVE);
        var unmergedCommenter = testHelper.createAndSaveUserEntity(204L, "david", "David", UserGitlabState.ACTIVE);

        var mergedMr = testHelper.createAndSaveMergeRequestEntity(501L, project.getId(), "MR 1", io.cx.gitstats.domain.model.MergeRequestGitlabState.MERGED, io.cx.gitstats.domain.model.MergeRequestStatus.SYNCED, activeUser1.getId());
        testHelper.createAndSaveMergeRequestCommentEntity(601L, mergedMr.getId(), activeUser2.getId(), "Great MR");

        var openedMr = testHelper.createAndSaveMergeRequestEntity(502L, project.getId(), "MR 2", io.cx.gitstats.domain.model.MergeRequestGitlabState.OPENED, io.cx.gitstats.domain.model.MergeRequestStatus.SYNCED, unmergedUser.getId());
        testHelper.createAndSaveMergeRequestCommentEntity(602L, openedMr.getId(), unmergedCommenter.getId(), "Comment on opened MR");

        // when
        var activeContributors = storage.findActiveContributors();

        // then
        assertThat(activeContributors)
                .extracting(User::getUsername)
                .containsExactly("alice", "bob");
    }

    @Test
    void findByGitlabId_whenUserExists_thenReturnUser() {
        // given
        var entity = testHelper.createAndSaveUserEntity(107L, "user107", "User 107", UserGitlabState.ACTIVE);

        // when
        var result = storage.findByGitlabId(107L);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(entity.getId());
        assertThat(result.get().getGitlabId()).isEqualTo(107L);
    }

    @Test
    void findAllByGitlabIdIn_whenUsersExist_thenReturnMatchingOnly() {
        // given
        testHelper.createAndSaveUserEntity(108L, "user108", "User 108", UserGitlabState.ACTIVE);
        testHelper.createAndSaveUserEntity(109L, "user109", "User 109", UserGitlabState.ACTIVE);
        testHelper.createAndSaveUserEntity(110L, "user110", "User 110", UserGitlabState.ACTIVE);

        // when
        var result = storage.findAllByGitlabIdIn(List.of(108L, 110L));

        // then
        assertThat(result).hasSize(2)
                .extracting(User::getGitlabId)
                .containsExactlyInAnyOrder(108L, 110L);
    }

    @Test
    void deleteById_whenExists_thenDeleted() {
        // given
        var entity = testHelper.createAndSaveUserEntity(111L, "user111", "User 111", UserGitlabState.ACTIVE);

        // when
        storage.deleteById(entity.getId());

        // then
        assertThat(testHelper.findUserById(entity.getId())).isEmpty();
    }

    @Test
    void deleteById_whenDoesNotExist_thenNoException() {
        // when & then
        assertThatNoException().isThrownBy(() -> storage.deleteById(999999L));
    }

    @Test
    void searchByUsernameOrName_whenMatched_thenReturnMatchingUsers() {
        // given
        testHelper.createAndSaveUserEntity(301L, "dns_ad-t_pdns", "DNS PDNS", UserGitlabState.ACTIVE);
        testHelper.createAndSaveUserEntity(302L, "john_doe", "Admin User", UserGitlabState.ACTIVE);
        testHelper.createAndSaveUserEntity(303L, "other_user", "Other User", UserGitlabState.ACTIVE);

        // when
        var result1 = storage.searchByUsernameOrName("ad-");
        var result2 = storage.searchByUsernameOrName("ADMIN");

        // then
        assertThat(result1).extracting(User::getUsername).containsExactly("dns_ad-t_pdns");
        assertThat(result2).extracting(User::getUsername).containsExactly("john_doe");
    }
}
