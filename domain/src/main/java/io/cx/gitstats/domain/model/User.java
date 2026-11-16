package io.cx.gitstats.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private Long gitlabId;
    private String username;
    private String name;
    private UserGitlabState gitlabState;
    private Boolean isBot;
    private String avatarUrl;
    private String webUrl;
    private Instant createdAt;
}
