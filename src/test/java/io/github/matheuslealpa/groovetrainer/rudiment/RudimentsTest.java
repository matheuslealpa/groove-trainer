package io.github.matheuslealpa.groovetrainer.rudiment;

import io.github.matheuslealpa.groovetrainer.rudiment.Rudiment.Family;
import io.github.matheuslealpa.groovetrainer.rudiment.Stroke.Dynamic;
import io.github.matheuslealpa.groovetrainer.rudiment.Stroke.Hand;
import io.github.matheuslealpa.groovetrainer.rudiment.Stroke.Ornament;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RudimentsTest {

    @Test
    void everyRudimentInTheCatalogIsWellFormed() {
        Set<String> names = new HashSet<>();
        for (Rudiment r : Rudiments.ALL) {
            assertTrue(names.add(r.name()), "nome repetido: " + r.name());
            assertTrue(r.lengthBeats() >= 1, r.name());
            double previous = -1;
            for (Stroke s : r.strokes()) {
                assertTrue(s.beatPos() > previous, r.name() + ": posições devem ser crescentes");
                assertTrue(s.beatPos() < r.lengthBeats(), r.name() + ": toque fora do padrão");
                previous = s.beatPos();
            }
        }
    }

    @Test
    void parsesStickingAccentsAndPositions() {
        Rudiment r = Rudiments.byName("Paradiddle simples").orElseThrow();

        assertEquals("RLRRLRLL", r.sticking());
        assertEquals(2, r.lengthBeats());
        assertEquals(Dynamic.ACCENT, r.strokes().get(0).dynamic());
        assertEquals(Dynamic.TAP, r.strokes().get(1).dynamic());
        assertEquals(0.25, r.strokes().get(1).beatPos());
        assertEquals(1.0, r.strokes().get(4).beatPos());
    }

    @Test
    void parsesOrnamentsAndSkipsRests() {
        Rudiment r = Rudiment.of("teste", Family.FLAM, 2, "f>R - dL -");

        assertEquals(2, r.strokes().size());
        Stroke flam = r.strokes().get(0);
        assertEquals(new Stroke(0.0, Hand.R, Dynamic.ACCENT, Ornament.FLAM), flam);
        assertEquals(new Stroke(1.0, Hand.L, Dynamic.TAP, Ornament.DRAG), r.strokes().get(1));
    }

    @Test
    void rejectsBadNotation() {
        assertThrows(IllegalArgumentException.class, () -> Rudiment.of("x", Family.ROLL, 4, "R L X L"));
        assertThrows(IllegalArgumentException.class, () -> Rudiment.of("x", Family.ROLL, 4, "R L R"));
        assertThrows(IllegalArgumentException.class, () -> Rudiment.of("x", Family.ROLL, 2, "- -"));
    }
}
