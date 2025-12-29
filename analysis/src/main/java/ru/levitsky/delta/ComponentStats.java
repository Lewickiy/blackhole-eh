package ru.levitsky.delta;

import java.util.HashMap;
import java.util.Map;

public class ComponentStats {
    private final String name;

    private long absCount = 0;
    private long deltaCount = 0;

    private long zeroDeltas = 0;
    private long nearZeroDeltas = 0;

    private int absMin = Integer.MAX_VALUE;
    private int absMax = Integer.MIN_VALUE;

    private int deltaMin = Integer.MAX_VALUE;
    private int deltaMax = Integer.MIN_VALUE;

    private final Map<Integer, Long> absHist = new HashMap<>();
    private final Map<Integer, Long> deltaHist = new HashMap<>();

    public ComponentStats(String name) {
        this.name = name;
    }

    public void observeAbsolute(int v) {
        absCount++;
        absMin = Math.min(absMin, v);
        absMax = Math.max(absMax, v);
        absHist.merge(v, 1L, Long::sum);
    }

    public void observeDelta(int d) {
        deltaCount++;
        deltaMin = Math.min(deltaMin, d);
        deltaMax = Math.max(deltaMax, d);
        deltaHist.merge(d, 1L, Long::sum);

        if (d == 0) zeroDeltas++;
        if (Math.abs(d) <= 1) nearZeroDeltas++;
    }

    public void printReport() {
        System.out.println("\n--- Component " + name + " ---");

        double absEntropy = EntropyCalculator.shannonEntropy(absHist, absCount);
        double deltaEntropy = EntropyCalculator.shannonEntropy(deltaHist, deltaCount);

        System.out.printf("Absolute: min=%d max=%d entropy=%.4f%n",
                absMin, absMax, absEntropy);

        System.out.printf("Delta:    min=%d max=%d entropy=%.4f%n",
                deltaMin, deltaMax, deltaEntropy);

        System.out.printf("Zero deltas: %.2f%%%n",
                100.0 * zeroDeltas / deltaCount);

        System.out.printf("|delta|<=1: %.2f%%%n",
                100.0 * nearZeroDeltas / deltaCount);
    }
}
