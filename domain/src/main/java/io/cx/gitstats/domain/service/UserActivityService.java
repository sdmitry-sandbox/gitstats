package io.cx.gitstats.domain.service;

import io.cx.gitstats.domain.model.activity.UserActivityItem;

import java.time.LocalDate;
import java.util.List;

public interface UserActivityService {

    List<UserActivityItem> getUserActivities(
            Long userId,
            LocalDate from,
            LocalDate to,
            boolean includeMr,
            boolean includeApprovals,
            boolean includeComments
    );
}
