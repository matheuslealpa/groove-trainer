package io.github.matheuslealpa.groovetrainer.ui;

import java.util.List;

/** Fórmula de compasso. Cada tempo do numerador gera um clique. */
public record TimeSignature(int beats, int noteValue) {

    public static final List<TimeSignature> PRESETS = List.of(
            new TimeSignature(2, 4),
            new TimeSignature(3, 4),
            new TimeSignature(4, 4),
            new TimeSignature(5, 4),
            new TimeSignature(6, 8),
            new TimeSignature(7, 8),
            new TimeSignature(9, 8),
            new TimeSignature(12, 8));

    @Override
    public String toString() {
        return beats + "/" + noteValue;
    }
}
