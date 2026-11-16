package io.cx.gitstats.persistence.entity;

import io.cx.gitstats.domain.model.MergeRequestGitlabState;
import io.cx.gitstats.domain.model.MergeRequestStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "merge_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gitlab_id", nullable = false)
    private Long gitlabId;

    @Column(name = "title", length = 500)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "gitlab_state", nullable = false, length = 50)
    private MergeRequestGitlabState gitlabState;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private MergeRequestStatus status;

    @Column(name = "source_branch")
    private String sourceBranch;

    @Column(name = "target_branch")
    private String targetBranch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private UserEntity author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private UserEntity approver;

    @Column(name = "web_url", length = 500)
    private String webUrl;

    @Column(name = "rows_added")
    private Integer rowsAdded;

    @Column(name = "rows_deleted")
    private Integer rowsDeleted;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "merged_at")
    private Instant mergedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "ai_review_started_at")
    private Instant aiReviewStartedAt;

    @Column(name = "ai_review_finished_at")
    private Instant aiReviewFinishedAt;

    @Column(name = "ready_for_review_at")
    private Instant readyForReviewAt;

    @Column(name = "review_requested_at")
    private Instant reviewRequestedAt;

    @Column(name = "review_finished_at")
    private Instant reviewFinishedAt;

    @Column(name = "human_review_duration_seconds")
    private Long humanReviewDurationSeconds;

    @Column(name = "bot_review_duration_seconds")
    private Long botReviewDurationSeconds;
}
