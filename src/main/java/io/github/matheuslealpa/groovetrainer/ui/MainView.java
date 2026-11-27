package io.github.matheuslealpa.groovetrainer.ui;

import io.github.matheuslealpa.groovetrainer.audio.ClickRenderer;
import io.github.matheuslealpa.groovetrainer.audio.MetronomeEngine;
import io.github.matheuslealpa.groovetrainer.audio.TapTempo;
import io.github.matheuslealpa.groovetrainer.rudiment.Rudiment;
import io.github.matheuslealpa.groovetrainer.rudiment.Rudiments;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import javax.sound.sampled.LineUnavailableException;
import java.util.ArrayList;
import java.util.List;

/** Tela principal: BPM, compasso, acento, volume, tap tempo, indicadores de tempo e rudimentos. */
public final class MainView {

    static final int MIN_BPM = 30;
    static final int MAX_BPM = 300;
    private static final String NO_RUDIMENT = "Nenhum";

    private final MetronomeEngine engine;
    private final TapTempo tapTempo = new TapTempo();

    private final VBox root = new VBox(20);
    private final Label bpmLabel = new Label();
    private final Label tempoNameLabel = new Label();
    private final Label statusLabel = new Label();
    private final Slider bpmSlider = new Slider(MIN_BPM, MAX_BPM, 100);
    private final Button playButton = new Button("▶  Tocar");
    private final HBox beatDots = new HBox(14);
    private final List<Circle> dots = new ArrayList<>();
    private final RudimentStrip rudimentStrip = new RudimentStrip();

    private boolean updatingBpm;

    public MainView(MetronomeEngine engine) {
        this.engine = engine;
        build();
        setBpm(engine.getBpm());
        rebuildDots(4);
        startBeatAnimation();
    }

    public Parent getRoot() {
        return root;
    }

    /** Atalhos: Espaço toca/para, T faz tap tempo, ↑/↓ ajusta 1 BPM (com Shift, 5). */
    public void installShortcuts(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            int step = e.isShiftDown() ? 5 : 1;
            switch (e.getCode()) {
                case SPACE -> togglePlay();
                case T -> tap();
                case UP, RIGHT -> setBpm(engine.getBpm() + step);
                case DOWN, LEFT -> setBpm(engine.getBpm() - step);
                default -> {
                    return;
                }
            }
            e.consume();
        });
    }

    private void build() {
        root.getStyleClass().add("root-pane");
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(28));

        // --- BPM ---
        bpmLabel.getStyleClass().add("bpm-value");
        Label bpmUnit = new Label("BPM");
        bpmUnit.getStyleClass().add("bpm-unit");
        tempoNameLabel.getStyleClass().add("tempo-name");

        Button minus5 = smallButton("−5", () -> setBpm(engine.getBpm() - 5));
        Button minus1 = smallButton("−1", () -> setBpm(engine.getBpm() - 1));
        Button plus1 = smallButton("+1", () -> setBpm(engine.getBpm() + 1));
        Button plus5 = smallButton("+5", () -> setBpm(engine.getBpm() + 5));

        VBox bpmBox = new VBox(-6, bpmLabel, bpmUnit);
        bpmBox.setAlignment(Pos.CENTER);
        bpmBox.setMinWidth(170);
        HBox bpmRow = new HBox(10, minus5, minus1, bpmBox, plus1, plus5);
        bpmRow.setAlignment(Pos.CENTER);

        bpmSlider.setFocusTraversable(false);
        bpmSlider.valueProperty().addListener((obs, old, val) -> {
            if (!updatingBpm) {
                setBpm(Math.round(val.doubleValue()));
            }
        });

        // --- Indicadores de tempo ---
        beatDots.setAlignment(Pos.CENTER);
        beatDots.setMinHeight(40);

        // --- Controles ---
        ComboBox<TimeSignature> signature = new ComboBox<>();
        signature.getItems().addAll(TimeSignature.PRESETS);
        signature.setValue(TimeSignature.PRESETS.get(2)); // 4/4
        signature.setFocusTraversable(false);
        signature.valueProperty().addListener((obs, old, ts) -> {
            engine.setBeatsPerBar(ts.beats());
            rebuildDots(ts.beats());
        });

        CheckBox accent = new CheckBox("Acentuar o 1");
        accent.setSelected(true);
        accent.setFocusTraversable(false);
        accent.selectedProperty().addListener((obs, old, on) -> {
            engine.setAccentEnabled(on);
            rebuildDots(dots.size());
        });

        Slider volume = new Slider(0, 1, 0.8);
        volume.setPrefWidth(120);
        volume.setFocusTraversable(false);
        volume.valueProperty().addListener((obs, old, v) -> engine.setVolume(v.floatValue()));

        HBox controls = new HBox(18,
                labeled("Compasso", signature),
                labeled("Volume", volume),
                accent);
        controls.setAlignment(Pos.CENTER);

        // --- Rudimento ---
        ComboBox<String> rudiment = new ComboBox<>();
        rudiment.getItems().add(NO_RUDIMENT);
        Rudiments.ALL.forEach(r -> rudiment.getItems().add(r.name()));
        rudiment.setValue(NO_RUDIMENT);
        rudiment.setFocusTraversable(false);
        rudiment.valueProperty().addListener((obs, old, name) -> {
            Rudiment chosen = Rudiments.byName(name).orElse(null);
            engine.setRudiment(chosen);
            rudimentStrip.show(chosen);
        });

        Slider rudimentVolume = new Slider(0, 1, 0.8);
        rudimentVolume.setPrefWidth(120);
        rudimentVolume.setFocusTraversable(false);
        rudimentVolume.valueProperty().addListener((obs, old, v) -> engine.setRudimentVolume(v.floatValue()));

        HBox rudimentControls = new HBox(18,
                labeled("Rudimento", rudiment),
                labeled("Volume do rudimento", rudimentVolume));
        rudimentControls.setAlignment(Pos.CENTER);

        // --- Tocar / Tap ---
        playButton.getStyleClass().add("play-button");
        playButton.setFocusTraversable(false);
        playButton.setOnAction(e -> togglePlay());

        Button tapButton = new Button("Tap");
        tapButton.getStyleClass().add("tap-button");
        tapButton.setFocusTraversable(false);
        tapButton.setOnAction(e -> tap());

        HBox actions = new HBox(12, playButton, tapButton);
        actions.setAlignment(Pos.CENTER);

        Label hint = new Label("Espaço: tocar/parar  ·  T: tap  ·  ↑/↓: ±1 BPM (Shift: ±5)");
        hint.getStyleClass().add("hint");
        statusLabel.getStyleClass().add("status");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        root.getChildren().addAll(bpmRow, tempoNameLabel, bpmSlider, beatDots, controls,
                rudimentControls, rudimentStrip.getNode(), actions, statusLabel, spacer, hint);
    }

    private void setBpm(double value) {
        int bpm = (int) Math.clamp(Math.round(value), MIN_BPM, MAX_BPM);
        engine.setBpm(bpm);
        bpmLabel.setText(Integer.toString(bpm));
        tempoNameLabel.setText(TempoName.of(bpm));
        updatingBpm = true;
        bpmSlider.setValue(bpm);
        updatingBpm = false;
    }

    private void tap() {
        tapTempo.tap(System.nanoTime()).ifPresent(this::setBpm);
    }

    private void togglePlay() {
        if (engine.isRunning()) {
            engine.stop();
            playButton.setText("▶  Tocar");
            playButton.getStyleClass().remove("playing");
            dots.forEach(d -> d.setOpacity(0.25));
            rudimentStrip.dim();
            return;
        }
        try {
            engine.start();
            playButton.setText("■  Parar");
            playButton.getStyleClass().add("playing");
            statusLabel.setText("");
        } catch (LineUnavailableException | IllegalArgumentException e) {
            statusLabel.setText("Não consegui abrir a saída de áudio: " + e.getMessage());
        }
    }

    private void rebuildDots(int beats) {
        dots.clear();
        beatDots.getChildren().clear();
        for (int i = 0; i < beats; i++) {
            Circle c = new Circle(beats > 9 ? 11 : 15);
            c.getStyleClass().add("beat-dot");
            if (i == 0 && beats > 1 && engine.isAccentEnabled()) {
                c.getStyleClass().add("accent");
            }
            c.setOpacity(0.25);
            dots.add(c);
        }
        beatDots.getChildren().addAll(dots);
    }

    private void startBeatAnimation() {
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                engine.pollPlayedBeats(MainView.this::flash);
                engine.pollPlayedStrokes(rudimentStrip::highlight);
            }
        }.start();
    }

    private void flash(ClickRenderer.Beat beat) {
        if (beat.index() >= dots.size()) {
            return;
        }
        Circle dot = dots.get(beat.index());
        FadeTransition fade = new FadeTransition(Duration.millis(Math.min(250, 60_000 / engine.getBpm())), dot);
        fade.setFromValue(1.0);
        fade.setToValue(0.25);
        fade.play();
    }

    private static Button smallButton(String text, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("step-button");
        b.setFocusTraversable(false);
        b.setOnAction(e -> action.run());
        return b;
    }

    private static VBox labeled(String title, javafx.scene.Node node) {
        Label l = new Label(title);
        l.getStyleClass().add("field-label");
        VBox box = new VBox(4, l, node);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }
}
