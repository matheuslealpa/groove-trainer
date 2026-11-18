package io.github.matheuslealpa.groovetrainer.audio;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClickRendererTest {

    private static final float RATE = 44_100f;

    private static List<ClickRenderer.Beat> renderBeats(ClickRenderer r, int totalFrames, int chunk) {
        List<ClickRenderer.Beat> beats = new ArrayList<>();
        float[] buf = new float[chunk];
        for (int done = 0; done < totalFrames; done += chunk) {
            r.render(buf, chunk, beats::add);
        }
        return beats;
    }

    @Test
    void beatsFallOnExactSamplesAt120Bpm() {
        ClickRenderer r = new ClickRenderer(RATE, 120, 4);
        List<ClickRenderer.Beat> beats = renderBeats(r, 44_100 * 2 + 1, 256);

        // 120 BPM = 0.5 s por tempo = 22050 amostras
        for (int i = 0; i < beats.size(); i++) {
            assertEquals(i * 22_050L, beats.get(i).frame());
        }
        assertEquals(5, beats.size()); // 0, 0.5, 1.0, 1.5, 2.0 s
    }

    @Test
    void fractionalBeatLengthDoesNotDrift() {
        // 137 BPM -> 19313.138... amostras por tempo: arredondar a cada tempo acumularia erro
        ClickRenderer r = new ClickRenderer(RATE, 137, 4);
        List<ClickRenderer.Beat> beats = renderBeats(r, 44_100 * 60, 441);

        assertEquals(137, beats.size(), "um minuto a 137 BPM deve ter 137 tempos");
        double perBeat = RATE * 60.0 / 137;
        for (int i = 0; i < beats.size(); i++) {
            assertEquals(Math.ceil(i * perBeat), beats.get(i).frame(), 0.0);
        }
    }

    @Test
    void accentOnlyOnFirstBeatOfBar() {
        ClickRenderer r = new ClickRenderer(RATE, 300, 3);
        List<ClickRenderer.Beat> beats = renderBeats(r, 44_100 * 2, 256);

        for (ClickRenderer.Beat b : beats) {
            assertEquals(b.index() == 0, b.accent(), "tempo " + b.index());
        }
        assertEquals(List.of(0, 1, 2, 0, 1, 2), beats.subList(0, 6).stream().map(ClickRenderer.Beat::index).toList());
    }

    @Test
    void accentCanBeDisabled() {
        ClickRenderer r = new ClickRenderer(RATE, 200, 4);
        r.setAccentEnabled(false);
        assertTrue(renderBeats(r, 44_100 * 2, 256).stream().noneMatch(ClickRenderer.Beat::accent));
    }

    @Test
    void shrinkingTheBarWrapsToTheDownbeat() {
        ClickRenderer r = new ClickRenderer(RATE, 120, 7);
        List<ClickRenderer.Beat> beats = new ArrayList<>(renderBeats(r, 22_050 * 5 + 1, 1)); // tempos 0..5
        assertEquals(5, beats.getLast().index());

        r.setBeatsPerBar(4);
        beats.clear();
        float[] buf = new float[22_050];
        r.render(buf, buf.length, beats::add);
        assertEquals(0, beats.getFirst().index());
    }

    @Test
    void producesSoundOnlyRightAfterTheBeat() {
        ClickRenderer r = new ClickRenderer(RATE, 60, 4);
        float[] buf = new float[44_100];
        r.render(buf, buf.length, b -> { });

        assertTrue(maxAbs(buf, 0, 1_000) > 0.1f, "clique no início");
        assertFalse(maxAbs(buf, 5_000, 44_100) > 0f, "silêncio entre os cliques");
    }

    private static float maxAbs(float[] buf, int from, int to) {
        float max = 0;
        for (int i = from; i < to; i++) {
            max = Math.max(max, Math.abs(buf[i]));
        }
        return max;
    }
}
