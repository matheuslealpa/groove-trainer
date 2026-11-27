package io.github.matheuslealpa.groovetrainer.ui;

import io.github.matheuslealpa.groovetrainer.audio.ClickRenderer;
import io.github.matheuslealpa.groovetrainer.rudiment.Rudiment;
import io.github.matheuslealpa.groovetrainer.rudiment.Stroke;
import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Faixa com a manulação do rudimento: uma célula por toque (R/L), com {@code >} nos acentos
 * e as apojaturas em letra pequena. Os toques são agrupados por tempo.
 */
final class RudimentStrip {

    private static final double IDLE_OPACITY = 0.45;

    private final FlowPane root = new FlowPane(16, 6);
    private final List<Node> cells = new ArrayList<>();
    private Rudiment rudiment;

    RudimentStrip() {
        root.setAlignment(Pos.CENTER);
        root.setMinHeight(52);
    }

    Node getNode() {
        return root;
    }

    /** Mostra a manulação de {@code rudiment}; {@code null} esvazia a faixa. */
    void show(Rudiment rudiment) {
        this.rudiment = rudiment;
        cells.clear();
        root.getChildren().clear();
        if (rudiment == null) {
            return;
        }
        HBox beat = null;
        int currentBeat = -1;
        for (Stroke s : rudiment.strokes()) {
            if ((int) s.beatPos() != currentBeat) {
                currentBeat = (int) s.beatPos();
                beat = new HBox(4);
                beat.setAlignment(Pos.BOTTOM_CENTER);
                root.getChildren().add(beat);
            }
            Node cell = cell(s);
            cells.add(cell);
            beat.getChildren().add(cell);
        }
    }

    /** Acende a célula do toque que acabou de soar. */
    void highlight(ClickRenderer.StrokeEvent event) {
        if (event.rudiment() != rudiment || event.strokeIndex() >= cells.size()) {
            return; // sobra de um rudimento que já foi trocado
        }
        FadeTransition fade = new FadeTransition(Duration.millis(220), cells.get(event.strokeIndex()));
        fade.setFromValue(1.0);
        fade.setToValue(IDLE_OPACITY);
        fade.play();
    }

    void dim() {
        cells.forEach(c -> c.setOpacity(IDLE_OPACITY));
    }

    private static Node cell(Stroke s) {
        Label accent = new Label(s.dynamic() == Stroke.Dynamic.ACCENT ? ">" : " ");
        accent.getStyleClass().add("stroke-accent");

        Label hand = new Label(s.hand().name());
        hand.getStyleClass().addAll("stroke-hand", s.hand() == Stroke.Hand.R ? "right" : "left");

        HBox note = new HBox(1, hand);
        note.setAlignment(Pos.BASELINE_CENTER);
        int graces = s.ornament().graceNotes();
        if (graces > 0) {
            Label grace = new Label(s.hand().other().name().toLowerCase().repeat(graces));
            grace.getStyleClass().add("stroke-grace");
            note.getChildren().addFirst(grace);
        }

        VBox cell = new VBox(-4, accent, note);
        cell.setAlignment(Pos.CENTER);
        cell.setOpacity(IDLE_OPACITY);
        return cell;
    }
}
