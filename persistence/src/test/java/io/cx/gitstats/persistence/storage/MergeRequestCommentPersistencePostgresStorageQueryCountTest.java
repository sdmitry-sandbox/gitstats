package io.cx.gitstats.persistence.storage;

import io.cx.gitstats.domain.model.MergeRequestGitlabState;
import io.cx.gitstats.domain.model.MergeRequestStatus;
import io.cx.gitstats.domain.model.ProjectStatus;
import io.cx.gitstats.domain.model.UserGitlabState;
import io.cx.gitstats.persistence.AbstractPersistenceTest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.cx.gitstats.persistence.utils.QueryCountAssertion.assertQueryCountAtMost;
import static io.cx.gitstats.persistence.utils.QueryCountAssertion.assertQueryCountEqual;

@RequiredArgsConstructor
class MergeRequestCommentPersistencePostgresStorageQueryCountTest extends AbstractPersistenceTest {
    private final MergeRequestCommentPersistencePostgresStorage storage;

    @Test
    void save_whenNewComment_thenSingleDbQuery() {
        // given
        var project = testHelper.createAndSaveProjectEntity(601L, "QC Comment Project 1", ProjectStatus.SYNCED);
        var author = testHelper.createAndSaveUserEntity(60101L, "qcauthor601", "QC Author 601", UserGitlabState.ACTIVE);
        var mr = testHelper.createAndSaveMergeRequestEntity(60102L, project.getId(), "QC MR 601", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCED);
        var comment = testHelper.buildMergeRequestComment(60103L, mr.getId(), author.getId(), "Message");

        // when & then
        assertQueryCountEqual(1, () -> storage.save(comment));
    }

    @Test
    void saveAll_whenMultipleNewComments_thenSingleDbQuery() {
        // given
        var project = testHelper.createAndSaveProjectEntity(602L, "QC Comment Project 2", ProjectStatus.SYNCED);
        var author = testHelper.createAndSaveUserEntity(60201L, "qcauthor602", "QC Author 602", UserGitlabState.ACTIVE);
        var mr = testHelper.createAndSaveMergeRequestEntity(60202L, project.getId(), "QC MR 602", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCED);
        var c1 = testHelper.buildMergeRequestComment(60203L, mr.getId(), author.getId(), "Message 1");
        var c2 = testHelper.buildMergeRequestComment(60204L, mr.getId(), author.getId(), "Message 2");

        // when & then
        assertQueryCountEqual(1, () -> storage.saveAll(List.of(c1, c2)));
    }

    @Test
    void findById_whenCommentExists_thenSingleDbQuery() {
        // given
        var project = testHelper.createAndSaveProjectEntity(603L, "QC Comment Project 3", ProjectStatus.SYNCED);
        var author = testHelper.createAndSaveUserEntity(60301L, "qcauthor603", "QC Author 603", UserGitlabState.ACTIVE);
        var mr = testHelper.createAndSaveMergeRequestEntity(60302L, project.getId(), "QC MR 603", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCED);
        var entity = testHelper.createAndSaveMergeRequestCommentEntity(60303L, mr.getId(), author.getId(), "Message");

        // when & then
        assertQueryCountEqual(1, () -> storage.findById(entity.getId()));
    }

    @Test
    void findAllByMergeRequestId_whenExists_thenSingleDbQuery() {
        // given
        var project = testHelper.createAndSaveProjectEntity(604L, "QC Comment Project 4", ProjectStatus.SYNCED);
        var author = testHelper.createAndSaveUserEntity(60401L, "qcauthor604", "QC Author 604", UserGitlabState.ACTIVE);
        var mr = testHelper.createAndSaveMergeRequestEntity(60402L, project.getId(), "QC MR 604", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCED);
        testHelper.createAndSaveMergeRequestCommentEntity(60403L, mr.getId(), author.getId(), "Message 1");
        testHelper.createAndSaveMergeRequestCommentEntity(60404L, mr.getId(), author.getId(), "Message 2");

        // when & then
        assertQueryCountEqual(1, () -> storage.findAllByMergeRequestId(mr.getId()));
    }

    @Test
    void findAll_whenExists_thenSingleDbQuery() {
        // given
        var project = testHelper.createAndSaveProjectEntity(605L, "QC Comment Project 5", ProjectStatus.SYNCED);
        var author = testHelper.createAndSaveUserEntity(60501L, "qcauthor605", "QC Author 605", UserGitlabState.ACTIVE);
        var mr = testHelper.createAndSaveMergeRequestEntity(60502L, project.getId(), "QC MR 605", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCED);
        testHelper.createAndSaveMergeRequestCommentEntity(60503L, mr.getId(), author.getId(), "Message");

        // when & then
        assertQueryCountEqual(1, () -> storage.findAll());
    }

    @Test
    void deleteById_whenExists_thenAtMostTwoDbQueries() {
        // given
        var project = testHelper.createAndSaveProjectEntity(606L, "QC Comment Project 6", ProjectStatus.SYNCED);
        var author = testHelper.createAndSaveUserEntity(60601L, "qcauthor606", "QC Author 606", UserGitlabState.ACTIVE);
        var mr = testHelper.createAndSaveMergeRequestEntity(60602L, project.getId(), "QC MR 606", MergeRequestGitlabState.OPENED, MergeRequestStatus.SYNCED);
        var entity = testHelper.createAndSaveMergeRequestCommentEntity(60603L, mr.getId(), author.getId(), "Message");

        // when & then
        assertQueryCountAtMost(2, () -> storage.deleteById(entity.getId()));
    }
}
