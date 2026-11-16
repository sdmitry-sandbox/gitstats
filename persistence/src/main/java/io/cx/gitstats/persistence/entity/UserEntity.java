package io.cx.gitstats.persistence.entity;

import io.cx.gitstats.domain.model.UserGitlabState;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gitlab_id", nullable = false, unique = true)
    private Long gitlabId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "gitlab_state", nullable = false, length = 50)
    private UserGitlabState gitlabState;

    @Column(name = "is_bot", nullable = false)
    private Boolean isBot;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "web_url", length = 500)
    private String webUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
