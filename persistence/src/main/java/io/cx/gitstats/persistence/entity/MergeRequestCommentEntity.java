package io.cx.gitstats.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "merge_request_comments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeRequestCommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gitlab_id", nullable = false)
    private Long gitlabId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merge_request_id", nullable = false)
    private MergeRequestEntity mergeRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private UserEntity author;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_resolved", nullable = false)
    private Boolean isResolved;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
