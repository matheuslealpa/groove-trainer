package io.github.matheuslealpa.groovetrainer.audio;

import io.github.matheuslealpa.groovetrainer.rudiment.Stroke.Hand;

import java.util.Random;

/**
 * Sons gerados por código: o clique é uma senoide curta com decaimento exponencial,
 * e o toque do rudimento é um estalo de ruído sobre um corpo grave, lembrando uma caixa.
 */
public final class ClickSound {

    private static final double DURATION_SECONDS = 0.035;
    private static final double STROKE_SECONDS = 0.060;

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

    /** Toque do rudimento. A mão esquerda soa bem mais grave, para dar para distinguir de ouvido. */
    public static float[] stroke(float sampleRate, Hand hand) {
        double frequency = hand == Hand.R ? 260 : 150;
        int length = (int) (sampleRate * STROKE_SECONDS);
        float[] samples = new float[length];
        Random noise = new Random(42); // semente fixa: o som é sempre o mesmo
        for (int i = 0; i < length; i++) {
            double t = (double) i / length;
            double body = 0.55 * Math.exp(-6 * t) * Math.sin(2 * Math.PI * frequency * i / sampleRate);
            double snap = 0.35 * Math.exp(-14 * t) * (noise.nextDouble() * 2 - 1);
            samples[i] = (float) (body + snap);
        }
        return samples;
    }
}
