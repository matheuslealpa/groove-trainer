# Groove Trainer

Metrônomo e treinador de tempo para estudo de bateria, feito em Java + JavaFX.

## Recursos (v1)

- BPM de 30 a 300 (slider, botões ±1/±5 ou teclado)
- Compassos 2/4, 3/4, 4/4, 5/4, 6/8, 7/8, 9/8 e 12/8
- Acento no tempo 1 (opcional)
- Tap tempo
- Indicadores visuais sincronizados com o som
- Controle de volume

### Atalhos

| Tecla        | Ação               |
|--------------|--------------------|
| Espaço       | Tocar / parar      |
| T            | Tap tempo          |
| ↑ / ↓        | ±1 BPM             |
| Shift + ↑/↓  | ±5 BPM             |

## Rodando

Requer JDK 25 e Maven.

```bash
mvn javafx:run
```

Testes:

```bash
mvn test
```

## Como o tempo fica preciso

O metrônomo **não** usa `Thread.sleep()`, que atrasa de forma irregular. O `ClickRenderer`
gera o áudio amostra por amostra e coloca cada clique na amostra exata do tempo
(a 44,1 kHz, isso é precisão de ~0,02 ms). O `MetronomeEngine` manda esse fluxo contínuo
para a placa de som por um `SourceDataLine`.

A interface pisca os indicadores lendo a posição real de reprodução da placa
(`getLongFramePosition()`), então a luz acende quando o clique sai na caixa de som, e não
quando ele é gerado.

```
src/main/java/io/github/matheuslealpa/groovetrainer/
├── audio/
│   ├── ClickRenderer.java    # gera o áudio e agenda os tempos (lógica pura, testável)
│   ├── ClickSound.java       # sons de clique sintetizados
│   ├── MetronomeEngine.java  # thread de áudio + SourceDataLine
│   └── TapTempo.java
└── ui/
    ├── MainView.java
    ├── TimeSignature.java
    └── TempoName.java
```

## Próximos passos

- [ ] Subdivisões (colcheia, tercina, semicolcheia, sextina) com volume próprio
- [ ] Speed trainer (sobe X BPM a cada N compassos)
- [ ] Gap click (toca N compassos, silencia M)
- [ ] Swing / shuffle
- [ ] Polirritmias
- [ ] Rotinas de estudo e histórico de prática
