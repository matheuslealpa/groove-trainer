package io.github.matheuslealpa.groovetrainer.audio;

import io.github.matheuslealpa.groovetrainer.rudiment.Rudiment;
import io.github.matheuslealpa.groovetrainer.rudiment.Rudiment.Family;
import io.github.matheuslealpa.groovetrainer.rudiment.Rudiments;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

    private static List<ClickRenderer.StrokeEvent> renderStrokes(ClickRenderer r, int totalFrames, int chunk) {
        List<ClickRenderer.StrokeEvent> strokes = new ArrayList<>();
        float[] buf = new float[chunk];
        for (int done = 0; done < totalFrames; done += chunk) {
            r.render(buf, chunk, b -> { }, strokes::add);
        }
        return strokes;
    }

    @Test
    void rudimentStrokesFallOnExactSubdivisions() {
        Rudiment paradiddle = Rudiments.byName("Paradiddle simples").orElseThrow();
        ClickRenderer r = new ClickRenderer(RATE, 120, 4);
        r.setRudiment(paradiddle);
        List<ClickRenderer.StrokeEvent> strokes = renderStrokes(r, 44_100 * 2, 441);

        // 120 BPM: semicolcheia = 22050 / 4 amostras; o padrão de 8 toques dá a volta
        assertEquals(16, strokes.size());
        for (int i = 0; i < strokes.size(); i++) {
            assertEquals((long) Math.ceil(i * 22_050 / 4.0), strokes.get(i).frame(), "toque " + i);
            assertEquals(i % 8, strokes.get(i).strokeIndex());
        }
    }

    @Test
    void rudimentDoesNotDriftAtFractionalTempo() {
        ClickRenderer r = new ClickRenderer(RATE, 137, 4);
        r.setRudiment(Rudiments.byName("Toque simples").orElseThrow());
        List<ClickRenderer.StrokeEvent> strokes = renderStrokes(r, 44_100 * 60, 441);

        assertEquals(137 * 4, strokes.size());
        double perStroke = RATE * 60.0 / 137 / 4;
        for (int i = 0; i < strokes.size(); i++) {
            assertEquals(i * perStroke, strokes.get(i).frame(), 1.0, "toque " + i);
        }
    }

    @Test
    void withoutRudimentTheOutputIsJustTheClick() {
        ClickRenderer r = new ClickRenderer(RATE, 60, 4);
        float[] buf = new float[2_000];
        r.render(buf, buf.length, b -> { });

        float[] click = ClickSound.accent(RATE);
        for (int i = 0; i < click.length; i++) {
            assertEquals(click[i] * 0.8f, buf[i], 1e-6f);
        }
        assertFalse(maxAbs(buf, click.length, buf.length) > 0f);
    }

    @Test
    void strokeIsMixedWithTheClickInsteadOfCuttingIt() {
        ClickRenderer withStrokes = new ClickRenderer(RATE, 60, 4);
        withStrokes.setRudiment(Rudiment.of("um por tempo", Family.ROLL, 1, "R"));
        ClickRenderer clickOnly = new ClickRenderer(RATE, 60, 4);
        ClickRenderer strokeOnly = new ClickRenderer(RATE, 60, 4);
        strokeOnly.setRudiment(Rudiment.of("um por tempo", Family.ROLL, 1, "R"));
        strokeOnly.setVolume(0f);

        float[] mixed = new float[4_000];
        float[] click = new float[4_000];
        float[] stroke = new float[4_000];
        withStrokes.render(mixed, mixed.length, b -> { });
        clickOnly.render(click, click.length, b -> { });
        strokeOnly.render(stroke, stroke.length, b -> { });

        assertTrue(maxAbs(stroke, 0, 1_000) > 0.1f, "o toque tem som próprio");
        float[] expected = new float[4_000];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = Math.clamp(click[i] + stroke[i], -1f, 1f);
        }
        assertArrayEquals(expected, mixed, 1e-6f);
    }

    @Test
    void stereoPutsEachHandOnItsOwnSide() {
        // 60 BPM, colcheias R L: a direita cai em 0 e a esquerda em 22050
        ClickRenderer r = new ClickRenderer(RATE, 60, 4);
        r.setRudiment(Rudiment.of("alternado", Family.ROLL, 2, "R L"));
        r.setVolume(0f);
        float[] left = new float[30_000];
        float[] right = new float[30_000];
        r.renderStereo(left, right, left.length, b -> { }, s -> { });

        assertTrue(maxAbs(right, 0, 1_000) > 3 * maxAbs(left, 0, 1_000), "mão direita no canal direito");
        assertTrue(maxAbs(left, 22_050, 23_050) > 3 * maxAbs(right, 22_050, 23_050), "mão esquerda no canal esquerdo");
        assertTrue(maxAbs(left, 0, 1_000) > 0f, "um pouco do toque vaza para o outro canal");
    }

    @Test
    void stereoKeepsTheClickCentered() {
        ClickRenderer r = new ClickRenderer(RATE, 60, 4);
        float[] left = new float[2_000];
        float[] right = new float[2_000];
        r.renderStereo(left, right, left.length, b -> { }, s -> { });

        assertTrue(maxAbs(left, 0, 1_000) > 0.1f);
        assertArrayEquals(left, right);
    }

    @Test
    void flamGraceNoteSoundsJustBeforeTheMainStroke() {
        // 60 BPM, um flam por tempo: a apojatura do 2º tempo soa 30 ms antes dele
        ClickRenderer r = new ClickRenderer(RATE, 60, 4);
        r.setRudiment(Rudiments.byName("Flam").orElseThrow());
        r.setVolume(0f);
        float[] buf = new float[44_100 + 4_000];
        r.render(buf, buf.length, b -> { });

        int grace = 44_100 - (int) (0.030 * RATE);
        assertFalse(maxAbs(buf, 10_000, grace) > 0f, "silêncio antes da apojatura");
        float graceLevel = maxAbs(buf, grace, grace + 500);
        float mainLevel = maxAbs(buf, 44_100, 44_100 + 500);
        assertTrue(graceLevel > 0f, "apojatura antes do tempo");
        assertTrue(mainLevel > 2 * graceLevel, "a nota principal é bem mais forte que a apojatura");
    }

    @Test
    void changingTheRudimentRestartsThePattern() {
        ClickRenderer r = new ClickRenderer(RATE, 120, 4);
        r.setRudiment(Rudiments.byName("Paradiddle simples").orElseThrow());
        renderStrokes(r, 22_050 + 1, 1); // entra no 2º tempo do padrão

        Rudiment doubles = Rudiments.byName("Toque duplo").orElseThrow();
        r.setRudiment(doubles);
        List<ClickRenderer.StrokeEvent> strokes = renderStrokes(r, 22_050, 1);
        assertEquals(doubles, strokes.getFirst().rudiment());
        assertEquals(0, strokes.getFirst().strokeIndex());

        r.setRudiment(null);
        assertTrue(renderStrokes(r, 44_100, 256).isEmpty());
    }

    private static float maxAbs(float[] buf, int from, int to) {
        float max = 0;
        for (int i = from; i < to; i++) {
            max = Math.max(max, Math.abs(buf[i]));
        }
        return max;
    }
}
