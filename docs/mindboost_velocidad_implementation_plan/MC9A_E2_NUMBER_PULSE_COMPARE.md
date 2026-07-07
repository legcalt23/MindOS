# MC9-A — Velocidad Mental: E2 · Comparación (Number Pulse Compare)

5º ejercicio del bloque Velocidad Mental. Comparación rápida de magnitudes: aparecen dos números y el usuario
toca el lado del MAYOR bajo presión temporal. **Reutiliza toda la infraestructura MVP-0→3** (bucle de 2 vías,
métricas, quality gate, staircase, score preliminar 0–1, persistencia cifrada, write-ahead/recovery) — **sin
infraestructura nueva y sin tocar el scoring**.

## Reglas ejecutivas respetadas
- Scoring **0–1 preliminario** intacto (`SpeedScoreEngine`, `preliminary=true`); NO se añadió un score 0–100
  ni `candidateScore100` (omitido a propósito — era opcional "si existe").
- NO alimenta profile/global/ranking con score autoritativo: `SpeedProfileSource` sigue gateando por timebase
  autoritativo (Choreographer + nanos, Android API≥34) y anti-gaming → PROCESSING_SPEED permanece CALIBRATING.
  E2 se añadió a `CHOICE_EXERCISE_CODES` para que el gate de azar (chance 0.5) lo trate como tarea de elección.
- AI Coach: **no tocado**.

## Mecánica
2-way choice sobre `SpeedTrialPrompt`. `NumberPulseCompareGenerator` produce dos magnitudes distintas con
`correctChoice` = el lado mayor. Los números se colocan SOLO en la señal (`cue`, que se pinta en la fase
Stimulus), no en las etiquetas persistentes, para que el TR mida discriminación+decisión desde el onset y no
una respuesta pre-decidida durante el foreperiodo. Determinista (`XorShiftSpeedRng`).

## Métricas / dificultad / score
- **Métricas**: las de `SessionMetrics`/`BlockMetrics` reutilizadas (pc, rtMedian/mean, cv, lapse, anticipation,
  FA, omission, impIdx, fatIdx, vce, versiones, timebase). Nulos = "no computable", nunca 0.
- **Dificultad**: intrínseca por `NumberPulseConfig` (cercanía `minGap/maxGap`, rango `minValue/maxValue`); la
  palanca ADAPTATIVA en vivo es el `deadline` del `StaircaseEngine` (igual que E3/E6/E7). Respuesta fuera de
  deadline = incorrecta → "la velocidad sin precisión no se premia".
- **Score**: `SpeedScoreEngine` (0–1). Precisión < 0.80 o CV > 0.30 penalizan el total ×0.5; macro-categoría
  `FAST_IMPULSIVE` → feedback "velocidad alta, precisión baja".

## Feedback (§8)
`NumberPulseFeedback` (puro) mapea la macro-categoría → una lente seria de "velocidad con precisión";
`SpeedStrings.FEEDBACK_*` es la copy (todas mencionan velocidad Y precisión; sin claims de IQ/salud, sin tono
arcade/infantil). Se muestra en la pantalla de fin SOLO para E2 (los demás ejercicios quedan idénticos).

## Quality gate (§13)
| Gate | E2 |
|---|---|
| Objetivo cognitivo | ✅ comparación de magnitudes / velocidad con precisión |
| Métrica primaria | ✅ (reutilizadas) |
| Escalado de dificultad | ✅ deadline (staircase) + cercanía (config) |
| Fórmula de score | ✅ `SpeedScoreEngine` 0–1 preliminar |
| Regla de calibración | ✅ reutiliza el gate de `SpeedProfileSource` (5 sesiones válidas, timebase autoritativo) |
| Feedback | ✅ `NumberPulseFeedback` + copy |
| Analytics | ⏸ DEFERRED — no existe surface `mental_speed_*` reusable; construirla es integración (fuera de foco por regla 8) |
| Persistencia | ✅ `SecureSpeedMeasurementStore` (misma ruta, sessionId único, sin duplicados) |
| Anti-gaming | ✅ `SpeedAntiGaming` vía `CHOICE_EXERCISE_CODES` |
| Copy safety | ✅ scanner verde |
| Tests | ✅ generator + feedback/score + reveal-predicate + copy honesty |
| **MVP-candidate** | ✅ (jugable, medible, persistido, testeado, feedback, versionado) — score PRELIMINAR |
| **MVP-ready completo** | ❌ NO — el quality gate exige analytics, y `mental_speed_*` sigue DIFERIDO (+ release parity). No se declara MVP-ready mientras esté abierto |

## Inventario de Velocidad Mental (estados honestos)
- **Implementados y jugables**: 5 → E1 Destello, **E2 Comparación (nuevo)**, E3 Cifrado, E6 Multivía, E7 Compuerta.
- **Score-ready preliminar** (motor 0–1): 5.
- **MVP-candidate** (jugable+medible+persistido+testeado+feedback+versionado, score PRELIMINAR): 5.
- **MVP-ready completo**: **0** — ningún ejercicio del bloque cierra analytics/release-parity todavía; no se
  declara "MVP-ready completo" mientras el quality gate exija analytics (`mental_speed_*` DIFERIDO) y el gate
  físico §15 siga abierto.
- **Faltan hasta ~15**: ~10 ejercicios de diseño (batería Post-MVP de 16). Pendiente por ejercicio: mecánica,
  copy, tests; y transversal: el gate físico §15 (hardware) para retirar `preliminary`, y una surface de
  analytics `mental_speed_*` (integración, diferida).

## Tests
`NumberPulseCompareGeneratorTest` (validez/distinción/rango/gap, lado correcto = mayor, onset-gating, determinismo,
config, guards) · `NumberPulseFeedbackTest` (score 0–1 + preliminary, "velocidad sin precisión no premiada",
control > velocidad-imprudente, mapping completo, versiones, copy menciona velocidad+precisión, copy sin claims
prohibidos vía `ForbiddenLexicon`). Tests de omisión/dificultad-staircase/anomalía-RT/abandoned/persistencia-dedup
quedan cubiertos por las suites REUTILIZADAS (MetricCalculator/Staircase/SessionQuality/SpeedMeasurementStore/
SpeedProfileSource), no se duplican.

## Gates
`compileKotlinDesktop` ✅ · `desktopTest` 3007/0/0 ✅ · `compileAndroidMain` ✅ · `detekt` (solo el foreign
`IndexedSecureKeyValueStore`) ✅ · copy scanners ×3 ✅ · `scan:secrets` ✅. **Android runtime no probado** (sin
instrumented test) · **iOS no probado** (sin Mac) · score **PRELIMINAR** (gate físico §15 abierto).

---

# MC9-A-R — Honesty + UX hardening (micro-remediación)

Sin infraestructura nueva, sin AI Coach, sin score 0–100, sin analytics global.

## §1 Etiquetado honesto
E2 se re-clasifica de "MVP-ready" a **MVP-candidate** (ver tablas arriba). "MVP-ready completo" queda
reservado para cuando analytics/release-parity estén cerrados; hoy = **0** en el bloque.

## §2 Copy de UI sin lenguaje interno de fase
`SpeedStrings` ya no expone labels internos en texto visible:
- `FEATURE_TITLE`: "Velocidad Mental · Medición (MVP-0)" → **"Velocidad Mental"**.
- `MVP_NOTE`: → **"Entrenamiento preliminar. Resultados orientativos hasta completar la validación técnica; la
  velocidad se interpreta junto con precisión y estabilidad."**
Se conserva el marco PRELIMINAR/orientativo y no aparecen claims de mejora garantizada, IQ, ranking, score
oficial ni comparación normativa. Enforced por `SpeedStringsHonestyTest` sobre `SpeedStrings.VISIBLE_COPY`.

## §3 Decisión sobre la UI de números → **Opción preferida (números en los paneles, revelados al onset)**
Antes: los números vivían en un `cue` centrado con espacios (`◀ 3     5 ▶`), frágil por fuente/ancho. Ahora las
dos magnitudes **son las etiquetas de los paneles izquierdo/derecho** (alineadas con el área táctil real) y la
señal lleva solo la regla ("¿Cuál es mayor?"). Para no filtrar la respuesta antes del estímulo se añadió un
campo ADITIVO `SpeedTrialPrompt.revealLabelsOnlyAtStimulus` (default `false` ⇒ **E3/E6/E7 sin cambio alguno**);
E2 lo pone `true` y la pantalla revela las etiquetas SOLO desde la fase Stimulus, vía el predicado puro
`shouldRevealTwoChoiceLabels` (unit-testeado en `TwoChoiceLabelRevealTest`). El RT sigue midiéndose desde el
onset del estímulo. No se refactorizó el motor.

## §4 Matriz de métricas de E2 (ninguna inventada)
| Métrica deseada (spec) | Métrica existente usada | Estado |
|---|---|---|
| totalTrials | `SessionMetrics.totalTrials` | reused |
| correct / incorrect | `pc` × trials · `TrialRecord.isCorrect` | derived (reused) |
| omissions | `omissionRate` (conteo = `×trials`) | reused / derived |
| accuracy | `pc` | reused |
| median RT | `rtMedianNanos` | reused |
| variability | `cv` | reused |
| responses per minute | `totalTrials / durationMs` | derived-pending (no surfaced aún) |
| impulsivity / fast errors | `impIdx` + `anticipationRate` | reused |
| speed-accuracy index | `vce` (VCE) + barras `usefulSpeed`/`control` del score | reused |
| preliminaryScore01 | `CognitiveProfileScore.total` (0–1, `preliminary=true`) | reused |
| anomaly flags | `QualityDecisionLog.reasons` + `SessionQuality` | reused |
| exerciseVersion | identidad `SpeedExerciseId.E2_COMPARA.code` + `VersionSnapshot` | reused (no hay campo "exerciseVersion" separado — not-needed) |
| scoringVersion | `SpeedVersions.SCORE_VERSION` / `CognitiveProfileScore.scoreVersion` | reused |
| candidateScore100 | — | not-needed (deliberadamente omitido; no existe score 0–100) |

## §5 Invariantes de scoring (tests)
`the_preliminary_score_never_becomes_a_0_100_official_value` (total ∈ [0,1], `preliminary=true` en un barrido de
extremos), `speed_without_precision_is_not_rewarded`, mapping fast-incorrect→`FAST_LOW_PRECISION`. E2 está en el
`CHOICE_EXERCISE_CODES` privado de `SpeedProfileSource` (gate de azar), y PROCESSING_SPEED sigue CALIBRATING por
el gate de timebase autoritativo (no alimenta profile/rank/global de forma autoritativa; cubierto por
`SpeedProfileSourceTest`). Score preliminar = única fuente de verdad; no hay 0–100 oficial.

## Tests MC9-A-R
`SpeedStringsHonestyTest` (§2, 3 tests), `TwoChoiceLabelRevealTest` (§3, 3 tests), `NumberPulseFeedbackTest`
(+`the_preliminary_score_never_becomes_a_0_100_official_value`), `NumberPulseCompareGeneratorTest`
(onset test reescrito para números-en-paneles + regla-en-cue + flag).
