package io.cx.gitstats.persistence.storage;

import io.cx.gitstats.domain.model.MergeRequestGitlabState;
import io.cx.gitstats.domain.model.MergeRequestStatus;
import io.cx.gitstats.domain.model.ProjectStatus;
import io.cx.gitstats.persistence.AbstractPersistenceTest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.cx.gitstats.persistence.utils.QueryCountAssertion.assertQueryCountAtMost;
import static io.cx.gitstats.persistence.utils.QueryCountAssertion.assertQueryCountEqual;

@RequiredArgsConstructor
class MergeRequestPersistencePostgresStorageQueryCountTest extends AbstractPersistenceTest {
    private final MergeRequestPersistencePostgresStorage storage;

    @Test
    void save_whenNewMergeRequest_thenSingleDbQuery() {
        // given
        var project = testHelper.createAndSaveProjectEntity(401L, "QC Project 1", ProjectStatus.SYNCED);
        var mr = testHelper.buildMergeRequest(40101L, project.getId(), "MR 1", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCING);

        // when & then
        assertQueryCountEqual(1, () -> storage.save(mr));
    }

    @Test
    void saveAll_whenMultipleNewMergeRequests_thenSingleDbQuery() {
        // given
        var project = testHelper.createAndSaveProjectEntity(402L, "QC Project 2", ProjectStatus.SYNCED);
        var mr1 = testHelper.buildMergeRequest(40201L, project.getId(), "MR 1", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCING);
        var mr2 = testHelper.buildMergeRequest(40202L, project.getId(), "MR 2", MergeRequestGitlabState.MERGED, MergeRequestStatus.SYNCING);
        var mr3 = testHelper.buildMergeRequest(40203L, project.getId(), "MR 3", MergeRequestGitlabState.CLOSED, MergeRequestStatus.SYNCING);

        // when & then
        assertQueryCountEqual(1, () -> storage.saveAll(List.of(mr1, mr2, mr3)));
    }

    @Test
    void updateAll_whenMultipleExistingMergeRequests_thenSingleDbQuery() {
        // given
        var project = testHelper.createAndSaveProjectEntity(403L, "QC Project 3", ProjectStatus.SYNCED);
        var entity1 = testHelper.createAndSaveMergeRequestEntity(40301L, project.getId(), "MR 1", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCING);
        var entity2 = testHelper.createAndSaveMergeRequestEntity(40302L, project.getId(), "MR 2", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCING);

        var mr1 = testHelper.buildMergeRequest(40301L, project.getId(), "MR 1 Updated", MergeRequestGitlabState.MERGED, MergeRequestStatus.SYNCED);
        mr1.setId(entity1.getId());
        var mr2 = testHelper.buildMergeRequest(40302L, project.getId(), "MR 2 Updated", MergeRequestGitlabState.CLOSED, MergeRequestStatus.SYNCED);
        mr2.setId(entity2.getId());

        // when & then
        assertQueryCountEqual(1, () -> storage.updateAll(List.of(mr1, mr2)));
    }

    @Test
    void saveAllAndupdateAll_whenNewAndUpdatedTogether_thenAtMostTwoDbQueries() {
        // given
        var project = testHelper.createAndSaveProjectEntity(404L, "QC Project 4", ProjectStatus.SYNCED);
        var entity1 = testHelper.createAndSaveMergeRequestEntity(40401L, project.getId(), "MR 1", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCING);
        var entity2 = testHelper.createAndSaveMergeRequestEntity(40402L, project.getId(), "MR 2", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCING);

        var updated1 = testHelper.buildMergeRequest(40401L, project.getId(), "MR 1 Updated", MergeRequestGitlabState.MERGED, MergeRequestStatus.SYNCED);
        updated1.setId(entity1.getId());
        var updated2 = testHelper.buildMergeRequest(40402L, project.getId(), "MR 2 Updated", MergeRequestGitlabState.CLOSED, MergeRequestStatus.SYNCED);
        updated2.setId(entity2.getId());

        var new1 = testHelper.buildMergeRequest(40403L, project.getId(), "MR 3 New", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCING);
        var new2 = testHelper.buildMergeRequest(40404L, project.getId(), "MR 4 New", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCING);

        // when & then
        assertQueryCountAtMost(2, () -> {
            storage.saveAll(List.of(new1, new2));
            storage.updateAll(List.of(updated1, updated2));
        });
    }

    @Test
    void findById_whenMergeRequestExists_thenSingleDbQuery() {
        // given
        var project = testHelper.createAndSaveProjectEntity(405L, "QC Project 5", ProjectStatus.SYNCED);
        var entity = testHelper.createAndSaveMergeRequestEntity(40501L, project.getId(), "MR 1", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCED);

        // when & then
        assertQueryCountEqual(1, () -> storage.findById(entity.getId()));
    }

    @Test
    void findAllByProjectId_whenExists_thenSingleDbQuery() {
        // given
        var project = testHelper.createAndSaveProjectEntity(406L, "QC Project 6", ProjectStatus.SYNCED);
        testHelper.createAndSaveMergeRequestEntity(40601L, project.getId(), "MR 1", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCED);
        testHelper.createAndSaveMergeRequestEntity(40602L, project.getId(), "MR 2", MergeRequestGitlabState.MERGED, MergeRequestStatus.SYNCED);

        // when & then
        assertQueryCountEqual(1, () -> storage.findAllByProjectId(project.getId()));
    }

    @Test
    void findAll_whenExists_thenSingleDbQuery() {
        // given
        var project = testHelper.createAndSaveProjectEntity(407L, "QC Project 7", ProjectStatus.SYNCED);
        testHelper.createAndSaveMergeRequestEntity(40701L, project.getId(), "MR 1", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCED);

        // when & then
        assertQueryCountEqual(1, () -> storage.findAll());
    }

    @Test
    void deleteById_whenExists_thenAtMostTwoDbQueries() {
        // given
        var project = testHelper.createAndSaveProjectEntity(408L, "QC Project 8", ProjectStatus.SYNCED);
        var entity = testHelper.createAndSaveMergeRequestEntity(40801L, project.getId(), "MR 1", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCED);

        // when & then
        assertQueryCountAtMost(2, () -> storage.deleteById(entity.getId()));
    }
}
