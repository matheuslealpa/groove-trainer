package io.github.matheuslealpa.groovetrainer.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TapTempoTest {

    private static final long MS = 1_000_000L;

    @Test
    void firstTapHasNoTempo() {
        assertTrue(new TapTempo().tap(0).isEmpty());
    }

    @Test
    void steadyTapsGiveTheirTempo() {
        TapTempo tap = new TapTempo();
        double bpm = 0;
        for (int i = 0; i < 5; i++) {
            var result = tap.tap(i * 500 * MS); // 500 ms = 120 BPM
            if (result.isPresent()) {
                bpm = result.getAsDouble();
            }
        }
        assertEquals(120.0, bpm, 1e-9);
    }

    @Test
    void longPauseStartsOver() {
        TapTempo tap = new TapTempo();
        tap.tap(0);
        tap.tap(1_000 * MS); // 60 BPM
        assertTrue(tap.tap(5_000 * MS).isEmpty(), "pausa longa reinicia");
        assertEquals(150.0, tap.tap(5_400 * MS).getAsDouble(), 1e-9);
    }
}
