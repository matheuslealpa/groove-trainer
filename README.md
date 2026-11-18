# Groove Trainer

Metronome and timing trainer for drum practice, built with Java + JavaFX.

## Features (v1)

- 30 to 300 BPM (slider, ±1/±5 buttons or keyboard)
- 2/4, 3/4, 4/4, 5/4, 6/8, 7/8, 9/8 and 12/8 time signatures
- Optional accent on beat 1
- Tap tempo
- Beat indicators in sync with the sound
- Volume control

### Shortcuts

| Key          | Action             |
|--------------|--------------------|
| Space        | Play / stop        |
| T            | Tap tempo          |
| ↑ / ↓        | ±1 BPM             |
| Shift + ↑/↓  | ±5 BPM             |

## Installation

Java is bundled, so there is nothing else to install.

### Linux (recommended)

```bash
curl -fsSL https://raw.githubusercontent.com/matheuslealpa/groove-trainer/main/install.sh | bash
```

Installs into `~/.local` without sudo and adds a `groove-trainer` command and an applications
menu entry. Run the same command again to update. To uninstall:

```bash
curl -fsSL https://raw.githubusercontent.com/matheuslealpa/groove-trainer/main/install.sh | bash -s -- --uninstall
```

### Installers

Or download an installer from the [latest release](https://github.com/matheuslealpa/groove-trainer/releases/latest):

| System               | File     | How to install                                       |
|----------------------|----------|------------------------------------------------------|
| Ubuntu / Debian      | `.deb`   | `sudo apt install ./groove-trainer_*.deb`            |
| Fedora / openSUSE    | `.rpm`   | `sudo dnf install ./groove-trainer-*.rpm`            |
| Windows              | `.msi`   | Double-click it (no admin rights needed)             |

To update, install the new version over the old one.

## Development

Requires JDK 25 and Maven.

```bash
mvn javafx:run   # run the app
mvn test         # run the tests
```

### Building the native app

```bash
mvn -Ppackage package                       # runnable folder in target/dist/GrooveTrainer
mvn -Ppackage package -Djpackage.type=deb   # .deb (needs fakeroot)
mvn -Ppackage package -Djpackage.type=rpm   # .rpm (needs rpmbuild)
mvn -Ppackage package -Djpackage.type=msi   # .msi (Windows, needs WiX)
```

## How the timing stays accurate

The metronome does **not** use `Thread.sleep()`, which drifts and jitters. `ClickRenderer`
generates the audio sample by sample and places each click on the exact sample of its beat
(at 44.1 kHz that is ~0.02 ms precision). `MetronomeEngine` streams that audio continuously
to the sound card through a `SourceDataLine`.

The UI flashes the beat indicators by reading the sound card's actual playback position
(`getLongFramePosition()`), so the light turns on when the click comes out of the speaker,
not when it is generated.

```
src/main/java/io/github/matheuslealpa/groovetrainer/
├── audio/
│   ├── ClickRenderer.java    # generates audio and schedules beats (pure logic, testable)
│   ├── ClickSound.java       # synthesized click sounds
│   ├── MetronomeEngine.java  # audio thread + SourceDataLine
│   └── TapTempo.java
└── ui/
    ├── MainView.java
    ├── TimeSignature.java
    └── TempoName.java
```

## Roadmap

- [ ] Subdivisions (eighths, triplets, sixteenths, sextuplets) with their own volume
- [ ] Speed trainer (raise X BPM every N bars)
- [ ] Gap click (play N bars, mute M)
- [ ] Swing / shuffle
- [ ] Polyrhythms
- [ ] Practice routines and history
