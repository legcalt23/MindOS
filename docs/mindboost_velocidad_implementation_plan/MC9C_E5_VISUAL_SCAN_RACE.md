# MC9-C — Velocidad Mental: E5 · Búsqueda Visual (Visual Scan Race)

7º ejercicio del bloque. Detección de un objetivo entre distractores bajo carga visual y presión temporal: dos
paneles, EXACTAMENTE uno contiene el objetivo, el usuario toca ese lado rápido y sin fallar. Amplía la cobertura
cognitiva (velocidad de búsqueda visual, atención selectiva, discriminación target/distractor, procesamiento bajo
carga) sin duplicar mecánicas. Reutiliza TODO el pipeline de 2 vías (métricas, quality, staircase-deadline, score
0–1 preliminar, persistencia cifrada, write-ahead) — **sin infra nueva, sin score 0–100, AI Coach no tocado**.

## Reglas ejecutivas respetadas
- Score **0–1 preliminar** intacto (`SpeedScoreEngine`, `preliminary=true`); NO 0–100, NO `officialScore`, NO rank.
- No alimenta profile/global de forma autoritativa: PROCESSING_SPEED sigue CALIBRATING (gate de timebase
  autoritativo + `SpeedAntiGaming.COVERAGE=PARTIAL`). E5 añadido a `CHOICE_EXERCISE_CODES` (gate de azar 0.5).
- AI Coach: **no tocado**. Sin LLM/billing/cloud/Firestore/analytics global.

## Mecánica
Cue `"Busca ★"` (qué buscar, nunca el lado). Panel izquierdo y derecho, cada uno con `itemsPerSide` símbolos;
uno de los dos paneles contiene el objetivo. Respuesta = tocar el lado del objetivo. "La velocidad sin precisión
no se premia": una respuesta fuera del `deadline` cuenta como incorrecta (bucle de 2 vías) y el motor de score
preliminar penaliza la baja precisión. **Onset-gated** (`revealLabelsOnlyAtStimulus=true`): cue+paneles aparecen
SOLO desde la fase Stimulus (no en el foreperiodo), y el TR se mide desde ese onset.

## Diseño cognitivo
Entrena: velocidad de búsqueda visual, atención selectiva, discriminación target/distractor, procesamiento
perceptivo bajo carga, control velocidad-precisión, estabilidad bajo densidad visual, resistencia a respuestas
rápidas incorrectas. **NO afirma** entrenar inteligencia general, visión clínica, salud ocular, ni transferencia
garantizada; NO compara con población normativa (todo enforced por el copy scanner + `SpeedStringsHonestyTest`).

Riesgos de medición mitigados: respuesta pre-onset → onset-gating + flags de anticipación; cue delata el lado →
el cue solo nombra el objetivo; distractor == objetivo → invariante de pools disjuntos (test); objetivo ausente
o en ambos lados → estructural + test (`hits == 1`); diferencia demasiado obvia → familias de similitud; azar en
2 vías → E5 en el gate anti-gaming chance-0.5.

## Generador (`VisualScanRaceGenerator`, puro y determinista)
`VisualScanConfig(itemsPerSide, targetPool, distractorPool, minItemsPerSide, maxItemsPerSide,
allowRepeatedDistractors, similarity, separator)`. Invariante de integridad: `targetPool ∩ distractorPool = ∅`
(un distractor nunca puede ser el objetivo → "exactamente un objetivo" es estructural, no por azar). `trial(rng)`
→ objetivo del pool, lado del objetivo aleatorio, panel objetivo (1 objetivo + `itemsPerSide-1` distractores) +
panel llano (`itemsPerSide` distractores); distractores con repetición (uniforme) o distintos (Fisher-Yates
parcial determinista). `next(rng)` → `SpeedTrialPrompt(cue="Busca <t>", leftLabel, rightLabel, correctChoice,
revealLabelsOnlyAtStimulus=true)`. Es un generador SIN estado (reset() = no-op heredado).

## Dificultad
- **Real en V1**: `itemsPerSide` (3–6; más elementos = más carga), similitud target/distractor vía
  `VisualScanConfig.forSimilarity(LOW|MEDIUM|HIGH)` (tres familias de glyphs seguros cross-platform), presión
  temporal (deadline del staircase, palanca adaptativa en vivo, reutilizada).
- **Preparada** (config lista, escalado futuro): variabilidad del target (targetPool >1), densidad, sin
  distractores repetidos, longitud del panel. Sin targets ausentes ni multi-target en V1.

## Métricas + mapping honesto
| Métrica deseada E5 | Existente usada | Estado |
|---|---|---|
| totalTrials · accuracy · omissions | `SessionMetrics.totalTrials`/`pc`/`omissionRate` | reused |
| medianRT · averageRT · rtVariability | `SessionMetrics.rtMedianNanos`/`rtMeanNanos`/`cv` | reused |
| anticipationRate · falseFast/impulsivity | `SessionMetrics.anticipationRate`/`impIdx` | reused |
| preliminaryScore01 | `CognitiveProfileScore.total` (0–1, `preliminary=true`) | reused |
| anomalyFlags | `SessionQuality` (LOW/CONTAMINATED excluidos) | reused |
| exerciseVersion / scoringVersion | `SpeedExerciseId.E5_BUSQUEDA.code`+`VersionSnapshot` / `SpeedVersions.SCORE_VERSION` | reused |
| visualLoadLevel · itemsPerSide · distractorDensity · targetSide | `VisualScanConfig`/`VisualScanTrial` (generación) | config-derived |
| responsesPerMinute | — | pending (derivable; not-needed V1) |
| coste por NIVEL de carga visual (per-load) | — | **pending** (itemsPerSide NO se persiste por trial; fijo por sesión en V1) |

Nota honesta: el `itemsPerSide`/`targetSide` NO se persisten en `TrialRecord` (fijos por sesión en V1), así que
un análisis cruzado accuracy/RT ↔ carga (como el `RuleSwitchAnalyzer` de E4) NO es derivable. Por eso el feedback
`HIGH_VISUAL_LOAD_COST` es una lectura POR-CORRIDA (techo §9 de precisión/estabilidad), NO una medida validada por
nivel de carga. El score autoritativo sigue siendo el 0–1 preliminar; el análisis alimenta SOLO el feedback.

## Scoring preliminar
`SpeedScoreEngine` 0–1 (velocidad útil / control = (1−ImpIdx)·PC / estabilidad = (1−CV)·(1−lapsos); techo ×0.5 si
pc<0.80 ∨ cv>0.30). Penaliza: baja precisión, respuestas rápidas incorrectas, anticipaciones, omisiones, alta
variabilidad, rendimiento compatible con azar (gate anti-gaming). Premia: precisión alta, reacción rápida
controlada, estabilidad. No se inventó ninguna métrica de carga visual artificial.

## Feedback (`VisualScanFeedback` → `SpeedStrings.VS_FEEDBACK_*`)
6 categorías, cada una mapeada a una señal REAL del score (macro-categoría + techo), gated a E5 en la pantalla de
fin: CALIBRATING · GOOD_VISUAL_CONTROL · FAST_LOW_PRECISION · HIGH_VISUAL_LOAD_COST · UNSTABLE_SEARCH · BALANCED.
Copy serio, menciona búsqueda/detección visual + precisión/estabilidad; sin IQ/arcade/humillación/claims oficiales
(enforced por copy scanner + `VisualScanCopyTest` + `SpeedStringsHonestyTest`).

## Integración · persistencia · anti-gaming · profile
- Menú/pantalla: `SpeedExerciseId.E5_BUSQUEDA`, `E5BusquedaVisualScreen` (fina, delega en `SpeedExerciseScreen`),
  card en `SpeedRootScreen`, dispatch de generador/título/instrucciones/feedback + rama `TwoChoiceSurface`.
- Persistencia: pipeline existente (`MeasurementRun`→`store.saveRun`); id por corrida (MC9-B-R) → repetir no
  sobrescribe; abandoned/low/contaminated no calibran (reglas existentes).
- Anti-gaming: E5 ∈ `CHOICE_EXERCISE_CODES` → gate de azar 0.5; test de partial-fast-guessing E5 excluido +
  honest E5 mantiene elegibilidad.
- Profile: PROCESSING_SPEED sigue CALIBRATING (COVERAGE PARTIAL → `contribution()` null).

## Quality gate
| Gate | Estado |
|---|---|
| Cognitive objective | DONE |
| Playable mechanic | DONE |
| Pure generator | DONE |
| Difficulty config | DONE |
| Metrics mapping | PARTIAL (básicas reused; carga-por-trial pending) |
| Feedback | DONE |
| Preliminary scoring | DONE |
| Persistence | DONE |
| Anti-gaming | DONE |
| Copy safety | DONE |
| Tests | DONE |
| Menu/screen integration | DONE |
| Analytics | DEFERRED |
| Physical gate §15 | OPEN |
| MVP-candidate | YES |
| MVP-ready completo | NO |

## Inventario del bloque tras MC9-C
| Ejercicio | Subhabilidad principal | Score-ready | MVP-candidate | MVP-ready completo |
|---|---|---|---|---|
| E1 · Destello | tiempo de reacción simple | ✅ | ✅ | ❌ |
| E2 · Comparación | comparación de magnitudes | ✅ | ✅ | ❌ |
| E3 · Cifrado | verificación símbolo↔valor | ✅ | ✅ | ❌ |
| E4 · Cambio de Regla | flexibilidad / task-switching | ✅ | ✅ | ❌ |
| **E5 · Búsqueda Visual (nuevo)** | búsqueda visual bajo carga | ✅ | ✅ | ❌ |
| E6 · Multivía | elección de 2 alternativas | ✅ | ✅ | ❌ |
| E7 · Compuerta | regla condicional / inhibición | ✅ | ✅ | ❌ |

- **Diseñados/implementados/jugables/score-ready/MVP-candidate**: 7. **MVP-ready completo**: **0** (analytics
  `mental_speed_*` DIFERIDO + gate físico §15 abierto). **Faltan ~8** hasta ~15 (batería Post-MVP de 16).

## Tests
`VisualScanRaceGeneratorTest` (un solo objetivo en el lado correcto, pools disjuntos, cue sin lado, balance,
determinismo, config inválida, carga por itemsPerSide, familias de similitud, distractores distintos,
multi-target), `VisualScanFeedbackTest` (6 categorías alcanzables + techo→HIGH_VISUAL_LOAD_COST),
`VisualScanCopyTest` (vocabulario visual+precisión, sin arcade/humillación/normativo), `TwoChoiceLabelRevealTest`
(E5 onset-gated + E4/E2 intactos), `SpeedPromptGeneratorsTest` (E5 reset no-op), `SpeedProfileSourceTest` (E5
two-way anti-gaming: gamed excluido / honesto mantenido). Reutilizado explícitamente (sin duplicar): 0–1
preliminar + techo (`SpeedScoreEngineTest`), abandoned/anomalous/low-confidence/coverage-partial
(`SpeedProfileSourceTest`), no-labels-internos (`SpeedStringsHonestyTest`), id por corrida + E4 trial-0/onset
(`SpeedSessionStateHolderTest`/`TwoChoiceLabelRevealTest`).

## Gates
`compileKotlinDesktop` ✅ · `desktopTest` **3060/0/0** ✅ · `compileAndroidMain` ✅ · `detekt` (solo el foreign
untracked `IndexedSecureKeyValueStore:61`) ✅ · copy scanners ×3 ✅ · `scan:secrets` ✅.
**Android runtime no probado** (sin instrumented test) · **iOS no probado** (sin Mac) · **Firestore no probado**
(diferido) · score **PRELIMINAR** (§15 abierto).

## Readiness
E5 generator READY · E5 playable READY · E5 score-ready preliminary READY · E5 MVP-candidate YES · E5 MVP-ready
completo NO · Mental Speed 7-exercise library READY (jugable/score-ready) · Android internal technical build GO ·
Android user beta NO-GO · Android RC NO-GO · PRD MVP NO-GO.
