package io.github.matheuslealpa.groovetrainer.ui;

/** Nome italiano do andamento, só como referência visual. */
final class TempoName {

    private TempoName() {
    }

    static String of(int bpm) {
        if (bpm < 40) return "Grave";
        if (bpm < 60) return "Largo";
        if (bpm < 66) return "Larghetto";
        if (bpm < 76) return "Adagio";
        if (bpm < 108) return "Andante";
        if (bpm < 120) return "Moderato";
        if (bpm < 156) return "Allegro";
        if (bpm < 176) return "Vivace";
        if (bpm < 200) return "Presto";
        return "Prestissimo";
    }
}
