package io.github.matheuslealpa.groovetrainer.audio;

/**
 * Sons de clique gerados por código: uma senoide curta com decaimento exponencial.
 */
public final class ClickSound {

    private static final double DURATION_SECONDS = 0.035;

    private ClickSound() {
    }

    /** Clique do tempo 1 (acento): mais agudo e mais forte. */
    public static float[] accent(float sampleRate) {
        return generate(sampleRate, 1600, 0.95);
    }

    /** Clique dos demais tempos. */
    public static float[] beat(float sampleRate) {
        return generate(sampleRate, 1000, 0.65);
    }

    static float[] generate(float sampleRate, double frequency, double amplitude) {
        int length = (int) (sampleRate * DURATION_SECONDS);
        float[] samples = new float[length];
        double decay = 5.0 / length; // ~-43 dB no final do clique
        for (int i = 0; i < length; i++) {
            double envelope = Math.exp(-decay * i);
            samples[i] = (float) (amplitude * envelope * Math.sin(2 * Math.PI * frequency * i / sampleRate));
        }
        return samples;
    }
}
