package ru.levitsky.delta;

import java.util.Map;

public final class EntropyCalculator {

    public static double shannonEntropy(Map<Integer, Long> hist, long total) {
        double entropy = 0.0;

        for (long count : hist.values()) {
            double p = (double) count / total;
            entropy -= p * (Math.log(p) / Math.log(2));
        }

        return entropy;
    }
}
