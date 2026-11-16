package io.cx.gitstats.domain.model.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityItem {
    private Long id;
    private Long gitlabId;
    private UserActivityType type;
    private Long mergeRequestId;
    private Long mrGitlabId;
    private String mrTitle;
    private String mrWebUrl;
    private Long projectId;
    private String projectName;
    private Long userId;
    private String username;
    private Instant activityTime;
    private LocalDate contributionDate;
    private Integer rowsAdded;
    private Integer rowsDeleted;
    private String commentMessage;
    private String reactionTime;
}
