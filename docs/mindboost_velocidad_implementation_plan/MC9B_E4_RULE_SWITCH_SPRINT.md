# MC9-B — Velocidad Mental: E4 · Cambio de Regla (Rule Switch Sprint)

6º ejercicio del bloque. Task-switching color/forma: la regla activa alterna durante la corrida y el usuario
debe adaptarse rápido sin seguir aplicando la regla anterior. Amplía cobertura cognitiva (cognitive switching /
flexibilidad ejecutiva), **no duplica** mecánicas. Reutiliza todo el pipeline (bucle 2 vías, métricas, quality,
staircase, score 0–1 preliminar, persistencia cifrada, write-ahead) — **sin infra nueva, sin score 0–100**.

## Reglas ejecutivas respetadas
- Score **0–1 preliminar** intacto (`SpeedScoreEngine`, `preliminary=true`); NO 0–100, NO `officialScore`, NO rank.
- No alimenta profile/global de forma autoritativa: PROCESSING_SPEED sigue CALIBRATING (gate de timebase
  autoritativo). E4 añadido a `CHOICE_EXERCISE_CODES` (gate de azar lo trata como tarea de elección).
- AI Coach: **no tocado**. Sin LLM/billing/cloud/Firestore/analytics global.

## Mecánica
Estímulo de 2 dimensiones (color ∈ {azul, naranja} × forma ∈ {círculo, triángulo}). Regla activa = qué dimensión
decide: **COLOR** ⇒ azul=izquierda / naranja=derecha; **FORMA** ⇒ círculo=izquierda / triángulo=derecha. La regla
alterna cada `trialsPerRule` (default 5). El generador `RuleSwitchGenerator` es el ÚNICO con estado de secuencia
(contador de trial → regla); `reset()` (nuevo método aditivo de `SpeedPromptGenerator`, default no-op ⇒ E1–E7 sin
cambio) lo vuelve a cero por corrida. El estímulo es aleatorio por trial; la REGLA es determinista por índice, así
que el calendario de cambios es reproducible para el analizador. La señal (`cue`) muestra regla+estímulo en la fase
Stimulus; las etiquetas de panel muestran el MAPPING de la regla activa (no la respuesta).

## Métricas de cambio (analizador puro, `RuleSwitchAnalyzer`)
Derivadas de la secuencia ORDENADA de trials persistidos (rt + acierto + índice); `isSwitch(i)` = i>0 && i%N==0
(mismo criterio que el generador — cross-check testeado). `null` cuando no hay datos (nunca 0 fabricado).
- **switchCost** = mediana RT(ventana post-cambio) − mediana RT(estables) [ms].
- **postSwitchAccuracy / stableAccuracy**.
- **recoveryTrialsMedian** = trials desde cada cambio hasta el primer acierto (mediana).
- **perseveración** = `RuleSwitchLogic.isPerseveration` (respuesta = regla anterior): modelo PURO testeado;
  el CONTEO EN VIVO queda DIFERIDO (necesita el lado elegido en persistencia — infra fuera de foco).

## Scoring (mapping honesto)
| Métrica deseada | Existente usada | Estado |
|---|---|---|
| velocidad / precisión general / estabilidad / impulsividad | `SpeedScoreEngine` barras + `pc`/`cv`/`impIdx` | reused |
| preliminaryScore01 | `CognitiveProfileScore.total` (0–1, `preliminary=true`) | reused |
| precisión post-switch · coste de cambio · recuperación | `RuleSwitchAnalyzer` (sobre trials persistidos) | derived (feedback) |
| perseveración (conteo en vivo) | — | **pending** (modelo puro testeado; captura del lado elegido diferida) |
| exerciseVersion / scoringVersion | `SpeedExerciseId.E4_CAMBIO.code`+`VersionSnapshot` / `SpeedVersions.SCORE_VERSION` | reused |
El score autoritativo sigue siendo el 0–1 preliminar; el análisis de cambio alimenta SOLO el feedback.

## Feedback (`RuleSwitchFeedback` → `SpeedStrings.RS_FEEDBACK_*`)
Lente flexibilidad-con-precisión, gated a E4 en la pantalla de fin: CALIBRATING · LOW_PRECISION ("velocidad alta,
precisión cayó") · HIGH_SWITCH_COST ("el coste de cambio fue alto…") · GOOD_ADAPTATION ("te adaptaste bien…") ·
BALANCED. Copy seria, sin IQ/arcade/humillación/claims oficiales (enforced por test + copy scanner).

## Quality gate
Objetivo cognitivo ✅ · mecánica jugable ✅ · dificultad escalable (trialsPerRule + deadline staircase) ✅ ·
métricas ✅ · feedback ✅ · scoring preliminar ✅ · tests ✅ · copy seria ✅ · versionado ✅ · integrado en
menú/pantalla ✅ · persistencia (pipeline existente) ✅ · **analytics DIFERIDO** ⇒ estado = **score-ready +
MVP-candidate**, NO "MVP-ready completo".

## Inventario de Velocidad Mental (estados honestos)
- **Implementados/jugables**: 6 → E1 Destello · E2 Comparación · **E4 Cambio de Regla (nuevo)** · E3 Cifrado ·
  E6 Multivía · E7 Compuerta.
- **Score-ready preliminar**: 6 · **MVP-candidate**: 6 · **MVP-ready completo**: **0** (analytics `mental_speed_*`
  DIFERIDO + gate físico §15 abierto).
- **Faltan hasta ~15**: ~9 (batería Post-MVP de 16).

## Tests
`RuleSwitchGeneratorTest` (mapping por regla, alternancia/switch, perseveración, prompt 2-dim, reset, determinismo,
config), `RuleSwitchAnalyzerTest` (switchCost/postSwitchAccuracy/recovery, null-sin-switches, cross-check
generador↔analizador), `RuleSwitchFeedbackTest` (mapping + copy safety), `SpeedPromptGeneratorsTest`
(+reset no-op para generadores sin estado). E4 auto-cubierto por `SpeedStringsHonestyTest` (VISIBLE_COPY).

## Gates
`compileKotlinDesktop` ✅ · `desktopTest` 3033/0/0 ✅ · `compileAndroidMain` ✅ · `detekt` (solo el foreign
`IndexedSecureKeyValueStore`) ✅ · copy scanners ×3 ✅ · `scan:secrets` ✅. Preflight: `APP_VERSION` → `"1.0-speed-preliminary"`.
**Android runtime no probado** (sin instrumented test) · **iOS no probado** (sin Mac) · score **PRELIMINAR** (§15 abierto).
