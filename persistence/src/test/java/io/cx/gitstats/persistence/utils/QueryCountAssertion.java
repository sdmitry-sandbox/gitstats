package io.cx.gitstats.persistence.utils;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.ttddyy.dsproxy.QueryCountHolder;
import org.assertj.core.api.Assertions;

import java.util.concurrent.Callable;

@UtilityClass
public class QueryCountAssertion {

    @SneakyThrows
    public static <T> T assertQueryCountEqual(
            long expectedQueryCount,
            Callable<T> codeThatNeedsToBeCheckedHowManyTimesItQueriesDataBase
    ) {
        QueryCountHolder.clear();
        try {
            return codeThatNeedsToBeCheckedHowManyTimesItQueriesDataBase.call();
        } finally {
            long actualCount = QueryCountHolder.getGrandTotal().getTotal();
            Assertions.assertThat(actualCount).isEqualTo(expectedQueryCount);
        }
    }

    @SneakyThrows
    public static void assertQueryCountEqual(
            long expectedQueryCount,
            Runnable codeThatNeedsToBeCheckedHowManyTimesItQueriesDataBase
    ) {
        QueryCountHolder.clear();
        try {
            codeThatNeedsToBeCheckedHowManyTimesItQueriesDataBase.run();
        } finally {
            long actualCount = QueryCountHolder.getGrandTotal().getTotal();
            Assertions.assertThat(actualCount).isEqualTo(expectedQueryCount);
        }
    }

    public static long getQueryCount(Runnable codeThatNeedsToBeCheckedHowManyTimesItQueriesDataBase) {
        QueryCountHolder.clear();
        try {
            codeThatNeedsToBeCheckedHowManyTimesItQueriesDataBase.run();
        } catch (Exception ignore) {
        }

        return QueryCountHolder.getGrandTotal().getTotal();
    }

    @SneakyThrows
    public static void assertQueryCountAtMost(
            long maxQueryCount,
            Runnable codeThatNeedsToBeCheckedHowManyTimesItQueriesDataBase
    ) {
        QueryCountHolder.clear();
        try {
            codeThatNeedsToBeCheckedHowManyTimesItQueriesDataBase.run();
        } finally {
            long actualCount = QueryCountHolder.getGrandTotal().getTotal();
            Assertions.assertThat(actualCount).isLessThanOrEqualTo(maxQueryCount);
        }
    }
}
