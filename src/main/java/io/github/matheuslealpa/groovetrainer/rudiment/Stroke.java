package io.github.matheuslealpa.groovetrainer.rudiment;

/**
 * Um toque de um rudimento.
 *
 * @param beatPos posição em tempos desde o início do padrão (0.25 = segunda semicolcheia)
 * @param ornament apojatura tocada pela mão oposta logo antes deste toque
 */
public record Stroke(double beatPos, Hand hand, Dynamic dynamic, Ornament ornament) {

    public enum Hand {
        R, L;

        public Hand other() {
            return this == R ? L : R;
        }
    }

    public enum Dynamic { ACCENT, TAP }

    public enum Ornament {
        NONE(0), FLAM(1), DRAG(2);

        private final int graceNotes;

        Ornament(int graceNotes) {
            this.graceNotes = graceNotes;
        }

        /** Quantas apojaturas antecedem o toque. */
        public int graceNotes() {
            return graceNotes;
        }
    }
}
