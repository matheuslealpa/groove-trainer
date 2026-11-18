package io.github.matheuslealpa.groovetrainer;

/**
 * Ponto de entrada que não estende {@code Application}, para o app rodar a partir de
 * um JAR com as dependências no classpath.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        GrooveTrainerApp.main(args);
    }
}
