package io.github.matheuslealpa.groovetrainer.rudiment;

import io.github.matheuslealpa.groovetrainer.rudiment.Stroke.Dynamic;
import io.github.matheuslealpa.groovetrainer.rudiment.Stroke.Hand;
import io.github.matheuslealpa.groovetrainer.rudiment.Stroke.Ornament;

import java.util.ArrayList;
import java.util.List;

/**
 * Um rudimento: um padrão de toques que se repete a cada {@code lengthBeats} tempos.
 *
 * @param grid notas por tempo da grade em que o padrão foi escrito (4 = semicolcheias)
 */
public record Rudiment(String name, Family family, int grid, int lengthBeats, List<Stroke> strokes) {

    public enum Family {
        ROLL("Rulos"), DIDDLE("Diddles"), FLAM("Flams"), DRAG("Drags");

        private final String label;

        Family(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public Rudiment {
        strokes = List.copyOf(strokes);
    }

    /**
     * Cria um rudimento a partir de uma notação curta, um símbolo por nota da grade,
     * separados por espaço:
     *
     * <ul>
     *   <li>{@code R} / {@code L}: toque com a mão direita / esquerda</li>
     *   <li>{@code >}: acento (ex.: {@code >R})</li>
     *   <li>{@code f}: flam, {@code d}: drag (ex.: {@code fR}, {@code d>L})</li>
     *   <li>{@code -}: pausa</li>
     * </ul>
     *
     * @param grid notas por tempo (2 colcheias, 3 tercinas, 4 semicolcheias, 8 fusas...)
     */
    public static Rudiment of(String name, Family family, int grid, String notation) {
        if (grid < 1) {
            throw new IllegalArgumentException("Grade precisa ser ao menos 1: " + grid);
        }
        String[] tokens = notation.trim().split("\\s+");
        if (tokens.length % grid != 0) {
            throw new IllegalArgumentException(name + ": " + tokens.length
                    + " notas não fecham tempos inteiros numa grade de " + grid);
        }
        List<Stroke> strokes = new ArrayList<>();
        for (int i = 0; i < tokens.length; i++) {
            if (!tokens[i].equals("-")) {
                strokes.add(parse(name, tokens[i], (double) i / grid));
            }
        }
        if (strokes.isEmpty()) {
            throw new IllegalArgumentException(name + ": o padrão não tem nenhum toque");
        }
        return new Rudiment(name, family, grid, tokens.length / grid, strokes);
    }

    private static Stroke parse(String name, String token, double beatPos) {
        Dynamic dynamic = Dynamic.TAP;
        Ornament ornament = Ornament.NONE;
        int last = token.length() - 1;
        for (int i = 0; i < last; i++) {
            switch (token.charAt(i)) {
                case '>' -> dynamic = Dynamic.ACCENT;
                case 'f' -> ornament = Ornament.FLAM;
                case 'd' -> ornament = Ornament.DRAG;
                default -> throw invalid(name, token);
            }
        }
        Hand hand = switch (token.charAt(last)) {
            case 'R' -> Hand.R;
            case 'L' -> Hand.L;
            default -> throw invalid(name, token);
        };
        return new Stroke(beatPos, hand, dynamic, ornament);
    }

    private static IllegalArgumentException invalid(String name, String token) {
        return new IllegalArgumentException(name + ": símbolo inválido \"" + token + "\"");
    }

    /** A manulação em sequência, ex.: {@code RLRRLRLL}. */
    public String sticking() {
        StringBuilder sb = new StringBuilder();
        for (Stroke s : strokes) {
            sb.append(s.hand());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return name;
    }
}
