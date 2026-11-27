package io.github.matheuslealpa.groovetrainer.audio;

import io.github.matheuslealpa.groovetrainer.rudiment.Rudiment;
import io.github.matheuslealpa.groovetrainer.rudiment.Stroke;
import io.github.matheuslealpa.groovetrainer.rudiment.Stroke.Dynamic;
import io.github.matheuslealpa.groovetrainer.rudiment.Stroke.Hand;
import io.github.matheuslealpa.groovetrainer.rudiment.Stroke.Ornament;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Gera o áudio do metrônomo (e do rudimento escolhido) amostra por amostra.
 *
 * <p>Cada clique é posicionado na amostra exata em que o tempo cai, então a precisão
 * é de 1/44100 s, independente do agendador de threads do sistema operacional.
 * A posição do próximo tempo é acumulada em {@code double} para não perder frações
 * de amostra (ex.: 7/8 a 137 BPM).
 *
 * <p>Os toques do rudimento são agendados a cada tempo, a partir da posição exata desse
 * tempo, então acompanham o clique sem acumular erro. Os sons são somados: a apojatura
 * de um flam não é cortada pela nota principal, nem o clique pelo toque.
 *
 * <p>Os setters podem ser chamados de outra thread (a interface); as mudanças entram
 * em vigor a partir do próximo tempo.
 */
public final class ClickRenderer {

    /** Um tempo que foi agendado na amostra {@code frame}. */
    public record Beat(long frame, int index, boolean accent) {
    }

    /** Um toque do rudimento agendado para a amostra {@code frame}. */
    public record StrokeEvent(long frame, Rudiment rudiment, int strokeIndex) {
    }

    private static final float ACCENT_LEVEL = 1.0f;
    private static final float TAP_LEVEL = 0.55f;
    private static final float GRACE_LEVEL = 0.25f;
    /** Quanto de um toque vaza para o canal da outra mão: separa bem sem cansar no fone. */
    private static final float OPPOSITE_CHANNEL = 0.2f;
    private static final double FLAM_MAX_SECONDS = 0.030;
    private static final double DRAG_MAX_SECONDS = 0.060;

    /** Um som em andamento (ou agendado para começar em {@code start}). */
    private static final class Voice {
        final float[] samples;
        final long start;
        final float level;
        final boolean stroke;
        final float leftGain;
        final float rightGain;

        /** @param hand mão que toca, para posicionar no estéreo; {@code null} fica no centro */
        Voice(float[] samples, long start, float level, boolean stroke, Hand hand) {
            this.samples = samples;
            this.start = start;
            this.level = level;
            this.stroke = stroke;
            this.leftGain = hand == Hand.R ? OPPOSITE_CHANNEL : 1f;
            this.rightGain = hand == Hand.L ? OPPOSITE_CHANNEL : 1f;
        }
    }

    private final float sampleRate;
    private final float[] accentClick;
    private final float[] beatClick;
    private final float[] rightStroke;
    private final float[] leftStroke;

    private volatile double bpm;
    private volatile int beatsPerBar;
    private volatile boolean accentEnabled = true;
    private volatile float volume = 0.8f;
    private volatile Rudiment rudiment;
    private volatile float rudimentVolume = 0.8f;

    private long currentFrame;
    private double nextBeatFrame;
    private int beatIndex;

    private Rudiment activeRudiment;
    private int patternBeat;

    private final List<Voice> voices = new ArrayList<>();

    public ClickRenderer(float sampleRate, double bpm, int beatsPerBar) {
        this.sampleRate = sampleRate;
        this.accentClick = ClickSound.accent(sampleRate);
        this.beatClick = ClickSound.beat(sampleRate);
        this.rightStroke = ClickSound.stroke(sampleRate, Hand.R);
        this.leftStroke = ClickSound.stroke(sampleRate, Hand.L);
        setBpm(bpm);
        setBeatsPerBar(beatsPerBar);
    }

    /** Como {@link #render(float[], int, Consumer, Consumer)}, ignorando os toques do rudimento. */
    public void render(float[] out, int frames, Consumer<Beat> onBeat) {
        render(out, frames, onBeat, s -> { });
    }

    /**
     * Preenche {@code out} com {@code frames} amostras mono em [-1, 1].
     *
     * @param onBeat chamado para cada tempo que começa dentro deste bloco
     * @param onStroke chamado, no início de cada tempo, para os toques do rudimento que caem nele
     */
    public void render(float[] out, int frames, Consumer<Beat> onBeat, Consumer<StrokeEvent> onStroke) {
        mix(out, null, frames, onBeat, onStroke);
    }

    /**
     * Versão estéreo: o clique fica no centro, os toques da mão direita vão para o canal
     * direito e os da esquerda para o esquerdo.
     */
    public void renderStereo(float[] left, float[] right, int frames,
                             Consumer<Beat> onBeat, Consumer<StrokeEvent> onStroke) {
        mix(left, right, frames, onBeat, onStroke);
    }

    /** @param right {@code null} para mixar tudo em mono em {@code left} */
    private void mix(float[] left, float[] right, int frames,
                     Consumer<Beat> onBeat, Consumer<StrokeEvent> onStroke) {
        float clickVol = volume;
        float strokeVol = rudimentVolume;
        boolean stereo = right != null;
        for (int i = 0; i < frames; i++) {
            if (currentFrame >= nextBeatFrame) {
                startBeat(onBeat, onStroke);
            }

            float l = 0f;
            float r = 0f;
            for (int v = voices.size() - 1; v >= 0; v--) {
                Voice voice = voices.get(v);
                long pos = currentFrame - voice.start;
                if (pos < 0) {
                    continue;
                }
                float sample = voice.samples[(int) pos] * voice.level * (voice.stroke ? strokeVol : clickVol);
                if (stereo) {
                    l += sample * voice.leftGain;
                    r += sample * voice.rightGain;
                } else {
                    l += sample;
                }
                if (pos + 1 >= voice.samples.length) {
                    voices.remove(v);
                }
            }
            left[i] = Math.clamp(l, -1f, 1f);
            if (stereo) {
                right[i] = Math.clamp(r, -1f, 1f);
            }
            currentFrame++;
        }
    }

    private void startBeat(Consumer<Beat> onBeat, Consumer<StrokeEvent> onStroke) {
        int bar = beatsPerBar;
        if (beatIndex >= bar) {
            beatIndex = 0;
        }
        boolean accent = beatIndex == 0 && accentEnabled && bar > 1;

        voices.removeIf(v -> !v.stroke); // um clique novo corta o anterior
        voices.add(new Voice(accent ? accentClick : beatClick, currentFrame, 1f, false, null));
        onBeat.accept(new Beat(currentFrame, beatIndex, accent));

        scheduleStrokes(onStroke);

        nextBeatFrame += framesPerBeat();
        beatIndex = (beatIndex + 1) % bar;
    }

    /** Agenda os toques do rudimento que caem no tempo que começa em {@code nextBeatFrame}. */
    private void scheduleStrokes(Consumer<StrokeEvent> onStroke) {
        Rudiment r = rudiment;
        if (r != activeRudiment) {
            activeRudiment = r;
            patternBeat = 0;
        }
        if (r == null) {
            return;
        }
        if (patternBeat >= r.lengthBeats()) {
            patternBeat = 0;
        }
        int nextPatternBeat = (patternBeat + 1) % r.lengthBeats();
        double perBeat = framesPerBeat();
        double perGridNote = perBeat / r.grid();

        List<Stroke> strokes = r.strokes();
        for (int i = 0; i < strokes.size(); i++) {
            Stroke s = strokes.get(i);
            double offset = s.beatPos() - patternBeat;
            if (offset >= 0 && offset < 1) {
                long frame = (long) Math.ceil(nextBeatFrame + offset * perBeat);
                float level = s.dynamic() == Dynamic.ACCENT ? ACCENT_LEVEL : TAP_LEVEL;
                voices.add(new Voice(sound(s.hand()), frame, level, true, s.hand()));
                onStroke.accept(new StrokeEvent(frame, r, i));
                if (offset > 0) {
                    scheduleGraceNotes(s, nextBeatFrame + offset * perBeat, perGridNote);
                }
            }
            // A apojatura de um toque que cai na cabeça do próximo tempo soa ainda neste tempo.
            if (s.beatPos() == nextPatternBeat) {
                scheduleGraceNotes(s, nextBeatFrame + perBeat, perGridNote);
            }
        }
        patternBeat = nextPatternBeat;
    }

    private void scheduleGraceNotes(Stroke s, double mainFrame, double perGridNote) {
        int count = s.ornament().graceNotes();
        if (count == 0) {
            return;
        }
        // Flam: uma nota colada na principal. Drag: duas, a primeira no dobro da distância.
        double lead = s.ornament() == Ornament.FLAM
                ? Math.min(FLAM_MAX_SECONDS * sampleRate, perGridNote / 4)
                : Math.min(DRAG_MAX_SECONDS * sampleRate, perGridNote / 2) / count;
        Hand graceHand = s.hand().other();
        float[] sound = sound(graceHand);
        for (int g = 1; g <= count; g++) {
            long frame = (long) Math.ceil(mainFrame - g * lead);
            if (frame >= currentFrame) {
                voices.add(new Voice(sound, frame, GRACE_LEVEL, true, graceHand));
            }
        }
    }

    private float[] sound(Hand hand) {
        return hand == Hand.R ? rightStroke : leftStroke;
    }

    public double framesPerBeat() {
        return sampleRate * 60.0 / bpm;
    }

    public void setBpm(double bpm) {
        if (bpm <= 0) {
            throw new IllegalArgumentException("BPM deve ser positivo: " + bpm);
        }
        this.bpm = bpm;
    }

    public void setBeatsPerBar(int beatsPerBar) {
        if (beatsPerBar < 1) {
            throw new IllegalArgumentException("Compasso precisa ter ao menos 1 tempo: " + beatsPerBar);
        }
        this.beatsPerBar = beatsPerBar;
    }

    public void setAccentEnabled(boolean accentEnabled) {
        this.accentEnabled = accentEnabled;
    }

    public void setVolume(float volume) {
        this.volume = Math.clamp(volume, 0f, 1f);
    }

    /** Rudimento tocado sobre o clique; {@code null} deixa só o metrônomo. Recomeça no próximo tempo. */
    public void setRudiment(Rudiment rudiment) {
        this.rudiment = rudiment;
    }

    public void setRudimentVolume(float rudimentVolume) {
        this.rudimentVolume = Math.clamp(rudimentVolume, 0f, 1f);
    }
}
