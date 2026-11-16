package io.cx.gitstats.domain.service;

import io.cx.gitstats.domain.model.activity.UserActivityItem;
import io.cx.gitstats.domain.persistence.UserActivityPersistenceStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityServiceImpl implements UserActivityService {

    private static final int DEFAULT_ACTIVITY_LIMIT = 100;

    private final UserActivityPersistenceStorage userActivityPersistenceStorage;

    @Override
    public List<UserActivityItem> getUserActivities(
            Long userId,
            LocalDate from,
            LocalDate to,
            boolean includeMr,
            boolean includeApprovals,
            boolean includeComments
    ) {
        if (userId == null) {
            return List.of();
        }
        return userActivityPersistenceStorage.findUserActivities(
                userId,
                from,
                to,
                includeMr,
                includeApprovals,
                includeComments,
                DEFAULT_ACTIVITY_LIMIT
        );
    }
}
