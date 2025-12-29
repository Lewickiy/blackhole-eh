package ru.levitsky;

import ru.levitsky.delta.DeltaAnalyzer;

public class AnalysisRunner {

    private static final String IMG_DIR = "core/target/classes/img";

    public static void main(String[] args) throws Exception {

        DeltaAnalyzer deltaAnalyzer = new DeltaAnalyzer();
        deltaAnalyzer.run(IMG_DIR);

    }
}
