package io.github.matheuslealpa.groovetrainer.audio;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.OptionalDouble;

/**
 * Calcula o BPM a partir de toques do usuário (média dos últimos intervalos).
 * Uma pausa maior que {@link #RESET_AFTER_NANOS} recomeça a contagem.
 */
public final class TapTempo {

    static final long RESET_AFTER_NANOS = 2_000_000_000L;
    private static final int MAX_INTERVALS = 6;

    private final Deque<Long> intervals = new ArrayDeque<>();
    private long lastTap = -1;

    /**
     * Registra um toque.
     *
     * @param nanoTime instante do toque, ex.: {@link System#nanoTime()}
     * @return o BPM estimado, ou vazio se ainda só houve um toque
     */
    public OptionalDouble tap(long nanoTime) {
        if (lastTap >= 0 && nanoTime - lastTap > RESET_AFTER_NANOS) {
            intervals.clear();
            lastTap = -1;
        }
        if (lastTap >= 0) {
            intervals.addLast(nanoTime - lastTap);
            if (intervals.size() > MAX_INTERVALS) {
                intervals.removeFirst();
            }
        }
        lastTap = nanoTime;

        if (intervals.isEmpty()) {
            return OptionalDouble.empty();
        }
        double avgNanos = intervals.stream().mapToLong(Long::longValue).average().orElseThrow();
        return OptionalDouble.of(60_000_000_000.0 / avgNanos);
    }
}
