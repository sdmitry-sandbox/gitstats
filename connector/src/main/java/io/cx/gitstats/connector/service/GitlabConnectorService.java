package io.cx.gitstats.connector.service;

import io.cx.gitstats.connector.gitlab.client.DefaultApi;
import io.cx.gitstats.connector.gitlab.model.*;
import io.cx.gitstats.connector.mapper.ConnectorMapper;
import io.cx.gitstats.domain.connector.GitlabConnector;
import io.cx.gitstats.domain.model.MergeRequest;
import io.cx.gitstats.domain.model.MergeRequestComment;
import io.cx.gitstats.domain.model.Project;
import io.cx.gitstats.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitlabConnectorService implements GitlabConnector {
    public static final Set<Long> KNOWN_GITLAB_BOT_IDS = Set.of(428L);

    public static final int DEFAULT_PAGE_SIZE = 100;
    private static final String APPROVAL_MESSAGE = "approved this merge request";
    private static final String AI_START_MARKER = "Автоматический код";
    private static final String AI_BODY_MARKER = "AI Review";
    private static final String READY_MARKER = "marked this merge request as **ready**";
    private static final String REVIEW_REQUESTED_MARKER = "requested review from";

    private final DefaultApi gitlabApi;
    private final ConnectorMapper connectorMapper;

    @Override
    public Optional<Project> getProject(Long gitlabId) {
        if (gitlabId == null) {
            return Optional.empty();
        }

        try {
            log.info("Requesting gitlab project by id: {}", gitlabId);
            ProjectResponse response = gitlabApi.getProject(gitlabId);
            if (response == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(connectorMapper.toDomain(response));
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("GitLab project not found for id: {}", gitlabId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to fetch project from GitLab for id: {}. Error: {}", gitlabId, e.getMessage(), e);
            throw new RuntimeException("GitLab API error: " + e.getMessage(), e);
        }
    }

    @Override
    public List<MergeRequest> getMergeRequests(Long projectGitlabId) {
        if (projectGitlabId == null) {
            return List.of();
        }
        try {
            log.info("Requesting gitlab merge requests for projectGitlabId: {}", projectGitlabId);
            return collectMergeRequests(projectGitlabId);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("GitLab project not found for id: {}", projectGitlabId);
            return List.of();
        } catch (Exception e) {
            log.error("Failed to fetch merge requests from GitLab for projectGitlabId: {}. Error: {}", projectGitlabId, e.getMessage(), e);
            throw new RuntimeException("GitLab API error: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> getUser(Long gitlabId) {
        if (gitlabId == null) {
            return Optional.empty();
        }
        try {
            log.info("Requesting gitlab user by id: {}", gitlabId);
            var response = gitlabApi.getUser(gitlabId);
            if (response == null) {
                return Optional.empty();
            }
            response.setBot(KNOWN_GITLAB_BOT_IDS.contains(gitlabId));
            return Optional.ofNullable(connectorMapper.toDomain(response));
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("GitLab user not found for id: {}", gitlabId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to fetch user from GitLab for id: {}. Error: {}", gitlabId, e.getMessage(), e);
            throw new RuntimeException("GitLab API error: " + e.getMessage(), e);
        }
    }

    @Override
    public void setMergeRequestChanges(Long projectGitlabId, MergeRequest mergeRequest) {
        if (projectGitlabId == null || mergeRequest == null || mergeRequest.getGitlabId() == null) {
            return;
        }
        try {
            log.info("Requesting gitlab merge request changes for projectId: {}, mrId: {}", projectGitlabId, mergeRequest.getGitlabId());
            MergeRequestChangesResponse response = gitlabApi.getMergeRequestChanges(projectGitlabId, mergeRequest.getGitlabId());
            if (response == null) {
                return;
            }
            applyMergeRequestChanges(mergeRequest, response);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("GitLab merge request changes not found for projectId: {}, mrId: {}", projectGitlabId, mergeRequest.getGitlabId());
        } catch (Exception e) {
            log.error("Failed to fetch MR changes for projectId: {}, mrId: {}. Error: {}", projectGitlabId, mergeRequest.getGitlabId(), e.getMessage(), e);
            throw new RuntimeException("GitLab API error: " + e.getMessage(), e);
        }
    }

    @Override
    public void setMergeRequestCommentsData(Long projectGitlabId, MergeRequest mergeRequest) {
        if (projectGitlabId == null || mergeRequest == null || mergeRequest.getGitlabId() == null) {
            return;
        }
        try {
            log.info("Requesting gitlab merge request discussions for projectId: {}, mrId: {}", projectGitlabId, mergeRequest.getGitlabId());
            List<DiscussionResponse> discussions = collectDiscussions(projectGitlabId, mergeRequest.getGitlabId());
            processDiscussions(mergeRequest, discussions);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("GitLab merge request discussions not found for projectId: {}, mrId: {}", projectGitlabId, mergeRequest.getGitlabId());
        } catch (Exception e) {
            log.error("Failed to fetch MR discussions for projectId: {}, mrId: {}. Error: {}", projectGitlabId, mergeRequest.getGitlabId(), e.getMessage(), e);
            throw new RuntimeException("GitLab API error: " + e.getMessage(), e);
        }
    }

    private void applyMergeRequestChanges(MergeRequest mergeRequest, MergeRequestChangesResponse response) {
        var stats = calculateDiffStats(response.getChanges());
        mergeRequest.setRowsAdded(stats.added());
        mergeRequest.setRowsDeleted(stats.deleted());
        mergeRequest.setSourceBranch(response.getSourceBranch());
        mergeRequest.setTargetBranch(response.getTargetBranch());

        if (response.getAuthor() != null) {
            mergeRequest.setAuthorGitlabId(response.getAuthor().getId());
        }
        if (response.getMergedAt() != null) {
            mergeRequest.setMergedAt(connectorMapper.map(response.getMergedAt()));
        }
    }

    private DiffStats calculateDiffStats(List<ChangeResponse> changes) {
        if (changes == null || changes.isEmpty()) {
            return new DiffStats(0, 0);
        }
        int added = 0;
        int deleted = 0;
        for (var change : changes) {
            if (change.getDiff() != null) {
                var lines = change.getDiff().split("\r?\n");
                for (var line : lines) {
                    if (line.startsWith("+") && !line.startsWith("+++")) {
                        added++;
                    } else if (line.startsWith("-") && !line.startsWith("---")) {
                        deleted++;
                    }
                }
            }
        }
        return new DiffStats(added, deleted);
    }

    private List<DiscussionResponse> collectDiscussions(Long projectGitlabId, Long mergeRequestIid) {
        List<DiscussionResponse> allDiscussions = new ArrayList<>();
        int page = 1;
        while (true) {
            List<DiscussionResponse> response = gitlabApi.getMergeRequestDiscussions(
                    projectGitlabId, mergeRequestIid, page, DEFAULT_PAGE_SIZE
            );
            if (response == null || response.isEmpty()) {
                break;
            }
            allDiscussions.addAll(response);
            if (response.size() < DEFAULT_PAGE_SIZE) {
                break;
            }
            page++;
        }
        return allDiscussions;
    }

    private void processDiscussions(MergeRequest mergeRequest, List<DiscussionResponse> discussions) {
        var allNotes = new ArrayList<NoteResponse>();
        var comments = new ArrayList<MergeRequestComment>();
        for (var discussion : discussions) {
            if (discussion.getNotes() == null) {
                continue;
            }
            for (var note : discussion.getNotes()) {
                allNotes.add(note);
                checkApprovalNote(mergeRequest, note);
                if (isEligibleComment(mergeRequest, note)) {
                    comments.add(buildComment(mergeRequest, note));
                }
            }
        }
        allNotes.sort(Comparator.comparing(n -> connectorMapper.map(n.getCreatedAt()), Comparator.nullsLast(Comparator.naturalOrder())));
        mergeRequest.setComments(comments);
        extractReviewMarkers(mergeRequest, allNotes);
    }

    private void extractReviewMarkers(MergeRequest mergeRequest, List<NoteResponse> notes) {
        Instant aiStartedAt = findAiReviewStartedAt(notes);
        Instant aiFinishedAt = findAiReviewFinishedAt(notes, aiStartedAt);
        Instant readyAt = findNoteContaining(notes, READY_MARKER);
        Instant requestedAt = findNoteContaining(notes, REVIEW_REQUESTED_MARKER);
        Instant reviewFinishedAt = findReviewFinishedAt(mergeRequest, notes);

        mergeRequest.setAiReviewStartedAt(aiStartedAt);
        mergeRequest.setAiReviewFinishedAt(aiFinishedAt);
        mergeRequest.setReadyForReviewAt(readyAt);
        mergeRequest.setReviewRequestedAt(requestedAt);
        mergeRequest.setReviewFinishedAt(reviewFinishedAt);

        calculateReviewDurations(mergeRequest);
    }

    private Instant findAiReviewStartedAt(List<NoteResponse> notes) {
        for (var note : notes) {
            if (note.getAuthor() != null && KNOWN_GITLAB_BOT_IDS.contains(note.getAuthor().getId())) {
                String body = note.getBody();
                if (body != null && (body.contains("Автоматический код‑ревью запущен")
                        || body.contains("Автоматический код-ревью запущен")
                        || body.contains(AI_START_MARKER))) {
                    return connectorMapper.map(note.getCreatedAt());
                }
            }
        }
        return null;
    }

    private Instant findAiReviewFinishedAt(List<NoteResponse> notes, Instant aiStartedAt) {
        if (aiStartedAt == null) {
            return null;
        }
        for (var note : notes) {
            if (note.getAuthor() != null && KNOWN_GITLAB_BOT_IDS.contains(note.getAuthor().getId())) {
                Instant noteTime = connectorMapper.map(note.getCreatedAt());
                if (noteTime != null && noteTime.isAfter(aiStartedAt)) {
                    return noteTime;
                }
            }
        }
        return null;
    }

    private Instant findNoteContaining(List<NoteResponse> notes, String substring) {
        for (var note : notes) {
            if (note.getBody() != null && note.getBody().contains(substring)) {
                return connectorMapper.map(note.getCreatedAt());
            }
        }
        return null;
    }

    private Instant findReviewFinishedAt(MergeRequest mergeRequest, List<NoteResponse> notes) {
        for (var note : notes) {
            if (isHumanReviewerComment(mergeRequest, note)) {
                return connectorMapper.map(note.getCreatedAt());
            }
        }
        return null;
    }

    private boolean isHumanReviewerComment(MergeRequest mergeRequest, NoteResponse note) {
        if (note.getAuthor() == null) {
            return false;
        }
        Long authorId = note.getAuthor().getId();
        if (authorId == null || Objects.equals(authorId, mergeRequest.getAuthorGitlabId())) {
            return false;
        }
        if (KNOWN_GITLAB_BOT_IDS.contains(authorId)) {
            return false;
        }
        String body = note.getBody();
        return body != null && !body.isBlank()
                && !APPROVAL_MESSAGE.equals(body.trim())
                && !body.contains(READY_MARKER)
                && !body.contains(REVIEW_REQUESTED_MARKER);
    }

    private void calculateReviewDurations(MergeRequest mr) {
        Instant reviewBase = calculateReviewBaseTime(mr);
        if (mr.getReviewFinishedAt() != null) {
            mr.setHumanReviewDurationSeconds(calculateDurationSeconds(reviewBase, mr.getReviewFinishedAt()));
        } else if (mr.getApprovedAt() != null) {
            mr.setHumanReviewDurationSeconds(calculateDurationSeconds(reviewBase, mr.getApprovedAt()));
        }
        if (mr.getAiReviewStartedAt() != null && mr.getAiReviewFinishedAt() != null) {
            mr.setBotReviewDurationSeconds(calculateDurationSeconds(mr.getAiReviewStartedAt(), mr.getAiReviewFinishedAt()));
        }
    }

    private Instant calculateReviewBaseTime(MergeRequest mr) {
        Instant requestedAt = mr.getReviewRequestedAt();
        Instant readyAt = mr.getReadyForReviewAt();
        if (requestedAt != null && readyAt != null) {
            return requestedAt.isAfter(readyAt) ? requestedAt : readyAt;
        }
        if (requestedAt != null) {
            return requestedAt;
        }
        if (readyAt != null) {
            return readyAt;
        }
        return mr.getCreatedAt();
    }

    private Long calculateDurationSeconds(Instant start, Instant end) {
        if (start == null || end == null) {
            return null;
        }
        long seconds = Duration.between(start, end).getSeconds();
        return Math.max(0L, seconds);
    }

    private void checkApprovalNote(MergeRequest mergeRequest, NoteResponse note) {
        if (note.getBody() != null && APPROVAL_MESSAGE.equals(note.getBody().trim())) {
            mergeRequest.setApprovedAt(connectorMapper.map(note.getCreatedAt()));
            if (note.getAuthor() != null) {
                mergeRequest.setApproverGitlabId(note.getAuthor().getId());
            }
        }
    }

    private boolean isEligibleComment(MergeRequest mergeRequest, NoteResponse note) {
        if (note.getAuthor() == null) {
            return false;
        }
        if (Objects.equals(note.getAuthor().getId(), mergeRequest.getAuthorGitlabId())) {
            return false;
        }
        var isAiCommentByRealUser = !KNOWN_GITLAB_BOT_IDS.contains(note.getAuthor().getId())
                && note.getBody() != null && note.getBody().contains(AI_BODY_MARKER);
        if (isAiCommentByRealUser) {
            return false;
        }
        return Boolean.TRUE.equals(note.getResolvable());
    }

    private MergeRequestComment buildComment(MergeRequest mergeRequest, NoteResponse note) {
        return MergeRequestComment.builder()
                .gitlabId(note.getId())
                .mergeRequestId(mergeRequest.getId())
                .authorGitlabId(note.getAuthor() != null ? note.getAuthor().getId() : null)
                .message(note.getBody())
                .isResolved(Boolean.TRUE.equals(note.getResolved()))
                .resolvedAt(connectorMapper.map(note.getResolvedAt()))
                .createdAt(connectorMapper.map(note.getCreatedAt()))
                .build();
    }

    private List<MergeRequest> collectMergeRequests(Long projectGitlabId) {
        List<MergeRequest> allMergeRequests = new ArrayList<>();
        int page = 1;
        while (true) {
            List<MergeRequestResponse> response = gitlabApi.getMergeRequests(
                    projectGitlabId, page, DEFAULT_PAGE_SIZE, "created_at", "desc", "simple", "keyset"
            );
            if (response == null || response.isEmpty()) {
                break;
            }
            var mergeRequests = connectorMapper.toDomainMergeRequestList(response);
            allMergeRequests.addAll(mergeRequests);
            if (response.size() < DEFAULT_PAGE_SIZE) {
                break;
            }
            page++;
        }
        return allMergeRequests;
    }

    private record DiffStats(int added, int deleted) {}
}
