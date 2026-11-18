package io.github.matheuslealpa.groovetrainer.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Toca o metrônomo numa thread dedicada, mandando um fluxo contínuo de áudio para a placa de som.
 *
 * <p>Para a interface piscar junto com o som (e não quando o áudio é gerado, que acontece
 * alguns milissegundos antes), os tempos ficam numa fila e só são entregues em
 * {@link #pollPlayedBeats(Consumer)} quando a placa de som realmente chegou naquela amostra.
 */
public final class MetronomeEngine {

    private static final float SAMPLE_RATE = 44_100f;
    private static final int CHUNK_FRAMES = 256;
    /** ~46 ms de buffer: baixo o bastante pra resposta rápida, alto o bastante pra não engasgar. */
    private static final int LINE_BUFFER_FRAMES = 2048;

    private final AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
    private final Queue<ClickRenderer.Beat> pendingBeats = new ConcurrentLinkedQueue<>();

    private volatile double bpm = 100;
    private volatile int beatsPerBar = 4;
    private volatile boolean accentEnabled = true;
    private volatile float volume = 0.8f;

    private volatile ClickRenderer renderer;
    private volatile SourceDataLine line;
    private volatile boolean running;
    private Thread audioThread;

    public synchronized void start() throws LineUnavailableException {
        if (running) {
            return;
        }
        SourceDataLine newLine = AudioSystem.getSourceDataLine(format);
        newLine.open(format, LINE_BUFFER_FRAMES * format.getFrameSize());

        ClickRenderer newRenderer = new ClickRenderer(SAMPLE_RATE, bpm, beatsPerBar);
        newRenderer.setAccentEnabled(accentEnabled);
        newRenderer.setVolume(volume);

        pendingBeats.clear();
        renderer = newRenderer;
        line = newLine;
        running = true;

        newLine.start();
        audioThread = Thread.ofPlatform()
                .name("metronome-audio")
                .daemon(true)
                .priority(Thread.MAX_PRIORITY)
                .start(() -> audioLoop(newLine, newRenderer));
    }

    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        try {
            audioThread.join(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        SourceDataLine oldLine = line;
        oldLine.stop();
        oldLine.flush();
        oldLine.close();
        line = null;
        renderer = null;
        pendingBeats.clear();
    }

    public boolean isRunning() {
        return running;
    }

    private void audioLoop(SourceDataLine out, ClickRenderer clicks) {
        float[] samples = new float[CHUNK_FRAMES];
        byte[] bytes = new byte[CHUNK_FRAMES * 2];
        while (running) {
            clicks.render(samples, CHUNK_FRAMES, pendingBeats::add);
            for (int i = 0; i < CHUNK_FRAMES; i++) {
                short s = (short) (Math.clamp(samples[i], -1f, 1f) * Short.MAX_VALUE);
                bytes[2 * i] = (byte) s;
                bytes[2 * i + 1] = (byte) (s >> 8);
            }
            out.write(bytes, 0, bytes.length); // bloqueia enquanto o buffer da placa está cheio
        }
    }

    /**
     * Entrega os tempos que já saíram na caixa de som. Chame a cada frame da interface.
     */
    public void pollPlayedBeats(Consumer<ClickRenderer.Beat> consumer) {
        SourceDataLine current = line;
        if (current == null) {
            return;
        }
        long played = current.getLongFramePosition();
        ClickRenderer.Beat beat;
        while ((beat = pendingBeats.peek()) != null && beat.frame() <= played) {
            pendingBeats.poll();
            consumer.accept(beat);
        }
    }

    public void setBpm(double bpm) {
        this.bpm = bpm;
        ClickRenderer r = renderer;
        if (r != null) {
            r.setBpm(bpm);
        }
    }

    public double getBpm() {
        return bpm;
    }

    public void setBeatsPerBar(int beatsPerBar) {
        this.beatsPerBar = beatsPerBar;
        ClickRenderer r = renderer;
        if (r != null) {
            r.setBeatsPerBar(beatsPerBar);
        }
    }

    public void setAccentEnabled(boolean accentEnabled) {
        this.accentEnabled = accentEnabled;
        ClickRenderer r = renderer;
        if (r != null) {
            r.setAccentEnabled(accentEnabled);
        }
    }

    public boolean isAccentEnabled() {
        return accentEnabled;
    }

    public void setVolume(float volume) {
        this.volume = volume;
        ClickRenderer r = renderer;
        if (r != null) {
            r.setVolume(volume);
        }
    }
}
