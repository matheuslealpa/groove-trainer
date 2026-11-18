package io.github.matheuslealpa.groovetrainer.audio;

import java.util.function.Consumer;

/**
 * Gera o áudio do metrônomo amostra por amostra.
 *
 * <p>Cada clique é posicionado na amostra exata em que o tempo cai, então a precisão
 * é de 1/44100 s, independente do agendador de threads do sistema operacional.
 * A posição do próximo tempo é acumulada em {@code double} para não perder frações
 * de amostra (ex.: 7/8 a 137 BPM).
 *
 * <p>Os setters podem ser chamados de outra thread (a interface); as mudanças entram
 * em vigor a partir do próximo tempo.
 */
public final class ClickRenderer {

    /** Um tempo que foi agendado na amostra {@code frame}. */
    public record Beat(long frame, int index, boolean accent) {
    }

    private final float sampleRate;
    private final float[] accentClick;
    private final float[] beatClick;

    private volatile double bpm;
    private volatile int beatsPerBar;
    private volatile boolean accentEnabled = true;
    private volatile float volume = 0.8f;

    private long currentFrame;
    private double nextBeatFrame;
    private int beatIndex;

    private float[] activeClick;
    private int activeClickPos;

    public ClickRenderer(float sampleRate, double bpm, int beatsPerBar) {
        this.sampleRate = sampleRate;
        this.accentClick = ClickSound.accent(sampleRate);
        this.beatClick = ClickSound.beat(sampleRate);
        setBpm(bpm);
        setBeatsPerBar(beatsPerBar);
    }

    /**
     * Preenche {@code out} com {@code frames} amostras mono em [-1, 1].
     *
     * @param onBeat chamado para cada tempo que começa dentro deste bloco
     */
    public void render(float[] out, int frames, Consumer<Beat> onBeat) {
        float vol = volume;
        for (int i = 0; i < frames; i++) {
            if (currentFrame >= nextBeatFrame) {
                startBeat(onBeat);
            }

            float sample = 0f;
            if (activeClick != null) {
                sample = activeClick[activeClickPos++] * vol;
                if (activeClickPos >= activeClick.length) {
                    activeClick = null;
                }
            }
            out[i] = sample;
            currentFrame++;
        }
    }

    private void startBeat(Consumer<Beat> onBeat) {
        int bar = beatsPerBar;
        if (beatIndex >= bar) {
            beatIndex = 0;
        }
        boolean accent = beatIndex == 0 && accentEnabled && bar > 1;

        activeClick = accent ? accentClick : beatClick;
        activeClickPos = 0;
        onBeat.accept(new Beat(currentFrame, beatIndex, accent));

        nextBeatFrame += framesPerBeat();
        beatIndex = (beatIndex + 1) % bar;
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
}
