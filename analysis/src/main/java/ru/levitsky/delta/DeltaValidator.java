package ru.levitsky.delta;

import ru.levitsky.blackholeeh.model.RctBlock;

public class DeltaValidator {
    private DeltaValidator() {
    }

    public static void verify(RctBlock curr, RctBlock prev) {
        verifyComponent(curr.y(), prev.y(), "Y");
        verifyComponent(curr.uPacked(), prev.uPacked(), "U");
        verifyComponent(curr.vPacked(), prev.vPacked(), "V");
    }

    private static void verifyComponent(byte[] curr, byte[] prev, String name) {
        int len = Math.min(curr.length, prev.length);

        for (int i = 0; i < len; i++) {
            int c = curr[i] & 0xFF;
            int p = prev[i] & 0xFF;

            int delta = c - p;
            int recon = (p + delta) & 0xFF;

            if (recon != c) {
                throw new IllegalStateException(
                        "Delta reconstruction failed for " + name +
                        " at index " + i +
                        " (curr=" + c + ", prev=" + p + ")"
                );
            }
        }
    }
}
