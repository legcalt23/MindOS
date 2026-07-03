# MindBoost · Emociones — CLOSED DESIGN (Fase 2B, Ola 0)

> Output del equipo multiagente (4 specs + 8 auditores red-team + síntesis). Todas las
> contradicciones reconciliadas y los 50 hallazgos P0/P1 resueltos a favor de las decisiones
> CEO bloqueadas y de la seguridad. Verificado contra ficheros reales del repo
> (`MetricKey.kt` = enum cerrado, `RtStatistics.kt` = `internal`, `MindBoostStorageNamespaces.kt`
> = `AccountBound` no auto-cablea borrado, `TerminologyScanner.kt` = IGNORE_CASE, NO plega acentos).
> **OPEN BLOCKERS: ninguno** (todo P0/P1 resuelto dentro de las decisiones bloqueadas).

**Convergencia de nombres (autoritativa):** id prefix `EM1..EM15`. `TransferLabel = { TASK_ONLY,
PLAUSIBLE, NOT_DEMONSTRATED, PLACEBO_RISK }`. Payload = `EmotionStimulusPayload`. Respuesta =
`EmotionResponseKey` (incl. `CATEGORY_A..D`). Categoría EM3 = `EmotionCategory`.

---

## 1. Resumen del módulo y constructo central

Entrenamiento cognitivo-emocional funcional aplicado al rendimiento (pausa antes de actuar,
recuperación tras error, claridad bajo carga). **No** es salud, clínica, diagnóstico ni terapia.
Standalone track v1 con interfaz overlay preparada para 2C.

**Constructo `EmotionalRegulationProfile`** — nombre visible "Perfil funcional de regulación en
entrenamiento". 10 scores funcionales + meta-campos (`dataSufficiency`, `lastSessionQuality`,
`safetyLoad`, `practiceEffectRisk`, `transferConfidence`, `confidence`). Cada score es un
`EmotionMetricValue` con `value 0..1`, `n`, `confident`. Ningún número se muestra como hard-number
por debajo de `INITIAL_TREND` ni con `confident == false`. **Señal de éxito principal:** recuperación
post-error (EM2).

**Invariantes de honestidad (transversales):**
- Eventos extremos (lapsos/comisión/anticipaciones) sobre crudos; tendencia central (mediana/CV)
  sobre MAD-filtrado.
- `selfReport ≠ proxy`: campos separados, nunca fusionados. Calibración = estructura-only v1, no
  persistida, no UI.
- Sin NaN: `EmotionMetricValue.unavailable(...)`; `confident = n >= MIN_N`.
- Proxies de sobrecarga **solo arman un check de seguridad**, jamás infieren emoción.
- `transferConfidence` nunca supera `NOT_DEMONSTRATED` por debajo de `STABLE`.

## 2. Catálogo de 15

`implemented == EmotionExerciseGenerators.registry.keys` (consistency test). `all` = 15.
EM4/EM5 = stretch (solo estructura si no compromete los 3 jugables).

| id | displayName | capacidad | métrica principal | transfer | v1 |
|----|-------------|-----------|-------------------|----------|:--:|
| EM1_PAUSA | Pausa antes de actuar | PAUSE_BEFORE_ACTION | COMMISSION_RATE | TASK_ONLY | ✅ |
| EM2_RECUPERACION | Recuperación tras error | POST_ERROR_RECOVERY | POST_ERROR_RECOVERY | PLAUSIBLE | ✅ |
| EM3_SEPARADOR | Separar emoción/pensamiento/impulso/hecho | EMOTION_THOUGHT_SEPARATION | CATEGORY_ACCURACY | NOT_DEMONSTRATED | ✅ |
| EM4_CALIBRADOR | Calibrador intensidad-rendimiento | CALIBRATION_ACCURACY | CALIBRATION_ERROR | NOT_DEMONSTRATED | estructura |
| EM5_SESGO_AFECTIVO | Decisiones bajo sesgo afectivo | AFFECTIVE_DECISION_QUALITY | AFFECTIVE_INTERFERENCE_COST | PLAUSIBLE | estructura |
| EM6_TOLERANCIA | Tolerancia a la demanda | FRUSTRATION_TOLERANCE | FRUSTRATION_TOLERANCE_INDEX | PLACEBO_RISK | — |
| EM7_REENTRADA | Reentrada tras pausa | REGULATION_STABILITY | REGULATION_STABILITY | PLAUSIBLE | — |
| EM8_ANTIIMPULSO | Anti-impulso por comisión | PAUSE_BEFORE_ACTION | COMMISSION_RATE | PLAUSIBLE | — |
| EM9_ETIQUETADO | Etiquetado funcional rápido | GRANULARITY | GRANULARITY_INDEX | NOT_DEMONSTRATED | — |
| EM10_SEP_PRESION | Separación bajo presión leve | EMOTION_THOUGHT_SEPARATION | CATEGORY_ACCURACY | PLAUSIBLE | — |
| EM11_FEEDBACK_NEG | Continuidad tras feedback negativo neutro | POST_ERROR_RECOVERY | POST_ERROR_RECOVERY | PLAUSIBLE | — |
| EM12_ESPERA | Espera activa | PAUSE_BEFORE_ACTION | ANTICIPATIONS | TASK_ONLY | — |
| EM13_VARIABILIDAD | Variabilidad bajo carga | REGULATION_STABILITY | RT_CV | NOT_DEMONSTRATED | — |
| EM14_CONFIANZA | Calibración de confianza | CALIBRATION_ACCURACY | CALIBRATION_ERROR | PLAUSIBLE | — |
| EM15_RECUP_CADENA | Recuperación encadenada | POST_ERROR_RECOVERY | POST_ERROR_RECOVERY | PLAUSIBLE | — |

`transferLabel` lleva exactamente UN valor enum (test lo fija). EM3 = `NOT_DEMONSTRATED`. EM1 =
`TASK_ONLY` (el matiz "plausible" vive solo en `plainDescription`, nunca como segundo label).

## 3. Diseño detallado de los 3 jugables

Regla común: `RecordedEmotionTrial(stimulus, latencyMs, responseKey, chosenCategory)` lleva TODO lo
que el clasificador necesita desde el día 1. El **`EmotionSessionAssembler` es la única autoridad de
clasificación** — los contadores en vivo del holder son solo display. Generadores deterministas
(`Random(seed)`). El análisis post-error es propiedad del **assembler** sobre `RecordedEmotionTrial[]`,
NO salida del generador.

**EM1 — Pausa antes de actuar (go/no-go afectivo single-tap).** El resultado GO/NO-GO lo decide un
**marco visual no afectivo** (abierto=GO, cerrado=NO-GO); la palabra afectiva es el **distractor**
(congruencia regla-vs-tono). El tono nunca es la respuesta (no render deshonesto). NO-GO ~25-30%.
Generador `X1PausaGenerator`: seeded; garantiza ≥1 no-go + mínimo de celdas HIT congruentes e
incongruentes por bloque (para que AFFECTIVE_INTERFERENCE_COST pueda ser `confident`). Floor de
impulsividad (lectura de palabra) = **350 ms** (no 250). Renderer `EmotionGoNoGoField` single-tap.
Primaria COMMISSION_RATE = `FALSE_ALARM / NO_GO` (denominador = no-go, por bloque). Secundarias
AFFECTIVE_INTERFERENCE_COST (gated `confident`, agregado multi-sesión), RT_MEDIAN (go), ANTICIPATIONS.
Zona `COMMISSION` (up si commission≤0.10 y confident y noGo≥6; down si ≥0.25; ignora accuracy).

**EM2 — Recuperación tras error (2-alt perceptual + secuencias post-error) — HEADLINE.** Host
perceptual neutro 2-alternativas (LEFT/RIGHT); el adaptativo baja `discriminability` hasta provocar
errores **normales** (nunca injustos). Tras error: feedback negativo NEUTRO ("No esa vez.") + ventana
post-error de discriminabilidad fija. **Garantía de medibilidad:** el generador planta de forma
determinista trials near-threshold "error-likely" calibrados para ≥ `MIN_N_POST_ERROR (=3)` secuencias
post-error por micro-sesión sin inducción injusta (testeado en N seeds). Generador
`X2RecuperacionGenerator`: host list seeded + flags `harder` + prefijos id train/eval. La ventana
post-error la detecta el **assembler** en runtime. Renderer `EmotionTwoAltField`: dos targets
persistentes, etiquetados, alto contraste (texto+icono, NO solo color) + divisor + `contentDescription`
(NO reusa el `ChoiceResponseLayer` invisible de focus). Primaria POST_ERROR_RECOVERY (composite §4).
Secundarias POST_ERROR_SLOWING, ACCURACY, RT_CV, ABANDONMENT. Zona `TOLERANCE` estricta (up cap 0.80,
down <0.70).

**EM3 — Separar emoción/pensamiento/impulso/hecho (clasificación 4 categorías).** Una frase neutra
cotidiana → 4 botones fijos (Estado interno · Pensamiento · Impulso · Hecho). Ejes de confusión
deliberados: pensamiento↔hecho, impulso↔acción. Generador `X3SeparadorGenerator`: shuffle seeded de un
**banco fijo ≥80 ítems (20/categoría)** con pools id train/eval separados (un banco de 24 no alcanza
MIN_N=8 en slice eval). Renderer `EmotionFourCategoryField`: grid 2×2 nuevo (autorizado), mapeo
cuadrante→categoría fijo toda la sesión, targets ≥48 dp, icono+label ES corto con wrap a dos líneas
testeado al ancho mínimo, confirm-on-release. 4 botones → `EmotionResponseKey.CATEGORY_A..D` (sin
fallback "PRIMARY+index"). Scoring: matriz de confusión 4×4 en el assembler (autoridad); corrección de
azar: 4-cat = 0.25; cada sub-índice pairwise = 0.5 solo sobre trials cuya verdad sea una de las dos
categorías. Primaria CATEGORY_ACCURACY + EMOTION_THOUGHT_SEPARATION (chance-adjusted). Confusiones
contadas sobre crudos. Zona `GENERAL` (0.80-0.85). Label `NOT_DEMONSTRATED`.

## 4. Motor de métricas (10 scores) + agregación + tiers

**Ownership:** emotions tiene su propio `EmotionMetricKey` enum + `EmotionMetricValue`/`EmotionMetricSet`
(forma espejo de `MetricValue`). **NO importar el `MetricKey` de focus** (enum cerrado) ni `RtStatistics`
(`internal` — copiar las 4 fns puras a `EmotionRtStatistics`, `MAD_SCALE=1.4826`). Test de compilación
"emotions no importa focus MetricKey". El adaptativo lee `EmotionMetricSet`, nunca el de focus.

`EmotionMetricKey` ⊇ { POST_ERROR_RECOVERY (headline), COMMISSION_RATE, PAUSE_BEFORE_ACTION_RATE,
CATEGORY_ACCURACY, GRANULARITY_INDEX, EMOTION_THOUGHT_SEPARATION, AFFECTIVE_INTERFERENCE_COST,
CALIBRATION_ERROR, FRUSTRATION_TOLERANCE_INDEX, AFFECTIVE_DECISION_QUALITY, SAFE_PERSISTENCE_INDEX,
REGULATION_STABILITY, ACCURACY, RT_MEDIAN, RT_CV, FALSE_ALARMS, OMISSIONS, ANTICIPATIONS,
POST_ERROR_ACCURACY, POST_ERROR_SLOWING, IMPULSIVITY, LAPSES, ABANDONMENT }.

**Acceso confidence-aware:** el engine/safety NUNCA actúan sobre un valor colapsado a default. Se provee
`valueIfConfident(key): Double?`; toda rama de escalado/seguridad exige `confident==true` Y su `MIN_N`.
Métrica gobernante ausente/unconfident → **Hold** (nunca StepUp).

| Score | MIN_N | Notas |
|---|---|---|
| **PostErrorRecovery** ⭐ | 3 pares post-error | composite condicional: penaliza slowing SOLO si la accuracy post-error no recupera; premia rápido SOLO si mantiene accuracy; penaliza impulsividad post-error (comisión rápida), no slowing benigno. Headline = tiempo-a-baseline con PES como covariante. n<3 → unavailable |
| Granularity | 8 | `0.6·labelAcc + 0.4·(distinctCorrect/available)` |
| EmotionThoughtSeparation | 8 pair-trials | `max(0, acc−chance)/(1−chance)`, chance 0.5 sobre ese par |
| PauseBeforeAction | 6 no-go | `(1−commission)·0.8 + (1−normRtGoCv)·0.2` |
| AffectiveInterferenceCost | 5/condición | `1 − clamp(cost)` |
| CalibrationAccuracy | 4 | self-report×proxy; **estructura-only v1, NO persistida, NO mostrada** |
| FrustrationTolerance | 1 episodio (prelim) | `completed/offered`, penaliza abandono/gaming |
| AffectiveDecisionQuality | 5 | `acc − biasErrorRate·0.5` clamp[0,1] |
| SafePersistence | 6 | `persistence·(1−impulsivity)` |
| RegulationStability | 2 bloques | `1 − normalize(|perf_calm − perf_pressure|)`, tendencia central por mediana |

**CalibrationAccuracy aislada:** función única con KDoc "mide PRECISIÓN DE PREDICCIÓN, no estado";
tipo distinto; nunca input de otro score; **excluida del `metricsSummary` v1**; oculta bajo `STABLE`.
selfReport y proxy en campos DTO separados.

**Tiers (decisión F):** 1=EXPLORATORY (sin números) · 2-3=PRELIMINARY (banda) · 4-7=INITIAL_TREND
(trend dibujable) · 8+=STABLE. `trend = UNKNOWN` hasta `confidence ≥ INITIAL_TREND` Y scores confident.
`practiceEffectRisk` NO se computa numéricamente en v1 (unavailable hasta Ola 4); nunca alimenta
`transferConfidence`, que en v1 deriva SOLO del peor-caso de label por ejercicio.

## 5. Motor adaptativo + modelo de seguridad (números exactos)

Difficulty = vector (`EmotionDifficultyVector`, ejes TEMPORAL_PRESSURE, AFFECTIVE_INTERFERENCE,
INHIBITORY_DEMAND, POST_ERROR_LOAD, CATEGORY_AMBIGUITY, TIME_ON_TASK); mueve exactamente un eje por
decisión. Intensidad rastreada aparte (variable de seguridad). Zona vive en el Def del catálogo
(EM1=COMMISSION, EM2/EM6/EM11/EM15=TOLERANCE, EM3/EM10=GENERAL).

**`decide()` orden de ramas — seguridad estrictamente primero, cada una retorna ya:**
1. SafetyStop (intensity ≥5). 2. ReduceIntensity (≥4): −1 intensity (floor 1), simplifica eje gobernante.
3. MidSessionCheckIn (proxy, no resuelto este bloque, cooldown ok). 4. Cooldown (slowing+RT↑ →
stepDown TIME_ON_TASK). 5. ContainImpulsivity (≥2 comisiones RT<floor Y FALSE_ALARMS≥2 → stepUp
INHIBITORY_DEMAND). 6. StepDown (zona: GENERAL acc<0.80 / TOLERANCE acc<0.70 / COMMISSION commission≥0.25,
o consecutiveErrors≥3, o post-error acc<0.60·acc). 7. Hold (histéresis). 8. StepUp.

**StepUp safety gate:** exige `firedProxies.isEmpty()` en bloque actual+anterior (no solo
`directive==NONE`), `consecutiveErrors==0`, zona-pass, y self-report gate (`selfReport==null ||
intensity ≤ 2`). Un check-in resuelto-bajo limpia el re-prompt (anti-nag) pero NO limpia
`escalationLocked` mientras haya proxies activos.

**Techo de intensidad (HARD 3):** modelar intensidad como Int discreto 1..5. Mapping 1-2 normal · 3
techo (hold) · 4 REDUCE · 5 STOP. El engine nunca induce >3; 4/5 solo de self-report/guardrail.

**Config (tuning, `@Suppress("MagicNumber")`):** stepUp 0.07, stepDown 0.12, GENERAL up≥0.85/down<0.78,
TOLERANCE up≤0.80/down<0.70, COMMISSION up≤0.10/down≥0.25, consecutiveErrorsDown 3,
`cooldownAfterDecisions=2`, impulsiveRtFloorMs por-ejercicio (EM1=350).

**Detector de sobrecarga — proxies arman un check, jamás infieren emoción. Cualquiera dispara:**
comisión ≥0.30 · racha ≥3 errores · RT_MEDIAN ≥1.40× baseline 2 bloques · RT_CV ≥1.60× baseline ·
≥2 intentos de salir/bloque · ≥3 pausas/bloque · post-error acc <0.60× pre-error acc · ≥2 comisiones
post-error RT<250 ms.

**Baseline v1:** sin baseline cross-sesión (Ola 4). Baseline rolling intra-sesión (primeros N bloques
confident de ESTA sesión) + fallback RT-mediana derivado de `responseWindowMs` (0.6×window). El proxy
de slowing NO dispara en el primer bloque; si no hay baseline, NONFUNCTIONAL explícito (nunca sobre una
constante adivinada). `fallbackBaselineRtCv = 0.25`.

**Check-in:** 1-2→NONE · 3→hold · 4→REDUCE · 5→STOP. Solo desde proxy o pausa de usuario. Nunca
intra-trial. 1-tap intensidad 1-5, sin label ni texto.

**Anti-gaming pausa — dos contadores:** (a) limit counter incrementa SIEMPRE → 4 pausas rápidas →
ReduceIntensity + `recommendShorterSession`. (b) cooldown 45s solo suprime re-prompt de check-in, NO el
limit ni el proxy de pausa-repetida. Máx 3 pausas/bloque sin penalizar si post-pausa `≥ pre − 0.05`.
Pausar siempre se registra, nunca se penaliza; se loguea como `safetyEvent`.

**Feedback:** 4 partes (qué ocurrió / qué significa / qué practicar / qué NO concluir) + transferLabel +
confidence. **Cierre por desregulación (STOP):** detener → cierre neutro ("Has terminado el ejercicio
por hoy. No pasa nada por parar.") → micro-pausa 20-30s → salir. Sin consejo clínico, sin retención.

## 6. Persistencia local cifrada

Store `SecureKeyValueStore`, namespace `MindBoostStorageNamespaces.Emotions = "mindboost_emotions"`.
DTOs `@Serializable`, `EmotionSchema.CURRENT = 1`. `EmotionJson = Json { ignoreUnknownKeys = true;
encodeDefaults = true }`. **Solo resúmenes:** exerciseId, startedAt, durationMs, difficultySummary,
metricsSummary, selfReportBefore, selfReportAfter, safetyEvents, completed, abandoned, schemaVersion.
NO trials crudos / texto libre / identificadores. DTO agregado con su propio `schemaVersion`, leído vía
`readVersioned` (quarantine).

**Retención:** `pruneToLast(userId, max=100)` en cada write (focus NO poda — esto es nuevo y testeado).
Agregados permanentes. Key `session:<userId>:<startedAt>` — validar `userId` (rechazar blank / con `:`).

**Borrado:** `deleteAll(userId)` llama a `store.clearNamespace()` (purga entries+failures+quarantined),
no por enumeración (que pierde quarantined). **AccountBound NO auto-cablea borrado (verificado).**
Ediciones: (1) añadir `Emotions` a `MindBoostStorageNamespaces.AccountBound`; (2) en
`AppGraph.rememberAppGraph()` añadir `emotionsStore` a la lista `remember(...)` Y al `listOf(...)` del
`SecureNamespaceUserDataEraser`. Test `AccountBoundErasureCoverageTest`.

**`safetyEvents` minimizado:** `SafetyEventType` + `atOffsetMs` (relativo EpochClock) + `reportedIntensity?`.
NO serializar la lista completa de `firedProxies`; a lo sumo un enum de proxy dominante.

**Interfaz read-only para MindBoostProfile:** `EmotionProfileReadOnly.snapshot(userId)` expone SOLO
`tier`, `dataSufficiency`, `confidence`, `trend`. Sin upload, sin trials, sin calibración. Dirección de
dependencia: emotions→(nada); MindBoostProfile consume la interfaz.

## 7. Seguridad / ética / claims

5 disclaimers (constantes scanner-clean en `EmotionStrings`), incluye el reescrito **"No usamos estos
datos con fines clínicos ni para identificar condiciones de salud."** (ni "clínicos" ni "salud" están en
la lista prohibida). **Sin allowlist UI** (test lo asegura).

**Terminology scanner.** Decisión I/Q4c exige **insensible a mayúsculas+acentos, palabra completa,
cuidadoso con guiones** sobre TODO texto visible. El scanner de arena es IGNORE_CASE solo, NO pliega
acentos (verificado). Adaptaciones: `EmotionTerminologyScanner` (puro): NFD-normaliza + strip `\p{Mn}`
en contenido y término, luego `Regex("\\b…\\b", IGNORE_CASE)` (se mantiene `\b`: cruzar guiones es
deseado para "anti-estrés"/"post-estrés"). Mismo matcher en la tarea Gradle Y en
`EmotionTerminologyComplianceTest` (parity). Lista prohibida en `build.gradle.kts` (no scaneado). La
tarea **falla (no return silencioso) si falta el sourceDir** (anti-gate-deshabilitado-por-typo).
Registrada sobre commonMain+commonTest+desktopTest, en `check` vía `dependsOn`. TODO string visible como
constante Kotlin bajo `commonMain/emotions` (sin copy cargada de recursos en v1).

**Lista prohibida (singular/plural/variantes, accent-folded):** diagnóstico/-ar/-a, terapia,
terapéutico, tratamiento, cura/curar, depresión, ansiedad, trauma, trastorno, patología, enfermedad
mental, crisis, pánico, TDAH, TOC, bipolar, **estrés** (+ compuestos con guion), suicidio, autolesión.

**Copy permitido:** presión, carga, reto, dificultad, intensidad, activación, demanda, regulación,
claridad, práctica, rendimiento bajo carga. **Prohibido:** "estrés"; read-outs emocionales ("estás
ansioso/alterado/desregulado/nervioso/bloqueado"); humillación; moralización; consejo clínico.

**Accesibilidad:** no-solo-color; targets ≥48 dp; **EmotionSafetyBar** siempre visible (Pausar |
Reducir intensidad | Salir) en todo estado en marcha; preferencia **"Reducir presión temporal"** que
clampa el eje PRESSURE por encima de la adaptación; **MicroPauseOverlay** estático/baja-moción, Salir
visible TODO el rato, Continuar habilitado solo tras 20s; 4-part feedback + transferLabel SIEMPRE
juntos; scores nunca numéricos por debajo de INITIAL_TREND.

## 8. Estructura de archivos (Ola 0 vs Ola 1)

OLA0 = catálogo + enums + DTOs + strings + engines (dominio) + safety + scanner + tests de dominio,
compilable, sin render. OLA1 = los 3 jugables end-to-end + wiring + UI tests.

```
emotions/
  domain/
    catalog/  EmotionCatalogIds.kt[0] · EmotionExerciseDefinition.kt[0] · EmotionExerciseCatalog.kt[0]
    model/    EmotionMetric... → metrics; EmotionStimulus.kt[0] · EmotionResponse.kt[0]
              SelfReport.kt[0] · EmotionalRegulationProfile.kt[0]
    metrics/  EmotionMetricKey.kt[0] · EmotionRtStatistics.kt[0] · EmotionMetricsEngine.kt[1] · DefaultEmotionMetricsEngine.kt[1]
    exercise/ EmotionTrialGenerator.kt[0] · EmotionExerciseGenerators.kt[0] · X1/X2/X3 generators[1]
    session/  EmotionTrialClassifier.kt[1] · EmotionSessionAssembler.kt[1]
    adaptive/ EmotionDifficulty.kt[0] · EmotionAdaptiveEngine.kt[1] · DefaultEmotionAdaptiveEngine.kt[1]
    safety/   EmotionSafetyLimits.kt[0] · SafetyProxySignals.kt[0] · SafetyEvent.kt[0]
              EmotionSafetyGuardrails.kt[1] · DefaultOverloadDetector.kt[1] · PauseAntiGaming.kt[1]
    feedback/ FourPartFeedback.kt[0] · EmotionFeedbackBuilder.kt[1]
  data/
    dto/      EmotionDtos.kt[0]
    local/    EmotionJson.kt[0] · LocalEmotionRepositories.kt[1]
    mapper/   EmotionMappers.kt[1]
  di/         EmotionGraph.kt[1]
  presentation/ EmotionStrings.kt[0] · EmotionSessionStateHolder.kt[1] · EmotionHomeStateHolder.kt[1]
              state/EmotionUiState.kt[1] · ui/EmotionRootScreen.kt[1] · EmotionSessionScreen.kt[1]
              ui/SelfReportScreen.kt[1] · MicroPauseOverlay.kt[1] · EmotionSafetyBar.kt[1]
              ui/components/EmotionGoNoGoField/EmotionTwoAltField/EmotionFourCategoryField[1]
  profile/    EmotionProfileReadOnly.kt[0]
```
Ediciones a ficheros existentes: `MindBoostStorageNamespaces.kt`[0], `AppGraph.kt`[0, borrado],
`MindBoostHubScreen.kt`[1, wiring+reorder], `shared/build.gradle.kts`[0, scanner task].

`EmotionRootScreen(userId, displayName, onExit, modifier)` (firma focus; el hub provee displayName).

## 9. Tests obligatorios + gates

OLA0 (commonTest salvo nota): EmotionExerciseCatalogConsistencyTest (15 ids sin dup; `implemented ==
registry.keys`; un valor de transferLabel; EM3=NOT_DEMONSTRATED), EmotionMetricKeyCoverageTest (+ "no
import focus MetricKey"), EmotionRtStatisticsParityTest, EmotionTerminologyComplianceTest (desktopTest:
acentos/case/guion + banks/disclaimers limpios + no-allowlist), AccountBoundErasureCoverageTest,
adaptive/safety boundary tests, metrics golden vectors (PostErrorRecovery slow-accurate ≥ fast-flat;
n<3 unavailable), profile n=1 → trend UNKNOWN.

OLA1: determinismo por generador (mismo seed→mismos trials); X1 garantías congruente/incongruente +
forced no-go; X2 ≥3 probes post-error a dificultad media; X3 banco ≥80 + train/eval disjuntos +
responder aleatorio → separación ~0; repo deleteAll incl. quarantined / prune 105→100 / userId con `:`
rechazado / version>current → null+quarantined; UI semantics (EM2 dos targets contentDescription; EM3 4
targets ≥48dp; SafetyBar siempre; MicroPause Salir t=0, Continuar<20s deshabilitado; 4-part feedback
siempre); **MindBoostHubReachabilityTest** (cada track montado + cada implemented seleccionable —
guarda contra "implemented pero invisible").

**Gates (serie, mismo worktree):** `:shared:compileKotlinDesktop` · `:shared:compileAndroidMain` ·
`:shared:desktopTest` · `:shared:detekt` · `:shared:check`. Detekt Compose:
`@file:Suppress("FunctionNaming","MagicNumber","LongMethod","MaxLineLength")` justificado;
`decide()` `@Suppress("CyclomaticComplexMethod")`; config `@Suppress("MagicNumber")`; ReturnCount≤2;
rethrow `CancellationException`; sin catch genérico Firebase-style. Tests que escanean fuentes en
`desktopTest`. No paralelizar Gradle en el mismo worktree.

## 10. OPEN BLOCKERS

**Ninguno.** Los 50 P0/P1 eran spec-internos (naming, "reusa X de focus" donde X es cerrado/internal,
"auto-cablea borrado" donde no) o under-spec, todos resueltos dentro de las decisiones bloqueadas +
seguridad. El reorder del hub se ejecuta como cambio explícitamente revisado (ejecuta decisión K, no es
una decisión nueva).

## 11. OLA 1 — Orden de implementación

Pasos acotados, compilar tras cada uno; tests en los pasos-gate.
1. Generators + classifier (dominio). Flip `implemented`=[EM1,EM2,EM3] + registry en el mismo cambio.
   → compile desktop; consistency + determinismo.
2. Metrics + assembler (autoridad; matriz 4×4; detección ventana post-error). → compile; metrics +
   assembler + holdout-separation (desktopTest).
3. Adaptive + safety (orden de ramas, escalationLocked gate, clamp intensidad, zona-from-Def, baseline
   intra-sesión); overload detector; pause anti-gaming (dos contadores); feedback builder. → compile;
   adaptive/safety/feedback tests.
4. Persistence + DI (pruneToLast on write, clearNamespace deleteAll, userId validation, readVersioned;
   EmotionGraph + rememberEmotionGraph). → compile; repo tests.
5. State holders + UI state (selfReport-before → bloque una-respuesta-por-trial → assemble →
   selfReport-after → feedback; safety hook; PauseGovernor). → compile; state-holder tests (TestScope +
   cancelChildren()).
6. Renderers + screens (targets etiquetados no-color + contentDescription; 2×2 ≥48dp confirm-on-release;
   MicroPause low-motion; SafetyBar). → compile desktop **y** android; UI/semantics tests.
7. Hub wiring (4-edit contract + reorder K: Decisiones↑, Emociones, ... Creatividad↓). → compile ambos;
   MindBoostHubReachabilityTest.
8. Final gate serie completo.
