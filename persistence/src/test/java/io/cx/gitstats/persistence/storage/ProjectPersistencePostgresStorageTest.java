package io.cx.gitstats.persistence.storage;

import io.cx.gitstats.domain.model.MergeRequest;
import io.cx.gitstats.domain.model.MergeRequestGitlabState;
import io.cx.gitstats.domain.model.MergeRequestStatus;
import io.cx.gitstats.domain.model.Project;
import io.cx.gitstats.domain.model.ProjectStatus;
import io.cx.gitstats.persistence.AbstractPersistenceTest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@RequiredArgsConstructor
class ProjectPersistencePostgresStorageTest extends AbstractPersistenceTest {
    private final ProjectPersistencePostgresStorage storage;

    @Test
    void save_whenNewProject_thenSavedAndIdAssigned() {
        // given
        var project = testHelper.buildProject(101L, "Project Alpha", ProjectStatus.CREATING);

        // when
        var saved = storage.save(project);

        // then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getGitlabId()).isEqualTo(101L);
        assertThat(saved.getName()).isEqualTo("Project Alpha");
        assertThat(saved.getStatus()).isEqualTo(ProjectStatus.CREATING);
        assertThat(saved.getLastSyncedAt()).isNull();

        var entityInDb = testHelper.findProjectById(saved.getId());
        assertThat(entityInDb).isPresent();
        assertThat(entityInDb.get().getGitlabId()).isEqualTo(101L);
        assertThat(entityInDb.get().getName()).isEqualTo("Project Alpha");
        assertThat(entityInDb.get().getStatus()).isEqualTo(ProjectStatus.CREATING);
        assertThat(entityInDb.get().getLastSyncedAt()).isNull();
    }

    @Test
    void save_whenExistingProjectUpdated_thenFieldsUpdated() {
        // given
        var initialEntity = testHelper.createAndSaveProjectEntity(102L, "Initial Name", ProjectStatus.CREATING);
        var syncTime = Instant.parse("2026-11-18T12:00:00Z");
        var projectToUpdate = Project.builder()
                .id(initialEntity.getId())
                .gitlabId(102L)
                .name("Updated Name")
                .webUrl("https://gitlab.example.com/updated")
                .status(ProjectStatus.SYNCED)
                .createdAt(initialEntity.getCreatedAt())
                .updatedAt(Instant.now())
                .lastSyncedAt(syncTime)
                .build();

        // when
        var updated = storage.save(projectToUpdate);

        // then
        assertThat(updated.getId()).isEqualTo(initialEntity.getId());
        assertThat(updated.getName()).isEqualTo("Updated Name");
        assertThat(updated.getWebUrl()).isEqualTo("https://gitlab.example.com/updated");
        assertThat(updated.getStatus()).isEqualTo(ProjectStatus.SYNCED);
        assertThat(updated.getLastSyncedAt()).isEqualTo(syncTime);

        var entityInDb = testHelper.findProjectById(initialEntity.getId());
        assertThat(entityInDb).isPresent();
        assertThat(entityInDb.get().getName()).isEqualTo("Updated Name");
        assertThat(entityInDb.get().getStatus()).isEqualTo(ProjectStatus.SYNCED);
        assertThat(entityInDb.get().getLastSyncedAt()).isEqualTo(syncTime);
    }

    @Test
    void findByGitlabId_whenProjectExists_thenReturnProject() {
        // given
        var entity = testHelper.createAndSaveProjectEntity(103L, "Project 103", ProjectStatus.SYNCED);

        // when
        var result = storage.findByGitlabId(103L);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(entity.getId());
        assertThat(result.get().getGitlabId()).isEqualTo(103L);
        assertThat(result.get().getName()).isEqualTo("Project 103");
        assertThat(result.get().getStatus()).isEqualTo(ProjectStatus.SYNCED);
    }

    @Test
    void findByGitlabId_whenProjectWithMergeRequestsExists_thenReturnProjectWithMergeRequests() {
        // given
        var project = testHelper.createAndSaveProjectEntity(1030L, "Project With MRs", ProjectStatus.SYNCED);
        testHelper.createAndSaveMergeRequestEntity(10301L, project.getId(), "MR 1", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCED);
        testHelper.createAndSaveMergeRequestEntity(10302L, project.getId(), "MR 2", MergeRequestGitlabState.MERGED, MergeRequestStatus.SYNCED);

        // when
        var result = storage.findByGitlabId(1030L);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getGitlabId()).isEqualTo(1030L);
        assertThat(result.get().getMergeRequests()).hasSize(2)
                .extracting(MergeRequest::getGitlabId)
                .containsExactlyInAnyOrder(10301L, 10302L);
    }

    @Test
    void findByGitlabId_whenProjectDoesNotExist_thenReturnEmpty() {
        // given
        var nonExistentGitlabId = 999999L;

        // when
        var result = storage.findByGitlabId(nonExistentGitlabId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void findAll_whenProjectsExist_thenReturnAllProjects() {
        // given
        testHelper.createAndSaveProjectEntity(104L, "Project 104", ProjectStatus.CREATING);
        testHelper.createAndSaveProjectEntity(105L, "Project 105", ProjectStatus.SYNCED);

        // when
        var result = storage.findAll();

        // then
        assertThat(result).hasSize(2)
                .extracting(Project::getGitlabId)
                .containsExactlyInAnyOrder(104L, 105L);
    }

    @Test
    void findAll_whenProjectsWithMergeRequestsExist_thenReturnAllProjectsWithMergeRequests() {
        // given
        testHelper.createAndSaveProjectEntity(1040L, "Project 1040", ProjectStatus.CREATING);
        var project1050Entity = testHelper.createAndSaveProjectEntity(1050L, "Project 1050", ProjectStatus.SYNCED);
        testHelper.createAndSaveMergeRequestEntity(10501L, project1050Entity.getId(), "MR 1", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCED);

        // when
        var result = storage.findAll();

        // then
        assertThat(result).hasSize(2);
        var project1050 = result.stream().filter(p -> p.getGitlabId().equals(1050L)).findFirst();
        assertThat(project1050).isPresent();
        assertThat(project1050.get().getMergeRequests()).hasSize(1);
        assertThat(project1050.get().getMergeRequests().get(0).getGitlabId()).isEqualTo(10501L);
    }

    @Test
    void findAll_whenNoProjects_thenReturnEmptyList() {
        // when
        var result = storage.findAll();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void findAllByStatus_whenProjectsWithStatusExist_thenReturnMatchingOnly() {
        // given
        testHelper.createAndSaveProjectEntity(107L, "Project 107", ProjectStatus.CREATING);
        testHelper.createAndSaveProjectEntity(108L, "Project 108", ProjectStatus.SYNCED);
        testHelper.createAndSaveProjectEntity(109L, "Project 109", ProjectStatus.SYNCED);

        // when
        var result = storage.findAllByStatus(ProjectStatus.SYNCED);

        // then
        assertThat(result).hasSize(2)
                .extracting(Project::getGitlabId)
                .containsExactlyInAnyOrder(108L, 109L);
    }

    @Test
    void saveAll_whenMultipleProjects_thenAllProjectsSaved() {
        // given
        var project1 = testHelper.buildProject(110L, "Project 110", ProjectStatus.CREATING);
        var project2 = testHelper.buildProject(111L, "Project 111", ProjectStatus.CREATING);

        // when
        storage.saveAll(List.of(project1, project2));

        // then
        var p1InDb = storage.findByGitlabId(110L);
        var p2InDb = storage.findByGitlabId(111L);
        assertThat(p1InDb).isPresent();
        assertThat(p2InDb).isPresent();
    }

    @Test
    void findExistingGitlabIds_whenSomeProjectsExist_thenReturnExistingIdsOnly() {
        // given
        testHelper.createAndSaveProjectEntity(112L, "Project 112", ProjectStatus.CREATING);
        testHelper.createAndSaveProjectEntity(113L, "Project 113", ProjectStatus.SYNCED);

        // when
        var existing = storage.findExistingGitlabIds(List.of(112L, 113L, 114L));

        // then
        assertThat(existing).containsExactlyInAnyOrder(112L, 113L);
    }

    @Test
    void deleteById_whenProjectExists_thenProjectDeleted() {
        // given
        var entity = testHelper.createAndSaveProjectEntity(106L, "To Delete", ProjectStatus.CREATING);

        // when
        storage.deleteById(entity.getId());

        // then
        assertThat(testHelper.findProjectById(entity.getId())).isEmpty();
    }

    @Test
    void deleteById_whenProjectDoesNotExist_thenNoException() {
        // given
        var nonExistentId = 999999L;

        // when & then
        assertThatNoException().isThrownBy(() -> storage.deleteById(nonExistentId));
    }

    @Test
    void searchByName_whenMatched_thenReturnMatchingProjects() {
        // given
        testHelper.createAndSaveProjectEntity(115L, "dns_ad-t_pdns", ProjectStatus.SYNCED);
        testHelper.createAndSaveProjectEntity(116L, "other_project", ProjectStatus.SYNCED);

        // when
        var result = storage.searchByName("ad-");

        // then
        assertThat(result).extracting(Project::getName).containsExactly("dns_ad-t_pdns");
    }
}