package io.cx.gitstats.domain.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ContributionThresholds {
    int level1Max;
    int level2Max;
    int level3Max;
    int level4Max;

    public static ContributionThresholds calculate(List<Integer> counts) {
        if (counts == null || counts.isEmpty()) {
            return defaultThresholds();
        }

        List<Integer> nonZero = counts.stream()
                .filter(c -> c != null && c > 0)
                .sorted()
                .toList();

        if (nonZero.isEmpty()) {
            return defaultThresholds();
        }

        return computeQuantiles(nonZero);
    }

    private static ContributionThresholds defaultThresholds() {
        return ContributionThresholds.builder()
                .level1Max(1)
                .level2Max(2)
                .level3Max(3)
                .level4Max(4)
                .build();
    }

    private static ContributionThresholds computeQuantiles(List<Integer> nonZero) {
        int n = nonZero.size();
        int q1 = nonZero.get((int) Math.floor((n - 1) * 0.25));
        int q2 = nonZero.get((int) Math.floor((n - 1) * 0.50));
        int q3 = nonZero.get((int) Math.floor((n - 1) * 0.75));
        int max = nonZero.get(n - 1);

        int t1 = Math.max(1, q1);
        int t2 = Math.max(t1 + 1, q2);
        int t3 = Math.max(t2 + 1, q3);
        int t4 = Math.max(t3 + 1, max);

        return ContributionThresholds.builder()
                .level1Max(t1)
                .level2Max(t2)
                .level3Max(t3)
                .level4Max(t4)
                .build();
    }

    public int getLevel(int count) {
        if (count <= 0) {
            return 0;
        }
        if (count <= level1Max) {
            return 1;
        }
        if (count <= level2Max) {
            return 2;
        }
        if (count <= level3Max) {
            return 3;
        }
        return 4;
    }
}
