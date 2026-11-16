package io.cx.gitstats.persistence.storage;

import io.cx.gitstats.domain.model.MergeRequest;
import io.cx.gitstats.domain.model.MergeRequestGitlabState;
import io.cx.gitstats.domain.model.MergeRequestStatus;
import io.cx.gitstats.domain.model.ProjectStatus;
import io.cx.gitstats.domain.model.UserGitlabState;
import io.cx.gitstats.persistence.AbstractPersistenceTest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@RequiredArgsConstructor
class MergeRequestPersistencePostgresStorageTest extends AbstractPersistenceTest {
    private final MergeRequestPersistencePostgresStorage storage;

    @Test
    void save_whenNewMergeRequest_thenSavedAndIdAssigned() {
        // given
        var project = testHelper.createAndSaveProjectEntity(301L, "MR Project 1", ProjectStatus.SYNCED);
        var mr = testHelper.buildMergeRequest(30101L, project.getId(), "MR Alpha", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCING);

        // when
        var saved = storage.save(mr);

        // then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getGitlabId()).isEqualTo(30101L);
        assertThat(saved.getProjectId()).isEqualTo(project.getId());
        assertThat(saved.getTitle()).isEqualTo("MR Alpha");
        assertThat(saved.getGitlabState()).isEqualTo(MergeRequestGitlabState.OPENED);
        assertThat(saved.getStatus()).isEqualTo(MergeRequestStatus.SYNCING);

        var entityInDb = testHelper.findMergeRequestById(saved.getId());
        assertThat(entityInDb).isPresent();
        assertThat(entityInDb.get().getGitlabId()).isEqualTo(30101L);
        assertThat(entityInDb.get().getTitle()).isEqualTo("MR Alpha");
    }

    @Test
    void saveAll_whenNewMergeRequests_thenAllSaved() {
        // given
        var project = testHelper.createAndSaveProjectEntity(302L, "MR Project 2", ProjectStatus.SYNCED);
        var mr1 = testHelper.buildMergeRequest(30201L, project.getId(), "MR 1", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCING);
        var mr2 = testHelper.buildMergeRequest(30202L, project.getId(), "MR 2", MergeRequestGitlabState.MERGED, MergeRequestStatus.SYNCING);

        // when
        storage.saveAll(List.of(mr1, mr2));

        // then
        var allInDb = testHelper.findAllMergeRequests();
        assertThat(allInDb).hasSize(2)
                .extracting(e -> e.getGitlabId())
                .containsExactlyInAnyOrder(30201L, 30202L);
    }

    @Test
    void saveAll_whenDuplicateMergeRequests_thenDoNothingOnConflict() {
        // given
        var project = testHelper.createAndSaveProjectEntity(3021L, "MR Project 21", ProjectStatus.SYNCED);
        var existing = testHelper.createAndSaveMergeRequestEntity(30211L, project.getId(), "Initial MR", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCED);

        var duplicateMr = testHelper.buildMergeRequest(30211L, project.getId(), "New Title Should Be Ignored", MergeRequestGitlabState.CLOSED, MergeRequestStatus.SYNCING);
        var newMr = testHelper.buildMergeRequest(30212L, project.getId(), "Brand New MR", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCING);

        // when
        storage.saveAll(List.of(duplicateMr, newMr));

        // then
        var allInDb = testHelper.findAllMergeRequests();
        assertThat(allInDb).hasSize(2);

        var existingInDb = testHelper.findMergeRequestById(existing.getId());
        assertThat(existingInDb).isPresent();
        assertThat(existingInDb.get().getTitle()).isEqualTo("Initial MR");
        assertThat(existingInDb.get().getGitlabState()).isEqualTo(MergeRequestGitlabState.OPENED);
        assertThat(existingInDb.get().getStatus()).isEqualTo(MergeRequestStatus.SYNCED);
    }

    @Test
    void updateAll_whenExistingMergeRequestsUpdated_thenFieldsUpdated() {
        // given
        var project = testHelper.createAndSaveProjectEntity(303L, "MR Project 3", ProjectStatus.SYNCED);
        var entity1 = testHelper.createAndSaveMergeRequestEntity(30301L, project.getId(), "Initial 1", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCING);
        var entity2 = testHelper.createAndSaveMergeRequestEntity(30302L, project.getId(), "Initial 2", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCING);

        var mrToUpdate1 = MergeRequest.builder()
                .id(entity1.getId())
                .gitlabId(30301L)
                .projectId(project.getId())
                .title("Updated 1")
                .gitlabState(MergeRequestGitlabState.MERGED)
                .status(MergeRequestStatus.SYNCED)
                .sourceBranch("feature/updated-1")
                .targetBranch("develop")
                .webUrl("https://gitlab.example.com/mr-30301-updated")
                .createdAt(entity1.getCreatedAt())
                .build();

        var mrToUpdate2 = MergeRequest.builder()
                .id(entity2.getId())
                .gitlabId(30302L)
                .projectId(project.getId())
                .title("Updated 2")
                .gitlabState(MergeRequestGitlabState.CLOSED)
                .status(MergeRequestStatus.SYNCED)
                .webUrl("https://gitlab.example.com/mr-30302-updated")
                .createdAt(entity2.getCreatedAt())
                .build();

        // when
        storage.updateAll(List.of(mrToUpdate1, mrToUpdate2));

        // then
        var updated1 = testHelper.findMergeRequestById(entity1.getId());
        assertThat(updated1).isPresent();
        assertThat(updated1.get().getTitle()).isEqualTo("Updated 1");
        assertThat(updated1.get().getGitlabState()).isEqualTo(MergeRequestGitlabState.MERGED);
        assertThat(updated1.get().getStatus()).isEqualTo(MergeRequestStatus.SYNCED);
        assertThat(updated1.get().getSourceBranch()).isEqualTo("feature/updated-1");
        assertThat(updated1.get().getTargetBranch()).isEqualTo("develop");

        var updated2 = testHelper.findMergeRequestById(entity2.getId());
        assertThat(updated2).isPresent();
        assertThat(updated2.get().getTitle()).isEqualTo("Updated 2");
        assertThat(updated2.get().getGitlabState()).isEqualTo(MergeRequestGitlabState.CLOSED);
        assertThat(updated2.get().getStatus()).isEqualTo(MergeRequestStatus.SYNCED);
    }

    @Test
    void findById_whenMergeRequestExists_thenReturnMergeRequest() {
        // given
        var project = testHelper.createAndSaveProjectEntity(304L, "MR Project 4", ProjectStatus.SYNCED);
        var entity = testHelper.createAndSaveMergeRequestEntity(30401L, project.getId(), "MR 30401", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCED);

        // when
        var result = storage.findById(entity.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(entity.getId());
        assertThat(result.get().getGitlabId()).isEqualTo(30401L);
        assertThat(result.get().getProjectId()).isEqualTo(project.getId());
        assertThat(result.get().getTitle()).isEqualTo("MR 30401");
    }

    @Test
    void findById_whenDoesNotExist_thenReturnEmpty() {
        // when
        var result = storage.findById(999999L);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void findAllByProjectId_whenExists_thenReturnMatchingOnly() {
        // given
        var project1 = testHelper.createAndSaveProjectEntity(305L, "MR Project 305", ProjectStatus.SYNCED);
        var project2 = testHelper.createAndSaveProjectEntity(306L, "MR Project 306", ProjectStatus.SYNCED);
        testHelper.createAndSaveMergeRequestEntity(30501L, project1.getId(), "MR 30501", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCED);
        testHelper.createAndSaveMergeRequestEntity(30502L, project1.getId(), "MR 30502", MergeRequestGitlabState.MERGED, MergeRequestStatus.SYNCED);
        testHelper.createAndSaveMergeRequestEntity(30601L, project2.getId(), "MR 30601", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCED);

        // when
        var result = storage.findAllByProjectId(project1.getId());

        // then
        assertThat(result).hasSize(2)
                .extracting(MergeRequest::getGitlabId)
                .containsExactlyInAnyOrder(30501L, 30502L);
    }

    @Test
    void deleteById_whenExists_thenDeleted() {
        // given
        var project = testHelper.createAndSaveProjectEntity(307L, "MR Project 307", ProjectStatus.SYNCED);
        var entity = testHelper.createAndSaveMergeRequestEntity(30701L, project.getId(), "MR To Delete", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCED);

        // when
        storage.deleteById(entity.getId());

        // then
        assertThat(testHelper.findMergeRequestById(entity.getId())).isEmpty();
    }

    @Test
    void save_whenNewMergeRequestWithAuthorAndApprover_thenSavedAndFieldsAssigned() {
        // given
        var project = testHelper.createAndSaveProjectEntity(308L, "MR Project 8", ProjectStatus.SYNCED);
        var author = testHelper.createAndSaveUserEntity(30801L, "author308", "Author 308", UserGitlabState.ACTIVE);
        var approver = testHelper.createAndSaveUserEntity(30802L, "approver308", "Approver 308", UserGitlabState.ACTIVE);
        var now = Instant.now();
        var mr = MergeRequest.builder()
                .gitlabId(30803L)
                .projectId(project.getId())
                .authorId(author.getId())
                .approverId(approver.getId())
                .title("MR With Users")
                .gitlabState(MergeRequestGitlabState.MERGED)
                .status(MergeRequestStatus.SYNCED)
                .sourceBranch("feature/branch-30803")
                .targetBranch("main")
                .webUrl("https://gitlab.example.com/mr-30803")
                .rowsAdded(10)
                .rowsDeleted(5)
                .approvedAt(now)
                .mergedAt(now)
                .createdAt(now)
                .build();

        // when
        var saved = storage.save(mr);

        // then
        assertThat(saved).isNotNull();
        assertThat(saved.getAuthorId()).isEqualTo(author.getId());
        assertThat(saved.getApproverId()).isEqualTo(approver.getId());
        assertThat(saved.getSourceBranch()).isEqualTo("feature/branch-30803");
        assertThat(saved.getTargetBranch()).isEqualTo("main");
        assertThat(saved.getRowsAdded()).isEqualTo(10);
        assertThat(saved.getRowsDeleted()).isEqualTo(5);
    }

    @Test
    void findByProjectIdAndGitlabId_whenExists_thenReturnMergeRequest() {
        // given
        var project = testHelper.createAndSaveProjectEntity(309L, "MR Project 9", ProjectStatus.SYNCED);
        var entity = testHelper.createAndSaveMergeRequestEntity(30901L, project.getId(), "MR 30901", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCED);

        // when
        var result = storage.findByProjectIdAndGitlabId(project.getId(), 30901L);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(entity.getId());
    }

    @Test
    void findByGitlabId_whenExists_thenReturnMergeRequest() {
        // given
        var project = testHelper.createAndSaveProjectEntity(310L, "MR Project 10", ProjectStatus.SYNCED);
        var entity = testHelper.createAndSaveMergeRequestEntity(31001L, project.getId(), "MR 31001", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCED);

        // when
        var result = storage.findByGitlabId(31001L);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(entity.getId());
    }

    @Test
    void countByProjectIdAndStatus_whenExists_thenReturnCount() {
        // given
        var project = testHelper.createAndSaveProjectEntity(311L, "MR Project 11", ProjectStatus.SYNCED);
        testHelper.createAndSaveMergeRequestEntity(31101L, project.getId(), "MR 1", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCING);
        testHelper.createAndSaveMergeRequestEntity(31102L, project.getId(), "MR 2", MergeRequestGitlabState.MERGED, MergeRequestStatus.SYNCING);
        testHelper.createAndSaveMergeRequestEntity(31103L, project.getId(), "MR 3", MergeRequestGitlabState.CLOSED, MergeRequestStatus.SYNCED);

        // when
        var count = storage.countByProjectIdAndStatus(project.getId(), MergeRequestStatus.SYNCING);

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void deleteById_whenDoesNotExist_thenNoException() {
        // when & then
        assertThatNoException().isThrownBy(() -> storage.deleteById(999999L));
    }
}
