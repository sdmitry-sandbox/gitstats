package io.cx.gitstats.domain.service.analytics;

import io.cx.gitstats.domain.model.analytics.BoxPlotPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StatisticsMathUtils {

    private StatisticsMathUtils() {}

    public static double calculateMedian(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        var sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        return calculatePercentile(sorted, 50.0);
    }

    public static double calculatePercentile(List<Double> sortedValues, double percentile) {
        if (sortedValues == null || sortedValues.isEmpty()) {
            return 0.0;
        }
        if (sortedValues.size() == 1) {
            return sortedValues.get(0);
        }
        double rank = (percentile / 100.0) * (sortedValues.size() - 1);
        int lower = (int) Math.floor(rank);
        int upper = (int) Math.ceil(rank);
        if (lower == upper) {
            return sortedValues.get(lower);
        }
        double weight = rank - lower;
        return sortedValues.get(lower) * (1.0 - weight) + sortedValues.get(upper) * weight;
    }

    public static BoxPlotPoint calculateBoxPlot(String category, List<Double> values) {
        if (values == null || values.isEmpty()) {
            return BoxPlotPoint.builder()
                    .category(category)
                    .min(0).q1(0).median(0).q3(0).max(0)
                    .build();
        }
        var sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        return BoxPlotPoint.builder()
                .category(category)
                .min(sorted.get(0))
                .q1(calculatePercentile(sorted, 25.0))
                .median(calculatePercentile(sorted, 50.0))
                .q3(calculatePercentile(sorted, 75.0))
                .max(sorted.get(sorted.size() - 1))
                .build();
    }
}
