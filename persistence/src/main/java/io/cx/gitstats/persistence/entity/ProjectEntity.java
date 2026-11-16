package io.cx.gitstats.persistence.entity;

import io.cx.gitstats.domain.model.ProjectStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "projects")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gitlab_id", nullable = false, unique = true)
    private Long gitlabId;

    @Column(name = "name")
    private String name;

    @Column(name = "web_url", length = 500)
    private String webUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ProjectStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY)
    private List<MergeRequestEntity> mergeRequests;
}
