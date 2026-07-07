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
| Tests | ✅ generator + feedback/score |
| **MVP eligible** | ✅ (jugable, medible, persistido, testeado, feedback, versionado) — score sigue PRELIMINAR (gate físico §15 abierto, como todo el bloque) |

## Inventario de Velocidad Mental
- **Implementados y jugables**: 5 → E1 Destello, **E2 Comparación (nuevo)**, E3 Cifrado, E6 Multivía, E7 Compuerta.
- **Score-ready** (por el motor 0–1 preliminar): 5.
- **MVP-ready** (jugable+medible+persistido+testeado+feedback+versionado, score PRELIMINAR): 5.
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
