package ru.levitsky.delta;

import ru.levitsky.blackholeeh.model.RctBlock;
import ru.levitsky.blackholeeh.service.BlockSplitter;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class DeltaAnalyzer {

    public void run(String dir) throws Exception {
        File imgDir = new File(dir);
        if (!imgDir.isDirectory()) {
            throw new IllegalStateException(imgDir + " is not a directory");
        }

        for (File imgFile : Objects.requireNonNull(imgDir.listFiles(f ->
                f.getName().endsWith(".jpg") || f.getName().endsWith(".jpeg")))) {

            System.out.println("\n=== Processing " + imgFile.getName() + " ===");

            List<RctBlock> blocks = BlockSplitter.splitIntoRctBlocks(imgFile);
            analyze(blocks);
        }
    }

    private void analyze(List<RctBlock> blocks) {
        ComponentStats yStats = new ComponentStats("Y");
        ComponentStats uStats = new ComponentStats("U (packed)");
        ComponentStats vStats = new ComponentStats("V (packed)");

        RctBlock prev = zeroBlock();

        for (RctBlock curr : blocks) {

            processComponent(curr.y(), prev.y(), yStats);
            processComponent(curr.uPacked(), prev.uPacked(), uStats);
            processComponent(curr.vPacked(), prev.vPacked(), vStats);

            // sanity check
            DeltaValidator.verify(curr, prev);

            prev = curr;
        }

        yStats.printReport();
        uStats.printReport();
        vStats.printReport();
    }

    private void processComponent(
            byte[] curr,
            byte[] prev,
            ComponentStats stats
    ) {
        int len = Math.min(curr.length, prev.length);

        for (int i = 0; i < len; i++) {
            int c = curr[i] & 0xFF;
            int p = prev[i] & 0xFF;
            int delta = c - p;

            stats.observeAbsolute(c);
            stats.observeDelta(delta);
        }
    }

    private RctBlock zeroBlock() {
        return new RctBlock(
                new byte[64],
                new byte[64],
                new byte[64]
        );
    }
}
