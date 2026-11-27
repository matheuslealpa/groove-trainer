package io.github.matheuslealpa.groovetrainer.rudiment;

import java.util.List;
import java.util.Optional;

import static io.github.matheuslealpa.groovetrainer.rudiment.Rudiment.Family.DIDDLE;
import static io.github.matheuslealpa.groovetrainer.rudiment.Rudiment.Family.DRAG;
import static io.github.matheuslealpa.groovetrainer.rudiment.Rudiment.Family.FLAM;
import static io.github.matheuslealpa.groovetrainer.rudiment.Rudiment.Family.ROLL;

/** Catálogo de rudimentos. Para adicionar um, basta mais uma linha em {@link #ALL}. */
public final class Rudiments {

    private Rudiments() {
    }

    public static final List<Rudiment> ALL = List.of(
            // --- Rulos ---
            Rudiment.of("Toque simples", ROLL, 4, "R L R L"),
            Rudiment.of("Toque duplo", ROLL, 4, "R R L L"),
            Rudiment.of("Rulo de 5", ROLL, 8, "R R L L >R - - -  L L R R >L - - -"),
            Rudiment.of("Rulo de 9", ROLL, 8,
                    "R R L L R R L L >R - - - - - - -  L L R R L L R R >L - - - - - - -"),

            // --- Diddles ---
            Rudiment.of("Paradiddle simples", DIDDLE, 4, ">R L R R >L R L L"),
            Rudiment.of("Paradiddle duplo", DIDDLE, 3, ">R L R L R R >L R L R L L"),
            Rudiment.of("Paradiddle triplo", DIDDLE, 4, ">R L R L R L R R >L R L R L R L L"),
            Rudiment.of("Paradiddle-diddle", DIDDLE, 3, ">R L R R L L"),

            // --- Flams ---
            Rudiment.of("Flam", FLAM, 1, "fR fL"),
            Rudiment.of("Flam tap", FLAM, 4, "f>R R f>L L"),
            Rudiment.of("Flam accent", FLAM, 3, "f>R L R f>L R L"),

            // --- Drags ---
            Rudiment.of("Drag", DRAG, 1, "dR dL"),
            Rudiment.of("Single drag tap", DRAG, 2, "dR >L dL >R"));

    public static Optional<Rudiment> byName(String name) {
        return ALL.stream().filter(r -> r.name().equals(name)).findFirst();
    }
}
