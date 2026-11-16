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
class ProjectPersistencePostgresStorageQueryCountTest extends AbstractPersistenceTest {
    private final ProjectPersistencePostgresStorage storage;

    @Test
    void save_whenNewProject_thenSingleDbQuery() {
        // given
        var project = testHelper.buildProject(201L, "QueryCount Project 1", ProjectStatus.CREATING);

        // when & then
        assertQueryCountEqual(1, () -> storage.save(project));
    }

    @Test
    void findByGitlabId_whenProjectExists_thenSingleDbQuery() {
        // given
        testHelper.createAndSaveProjectEntity(202L, "QueryCount Project 2", ProjectStatus.SYNCED);

        // when & then
        assertQueryCountEqual(1, () -> storage.findByGitlabId(202L));
    }

    @Test
    void findByGitlabId_whenProjectWithMergeRequestsExists_thenSingleDbQuery() {
        // given
        var project = testHelper.createAndSaveProjectEntity(2020L, "QueryCount Project With MRs", ProjectStatus.SYNCED);
        testHelper.createAndSaveMergeRequestEntity(20201L, project.getId(), "MR 1", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCED);
        testHelper.createAndSaveMergeRequestEntity(20202L, project.getId(), "MR 2", MergeRequestGitlabState.MERGED, MergeRequestStatus.SYNCED);

        // when & then
        assertQueryCountEqual(1, () -> storage.findByGitlabId(2020L));
    }

    @Test
    void findAll_whenMultipleProjects_thenSingleDbQuery() {
        // given
        testHelper.createAndSaveProjectEntity(203L, "QueryCount Project 3", ProjectStatus.CREATING);
        testHelper.createAndSaveProjectEntity(204L, "QueryCount Project 4", ProjectStatus.SYNCED);
        testHelper.createAndSaveProjectEntity(205L, "QueryCount Project 5", ProjectStatus.SYNCED);

        // when & then
        assertQueryCountEqual(1, () -> storage.findAll());
    }

    @Test
    void findAll_whenMultipleProjectsWithMergeRequests_thenSingleDbQuery() {
        // given
        testHelper.createAndSaveProjectEntity(2030L, "QueryCount Project 30", ProjectStatus.CREATING);
        var p40 = testHelper.createAndSaveProjectEntity(2040L, "QueryCount Project 40", ProjectStatus.SYNCED);
        testHelper.createAndSaveMergeRequestEntity(20401L, p40.getId(), "MR 1", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCED);
        testHelper.createAndSaveMergeRequestEntity(20402L, p40.getId(), "MR 2", MergeRequestGitlabState.MERGED, MergeRequestStatus.SYNCED);
        var p50 = testHelper.createAndSaveProjectEntity(2050L, "QueryCount Project 50", ProjectStatus.SYNCED);
        testHelper.createAndSaveMergeRequestEntity(20501L, p50.getId(), "MR 3", MergeRequestGitlabState.CLOSED, MergeRequestStatus.SYNCED);

        // when & then
        assertQueryCountEqual(1, () -> storage.findAll());
    }

    @Test
    void findAllByStatus_whenMultipleProjects_thenSingleDbQuery() {
        // given
        testHelper.createAndSaveProjectEntity(207L, "QueryCount Project 7", ProjectStatus.CREATING);
        testHelper.createAndSaveProjectEntity(208L, "QueryCount Project 8", ProjectStatus.SYNCED);
        testHelper.createAndSaveProjectEntity(209L, "QueryCount Project 9", ProjectStatus.SYNCED);

        // when & then
        assertQueryCountEqual(1, () -> storage.findAllByStatus(ProjectStatus.SYNCED));
    }

    @Test
    void saveAll_whenMultipleProjects_thenSingleDbQuery() {
        // given
        var project1 = testHelper.buildProject(210L, "QueryCount Project 10", ProjectStatus.CREATING);
        var project2 = testHelper.buildProject(211L, "QueryCount Project 11", ProjectStatus.CREATING);

        // when & then
        assertQueryCountEqual(1, () -> storage.saveAll(List.of(project1, project2)));
    }

    @Test
    void findExistingGitlabIds_whenMultipleIds_thenSingleDbQuery() {
        // given
        testHelper.createAndSaveProjectEntity(212L, "QueryCount Project 12", ProjectStatus.CREATING);
        testHelper.createAndSaveProjectEntity(213L, "QueryCount Project 13", ProjectStatus.SYNCED);

        // when & then
        assertQueryCountEqual(1, () -> storage.findExistingGitlabIds(List.of(212L, 213L, 214L)));
    }

    @Test
    void deleteById_whenProjectExists_thenAtMostTwoDbQueries() {
        // given
        var entity = testHelper.createAndSaveProjectEntity(206L, "QueryCount Project 6", ProjectStatus.CREATING);

        // when & then
        // find project, find merge requests for cascade, delete project
        assertQueryCountAtMost(3, () -> storage.deleteById(entity.getId()));
    }
}