package io.cx.gitstats.persistence.storage;

import io.cx.gitstats.domain.model.MergeRequestComment;
import io.cx.gitstats.domain.model.MergeRequestGitlabState;
import io.cx.gitstats.domain.model.MergeRequestStatus;
import io.cx.gitstats.domain.model.ProjectStatus;
import io.cx.gitstats.domain.model.UserGitlabState;
import io.cx.gitstats.persistence.AbstractPersistenceTest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@RequiredArgsConstructor
class MergeRequestCommentPersistencePostgresStorageTest extends AbstractPersistenceTest {
    private final MergeRequestCommentPersistencePostgresStorage storage;

    @Test
    void save_whenNewComment_thenSavedAndIdAssigned() {
        // given
        var project = testHelper.createAndSaveProjectEntity(501L, "Comment Project 1", ProjectStatus.SYNCED);
        var author = testHelper.createAndSaveUserEntity(50101L, "author501", "Author 501", UserGitlabState.ACTIVE);
        var mr = testHelper.createAndSaveMergeRequestEntity(50102L, project.getId(), "MR 501", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCED);
        var comment = testHelper.buildMergeRequestComment(50103L, mr.getId(), author.getId(), "Nice MR!");

        // when
        var saved = storage.save(comment);

        // then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getGitlabId()).isEqualTo(50103L);
        assertThat(saved.getMergeRequestId()).isEqualTo(mr.getId());
        assertThat(saved.getAuthorId()).isEqualTo(author.getId());
        assertThat(saved.getMessage()).isEqualTo("Nice MR!");

        var entityInDb = testHelper.findMergeRequestCommentById(saved.getId());
        assertThat(entityInDb).isPresent();
        assertThat(entityInDb.get().getGitlabId()).isEqualTo(50103L);
        assertThat(entityInDb.get().getMessage()).isEqualTo("Nice MR!");
    }

    @Test
    void saveAll_whenNewComments_thenAllSaved() {
        // given
        var project = testHelper.createAndSaveProjectEntity(502L, "Comment Project 2", ProjectStatus.SYNCED);
        var author = testHelper.createAndSaveUserEntity(50201L, "author502", "Author 502", UserGitlabState.ACTIVE);
        var mr = testHelper.createAndSaveMergeRequestEntity(50202L, project.getId(), "MR 502", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCED);
        var c1 = testHelper.buildMergeRequestComment(50203L, mr.getId(), author.getId(), "Comment 1");
        var c2 = testHelper.buildMergeRequestComment(50204L, mr.getId(), author.getId(), "Comment 2");

        // when
        storage.saveAll(List.of(c1, c2));

        // then
        var allInDb = testHelper.findAllMergeRequestComments();
        assertThat(allInDb).hasSize(2)
                .extracting(e -> e.getGitlabId())
                .containsExactlyInAnyOrder(50203L, 50204L);
    }

    @Test
    void saveAll_whenDuplicateComments_thenDoNothingOnConflict() {
        // given
        var project = testHelper.createAndSaveProjectEntity(503L, "Comment Project 3", ProjectStatus.SYNCED);
        var author = testHelper.createAndSaveUserEntity(50301L, "author503", "Author 503", UserGitlabState.ACTIVE);
        var mr = testHelper.createAndSaveMergeRequestEntity(50302L, project.getId(), "MR 503", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCED);
        var existing = testHelper.createAndSaveMergeRequestCommentEntity(50303L, mr.getId(), author.getId(), "Initial Message");

        var duplicateComment = testHelper.buildMergeRequestComment(50303L, mr.getId(), author.getId(), "Ignored Message");
        var newComment = testHelper.buildMergeRequestComment(50304L, mr.getId(), author.getId(), "New Message");

        // when
        storage.saveAll(List.of(duplicateComment, newComment));

        // then
        var allInDb = testHelper.findAllMergeRequestComments();
        assertThat(allInDb).hasSize(2);

        var existingInDb = testHelper.findMergeRequestCommentById(existing.getId());
        assertThat(existingInDb).isPresent();
        assertThat(existingInDb.get().getMessage()).isEqualTo("Initial Message");
    }

    @Test
    void findById_whenCommentExists_thenReturnComment() {
        // given
        var project = testHelper.createAndSaveProjectEntity(504L, "Comment Project 4", ProjectStatus.SYNCED);
        var author = testHelper.createAndSaveUserEntity(50401L, "author504", "Author 504", UserGitlabState.ACTIVE);
        var mr = testHelper.createAndSaveMergeRequestEntity(50402L, project.getId(), "MR 504", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCED);
        var entity = testHelper.createAndSaveMergeRequestCommentEntity(50403L, mr.getId(), author.getId(), "Comment 504");

        // when
        var result = storage.findById(entity.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(entity.getId());
        assertThat(result.get().getGitlabId()).isEqualTo(50403L);
        assertThat(result.get().getMessage()).isEqualTo("Comment 504");
    }

    @Test
    void findById_whenDoesNotExist_thenReturnEmpty() {
        // when
        var result = storage.findById(999999L);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void findAllByMergeRequestId_whenExists_thenReturnMatchingOnly() {
        // given
        var project = testHelper.createAndSaveProjectEntity(505L, "Comment Project 5", ProjectStatus.SYNCED);
        var author = testHelper.createAndSaveUserEntity(50501L, "author505", "Author 505", UserGitlabState.ACTIVE);
        var mr1 = testHelper.createAndSaveMergeRequestEntity(50502L, project.getId(), "MR 505-1", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCED);
        var mr2 = testHelper.createAndSaveMergeRequestEntity(50503L, project.getId(), "MR 505-2", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCED);

        testHelper.createAndSaveMergeRequestCommentEntity(50504L, mr1.getId(), author.getId(), "C1 for MR1");
        testHelper.createAndSaveMergeRequestCommentEntity(50505L, mr1.getId(), author.getId(), "C2 for MR1");
        testHelper.createAndSaveMergeRequestCommentEntity(50506L, mr2.getId(), author.getId(), "C3 for MR2");

        // when
        var result = storage.findAllByMergeRequestId(mr1.getId());

        // then
        assertThat(result).hasSize(2)
                .extracting(MergeRequestComment::getGitlabId)
                .containsExactlyInAnyOrder(50504L, 50505L);
    }

    @Test
    void deleteById_whenExists_thenDeleted() {
        // given
        var project = testHelper.createAndSaveProjectEntity(506L, "Comment Project 6", ProjectStatus.SYNCED);
        var author = testHelper.createAndSaveUserEntity(50601L, "author506", "Author 506", UserGitlabState.ACTIVE);
        var mr = testHelper.createAndSaveMergeRequestEntity(50602L, project.getId(), "MR 506", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCED);
        var entity = testHelper.createAndSaveMergeRequestCommentEntity(50603L, mr.getId(), author.getId(), "To Delete");

        // when
        storage.deleteById(entity.getId());

        // then
        assertThat(testHelper.findMergeRequestCommentById(entity.getId())).isEmpty();
    }

    @Test
    void deleteById_whenDoesNotExist_thenNoException() {
        // when & then
        assertThatNoException().isThrownBy(() -> storage.deleteById(999999L));
    }
}
