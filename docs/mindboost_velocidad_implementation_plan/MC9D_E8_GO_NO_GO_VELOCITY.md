# MC9-D — E8 · Go/No-Go Velocity (Inhibición Rápida)

8º ejercicio del bloque Velocidad Mental. Entrena **inhibición de respuesta bajo presión**: responder a la señal
de avance (GO) e inhibir en la de freno (NO-GO) antes del deadline. Serio, medible, versionado y honesto; sigue el
patrón de telemetría/persistencia de E5. **NO** toca `TrialRecord`/`MeasurementRun`, AI Coach, ranking/PvP,
Firestore ni otros ejercicios.

## Objetivo cognitivo
Inhibición de respuesta, control de impulsos, velocidad con precisión, discriminación rápida GO/NO-GO, estabilidad
bajo presión y resistencia a la respuesta prepotente ante señales frecuentes/infrecuentes.

**Claims permitidos:** "control de inhibición", "señal de avance/freno", "velocidad con precisión", "preliminar",
"orientativo". **Prohibidos** (y no usados, ni negados): IQ/inteligencia, clínico/diagnóstico/salud, TDAH,
autocontrol, ansiedad, multitarea/productividad, transferencia garantizada, salud ocular.

## Mecánica
Cada trial: foreperiodo aleatorio → estímulo GO o NO-GO → ventana de respuesta = `deadline + gracia`.
Resolución honesta (pura, `GoNoGoOutcome.resolve`):

| Trial | Respuesta | Resultado |
|---|---|---|
| GO | tap ≤ deadline | correct |
| GO | sin tap | omission |
| GO | tap > deadline | lateResponse |
| GO | tap en foreperiodo | anticipation (prematura) |
| NO-GO | sin tap | correct (inhibición) |
| NO-GO | tap post-onset | commission (fallo de inhibición) |
| NO-GO | tap en foreperiodo | anticipation (no cuenta como comisión post-onset) |

**Requisito crítico (§7):** la velocidad en GO NO compensa las comisiones en NO-GO — tocar casi todo o no tocar
nada puntúa bajo aunque el TR de GO sea excelente (probado).

## Dificultad
`GoNoGoConfig`: `goRatio` (V1 0.75), `scheduleBlock` con **scheduler de bag barajado** (Fisher-Yates determinista)
que garantiza la proporción y un `minNoGoPerBlock` (muestra mínima de inhibición, no azar puro); `deadlineMs`
derivado del nivel de dificultad (`base − nivel·step`, con floor); `foreperiodo` aleatorio; `difficultyLevels`
opcional (`mixedDifficulty()`) para habilitar `difficultyAdjustedControl`. V1 producción = dificultad única ⇒ esa
métrica queda `Unavailable` (mapper listo para runs mixtas).

## Métricas (mapper puro versionado `GONOGO_METRICS_VERSION`)
`goAccuracy`, `noGoAccuracy`, `commissionRate`, `omissionRate`, `anticipationRate`, `correctGoSpeedMs`,
`correctGoRtVariability`, `inhibitoryControlIndex` (= **precisión balanceada** (goAcc+noGoAcc)/2: tap-all⇒0.5,
tap-none⇒0.5, perfecto⇒1.0), `speedAccuracyTradeoff` (fast-commission rate), `stabilityIndex`,
`difficultyAdjustedControl`. Cada una `Measured(v)` con muestra suficiente o `Unavailable(razón)` — nunca un 0
fabricado. Gates: NO-GO<mín ⇒ commissionRate/inhibitory `Unavailable`; aciertos-GO<mín ⇒ velocidad `Unavailable`.

**Reales (medidas con muestra):** todas las anteriores. **Unavailable estructural en V1:**
`difficultyAdjustedControl` (un solo nivel por corrida en producción).

## Scoring honesto (`GoNoGoScorer`, `GONOGO_SCORE_VERSION`, PRELIMINAR)
Índice 0–1 = media ponderada de componentes MEDIDOS (renormalizada sobre los disponibles):
`0.35·inhibitoryControl + 0.25·goAccuracy + 0.15·correctGoSpeed(norm) + 0.10·stability +
0.10·difficultyAdjustedControl + 0.05·anticipationControl`. **Techos multiplicativos:** comisión alta ×0.30,
precisión GO baja ×0.45, anticipación alta ×0.50. Muestra insuficiente (GO<6 o NO-GO<4) ⇒ `index = null`
(CALIBRATING, nunca 0); NO-GO<8 ⇒ `lowConfidence`. `preliminary` siempre true. NO se cablea al perfil oficial
(transversal, no tocado).

## Feedback (`GoNoGoFeedback`, prioridad estricta)
comisión alta → omisión alta → anticipación alta → inestabilidad → buen control (rápido vs "reduce latencia") →
equilibrio/preliminar. La comisión se evalúa PRIMERO: **la velocidad nunca produce feedback positivo si falla la
inhibición**. Copy en `SpeedStrings` (`GNG_FEEDBACK_*`), describe la conducta de la corrida, no etiqueta a la
persona; sin lenguaje clínico ni arcade.

## Persistencia (patrón E5, store genérico)
`GoNoGoTelemetrySummaryV1` (`@Serializable`, señales `GoNoGoSignal` Measured/Unavailable) se guarda vía
`ExerciseTelemetryStore` (namespace `mindboost_speed`, prefijo `tele:`, cubierto por el borrado de cuenta) con
helpers tipados `saveGoNoGoSummary`/`goNoGoSummaryForSession`/`goNoGoSummariesForUser`. `telemetryVersion =
summary.summaryVersion` (esquema del payload, NO fórmulas); `metricsVersion` dentro del JSON; `syncStatus =
LOCAL_ONLY` (heredado del record). Idempotente por `userId::sessionId`; `exerciseId` != E8 o JSON corrupto ⇒ null
(nunca rompe la sesión). Firestore sync NO implementado.

## Anti-gaming / quality diagnostics (`GoNoGoQualityDiagnostics`, en el summary, NO altera el perfil)
`impossiblyFastTrials`, `tapAllStrategy`, `tapNoneStrategy`, `noGoSampleTooLow`, `anticipationFlood`,
`excessiveLateResponses`, `lowTrialCount`, `roboticTiming` (SD de TR casi nula = patrón repetido),
`improbablePerfectUnderDifficulty`. No acusatorio. La defensa PRINCIPAL contra el gaming es estructural: la
precisión balanceada + los techos hacen que tap-all/tap-none puntúen bajo (probado).

## UI / integración
`SpeedExerciseId.E8_INHIBICION`; pantalla dedicada `E8InhibicionScreen` (el paradigma NO-GO no encaja en la
superficie RT-tap compartida) que dirige la state machine pura `GoNoGoSessionStateHolder` (eventos de tiempo desde
la UI; el RT se mide con `TimeSource.Monotonic`, stdlib); tarjeta de menú + título/instrucciones en `SpeedRootScreen`.
E8 NO entra en `SpeedProfileSource.CHOICE_EXERCISE_CODES` ni en el pipeline `MeasurementRun` (paradigma distinto).

## Tests (48 nuevos, desktopTest)
- **Generador:** balance GO/NO-GO, NO-GO garantizado por bloque, determinismo por seed, reset, config inválida,
  dificultad→deadline, metadata estructurada.
- **Outcome/summary:** las 7 resoluciones (GO correct/omission/late/anticipation, NO-GO correct/commission/
  anticipation), agregación, buckets de dificultad.
- **Métricas:** perfecto, tap-all (noGoAcc 0, inhibitory 0.5), tap-none (goAcc 0), commission/tradeoff, NO-GO
  insuficiente Unavailable, difficultyAdjustedControl single/multi, run vacía todo Unavailable.
- **Score:** fuerte→alto, tap-all→bajo (cap de comisión), tap-none→bajo (cap de precisión), anticipación→cap,
  insuficiente→null CALIBRATING, siempre preliminar.
- **Feedback:** 8 casos + "comisión alta nunca da feedback positivo".
- **Persistencia:** round-trip tipado, wrong-exercise null, JSON corrupto null, `telemetryVersion==summaryVersion`
  `!= metricsVersion` (con centinelas), idempotencia, aislamiento por usuario.
- **Holder:** corrida perfecta→STRONG_CONTROL+persistida, tap-all→bajo+IMPULSIVE_COMMISSION, foreperiodo→anticipación,
  NO-GO end-to-end en el summary persistido.
- **Copy:** las strings de E8 sin léxico prohibido; todas registradas en `VISIBLE_COPY`.
- **Regresión:** los 7 ejercicios previos intactos (suite completa verde).

## Gates
`compileKotlinDesktop` ✅ · `desktopTest` **3196/0/0** ✅ · `compileAndroidMain` ✅ · `detekt` ✅ (solo 2 findings
foreign untracked ajenos: `IndexedSecureKeyValueStore:61`, `InterSessionConfidence:31`) · copy scanners ×3 ✅ ·
`scan:secrets` ✅. **Android runtime NO probado · iOS NO probado · Firestore NO probado.**

## Riesgos residuales
1. UX/timing en dispositivo no verificado (solo lógica pura en JVM); la pantalla E8 y su temporización no tienen
   test instrumentado.
2. Telemetría E8 local-only (sin Firestore sync).
3. Persistencia del summary best-effort (`scope.launch` al terminar).
4. Score PRELIMINAR y NO cableado al perfil oficial (§15 físico + analytics `mental_speed_*` abiertos).

## Inventario Velocidad Mental
8 ejercicios jugables / telemetry-ready / **preliminary-score-ready** (contribución al perfil oficial: DIFERIDA) /
**MVP-candidate** (E1/E2/E3/E4/E5/E6/E7/E8); **MVP-ready-completo = 0** (analytics
+ §15 físico + Firestore + runtime QA abiertos). Faltan ~7 hasta 15.

## GO/NO-GO para el 9º
**GO** — E8 es honesto, medible, versionado, testeado y con el patrón de telemetría ya consolidado. No iniciar el
9º sin autorización explícita.
