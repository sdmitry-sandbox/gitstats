package io.cx.gitstats.domain.persistence;

import io.cx.gitstats.domain.model.analytics.UserCommentDailyViewRow;
import io.cx.gitstats.domain.model.analytics.UserMergeRequestDailyViewRow;
import io.cx.gitstats.domain.model.analytics.UserMrHourlyViewRow;

import java.util.List;

public interface UserAnalyticsPersistenceStorage {
    List<UserMergeRequestDailyViewRow> findAllMergeRequests();

    List<UserCommentDailyViewRow> findAllComments();

    List<UserMrHourlyViewRow> findHourlyData(Long userId);
}
