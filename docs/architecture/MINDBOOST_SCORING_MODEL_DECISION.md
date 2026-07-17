# Modelo de Scoring de MindBoost — Decisión de Arquitectura

**Fecha:** 2026-07-04 · **Estado:** Sellado por decisión CEO · **Autor:** Staff Mobile Architect

> Propósito: fijar, con evidencia en código (`file:line` en cada afirmación técnica), que MindBoost conserva su modelo de scoring riguroso y honesto, y explicar cómo ese modelo satisface el contrato de producto del PRD por equivalencia superior — no por la fórmula ponderada simple literal.

---

## 1. Alcance y advertencia de honestidad

Este documento es **solo documentación**. No cambia scoring de producción, no sube trials crudos, no implementa IA Coach ni Elite, no migra namespaces y no toca archivos de reglas Firestore. Donde una cláusula del PRD se cumple solo en parte, se marca **Parcial** y se explica qué falta; nada se declara "Cumple" si es "Parcial". Lo que no se pudo verificar se marca **UNVERIFIED**.

Nota de canon: la superficie de producto son **8 motores cognitivos** (Enfoque, Decisiones, Lógica, Creatividad, Lenguaje, Emociones, Velocidad mental, Memoria). La taxonomía interna tiene **9 `CognitiveDimension`** (`CognitiveTaxonomy.kt:14-24`) y **9 `CognitiveModuleId`** (`CognitiveTaxonomy.kt:34-44`). El motor de producto **Memoria** mapea a DOS dimensiones internas — `WORKING_MEMORY` (span EX1 + pares EX4) y `EPISODIC_MEMORY` (recuerdo diferido 24h EX3) — vía los ids `MEMORIA` y `MEMORIA_EPISODICA` (`CognitiveTaxonomy.kt:37-38`, KDoc del split en `CognitiveTaxonomy.kt:30-32`). Todos los demás motores son 1:1. El enum de scoring `ScoreModuleId` refleja el mismo split (`MEMORY` + `EPISODIC_MEMORY` como ids distintos, `ScoreTypes.kt:11`).

---

## 2. Decisión

**Preservamos el modelo riguroso.** El PRD es un **contrato de PRODUCTO** (qué debe ver y entender el usuario), no una **orden de degradar** el motor interno. La fórmula ponderada simple del PRD (`0.30·Acc + 0.20·Speed + ...`) es una **baseline conceptual** que describe la intención ("precisión pesa más que velocidad; la velocidad no debe premiarse a costa de la precisión"). El código ya cumple esa intención con garantías **superiores**: scorers bespoke por módulo, una máquina de estados de honestidad (`CALIBRATING/EMERGING/ESTABLISHED`), la invariante `null != 0`, y un índice global que solo promedia dimensiones consolidadas. Sustituir eso por la media ponderada literal introduciría volatilidad, permitiría gaming y rompería `null != 0`.

Decisión CEO adicional (persistencia): **añadir sincronización Firestore como continuidad privacy-by-design**, manteniendo local-first, guardado local inmediato, **sin subir trials crudos** en MVP (solo métricas estructuradas), reglas estrictas, consentimiento GDPR e integración con el borrador de cuenta. Esta decisión NO se implementa este turno (ver §11).

---

## 3. El modelo interno real (flujo sesión → perfil)

El pipeline honesto es una cadena de tipos puros y testeables:

**sesión → `ScoreResult` → `ModuleContribution` → `aggregate(...)` → `MindBoostProfile`**

### 3.1 Scorers bespoke por módulo

Cada módulo puntúa con su propia matemática de dominio, no con una fórmula común:

- **Memoria (Span complejo, EX1):** `ComplexSpanScorer` — span basado en precisión (media de ítems recordados en orden serial correcto sobre trials con procesamiento válido; `ComplexSpanScorer.kt:62-64`). El gate de validez de procesamiento `PROCESSING_VALIDITY = 0.85` es lo que lo hace un span complejo y no "un span simple disfrazado": por debajo, el bloque es `INVALIDATED` y su span **no cuenta** (`ComplexSpanScorer.kt:40`, `48`, `81-82`). El span es `null` cuando no es computable, **nunca 0** (`ComplexSpanScorer.kt:20`, `44`, `49`; agregación inter-bloque `null` si no hay bloques válidos, `ComplexSpanScorer.kt:55-60`). El RT **nunca** entra en el span (KDoc `ComplexSpanScorer.kt:30-34`).
- **Lógica:** `DefaultReasoningScorer` — ICR proceso-primero: base ponderada de respuesta final/regla/premisas/justificación/calibración/independencia (`FINAL_ANSWER_WEIGHT=0.35`, `RULE_WEIGHT=0.20`, `PREMISE_WEIGHT=0.15`, ...; `ReasoningScorer.kt:42-47`, `91-98`). **La velocidad solo resta a través de la impulsividad; nunca suma puntos** (KDoc `ReasoningScorer.kt:19`; `impulsive = !isCorrect && response.timeSpentMs < item.minimumReasoningTimeMs`, `ReasoningScorer.kt:39`; penalización en `ReasoningScorer.kt:48-52`).
- **Decisiones (core legacy `com.mindverse.mindboost`):** `ScoringEquations` — CDE/PHD/ICD/IED/CRI/BUR/FRS/AGRS/Brier. Ejemplos de rigor: `calculatePHD` invalida la sesión si el anti-gaming supera el umbral, devolviendo `0.0` (`if (agrsRolling > AGRS_INVALID_SESSION_THRESHOLD) return 0.0`, `ScoringEquations.kt:41`; `AGRS_INVALID_SESSION_THRESHOLD = 0.75`, `ScoringEquations.kt:239`); `calculateIED` penaliza respuestas impulsivas devolviendo `0.0` bajo el umbral de RT (`if (reactionTimeMs < IMPULSIVE_RESPONSE_MS) return 0.0`, `ScoringEquations.kt:89`; `IMPULSIVE_RESPONSE_MS = 500L`, `ScoringEquations.kt:254`) y modula el decaimiento gaussiano por reversibilidad (`ScoringEquations.kt:93-94`; `REVERSIBLE_DELAY_LAMBDA = 0.8` vs `IRREVERSIBLE_DELAY_LAMBDA = 0.15`, `ScoringEquations.kt:255-256` — el lambda menor de las irreversibles hace el decaimiento **más suave**, tolerando más deliberación antes de penalizar; la diferencia de trato existe y es intencional).

> **UNVERIFIED:** no se auditaron en este turno los scorers bespoke de Enfoque, Velocidad, Lenguaje, Emociones, Creatividad ni los de Memoria EX3/EX4. La afirmación "cada módulo tiene scorer bespoke" está **probada** para Memoria-Span, Lógica y Decisiones; el resto se asume por el patrón de wiring y se marca como no verificado individualmente. Para verificarlos, revisar sus `*ProfileSource`/`*Scorer` bajo `features/mindboost/*/domain/scoring`.

### 3.2 `ScoreStateMachine` — máquina de estados de honestidad (INFRA-01)

Autoridad pura que decide el veredicto de una sesión mediante una **tabla ordenada de gates** `(state, claim, predicate)`; el **primer** gate que dispara fija el estado, si ninguno dispara el veredicto es `VALID` (`ScoreStateMachine.kt:65-68`, tabla `70-82`). Orden: gates de seguridad (abandono → timebase → jank → banco → gaming → invalidación) **antes** que gates de evidencia (computable → n → cobertura de detector → precisión secundaria → estabilidad) (KDoc `ScoreStateMachine.kt:42-49`). Puntos clave:

- `RT_CRITICAL` con timebase no autoritativo → `DEVICE_UNRELIABLE` (`ScoreStateMachine.kt:72`, `timebaseTrusted` en `:95`).
- Banco contaminado/agotado/pequeño-no-validado → `BANK_CONTAMINATED` (`ScoreStateMachine.kt:74`, `101`; delega en `BankIntegrity.blocksProfileEligibility()`, `ScoreTypes.kt:29-30`, fuente única del set `{CONTAMINATED, HOLDOUT_EXHAUSTED, SMALL_UNVALIDATED}`).
- Anti-gaming sucio → `SUSPECTED_GAMING`; sesión invalidada → `INVALIDATED` (`ScoreStateMachine.kt:75-76`).
- **La velocidad no útil se bloquea:** `RT_CRITICAL` con `secondaryAccuracyGate` que no pasa → `CALIBRATING` (`ScoreStateMachine.kt:80`; `SecondaryGate`, `:17-18` — "fast RT con precisión al azar no es velocidad útil").
- Solo `VALID` puede actualizar el perfil y solo `VALID` puede llevar señal, invariantes forzadas en `init` (`ScoreResult.kt:30-37`) y por `ScoreResultFactory` (`ScoreStateMachine.kt:108-128`).

### 3.3 `MinSampleGate` — muestra mínima por métrica (INFRA-06)

Tabla golden única, con nMin **por métrica** (no un piso general): comparativos (`CONGRUENCY_EFFECT`, `SWITCH_COST`, `PRESSURE_DELTA`, `DUAL_TASK_COST`, `POST_ERROR`) exigen TODAS las celdas + ≥20/celda; `SSRT ≥30`; `BRIER ≥40`; `RCI ≥2`; `BUR ≥12`; `DPRIME`/`COWANS_K`/`ACCURACY_RT_BASE ≥20` (`MinSampleGate.kt:47-60`). `evaluate` **nunca devuelve un número** si no se cumplen celdas/nMin: retorna `InsufficientData` (invariante `null != 0`; `MinSampleGate.kt:33`, `65-75`).

### 3.4 `ProfileUpdateGate` — único puente ScoreResult → perfil (INFRA-06)

Consume **exclusivamente** `ScoreResult` (nunca un `Double` crudo) y produce un `ModuleContribution` **solo si** el veredicto puede actualizar el perfil; si no, `null` (ausencia de update, **nunca un valor bajo**; R-NO-REDUCE) (`ProfileUpdateGate.kt:25-26`). Vive en el módulo profile (dirección de dependencia correcta, profile → core; KDoc `ProfileUpdateGate.kt:12-21`). Detalle crítico de honestidad ya corregido: `dataSufficiency` refleja suficiencia de MUESTRA (`nEffective/minNRequired`), NO estabilidad; un veredicto `VALID` ya pasó `MinSampleGate`, así que suficiencia = 1.0, y la baja `confidence` es lo que mantiene la señal `EMERGING` en vez de `ESTABLISHED` (`ProfileUpdateGate.kt:38-42`, `47`, `55-58`).

### 3.5 `ScoringVersionRegistry` — versionado de fórmulas (§8)

Fuente única de versiones de derivación/scoring (`ScoringVersionRegistry.kt:12-60`; detalle en §8).

### 3.6 `CalibrationPolicy` + `ProfileSessionEligibility` — autoridad de calibración

- `ProfileSessionEligibility`: gate de calidad de evidencia (no un score). Sesión elegible sii `endedAtMs > 0 && (endedAtMs - startedAtMs) >= MIN_DURATION_MS && hasMeasurableEvidence`, con `MIN_DURATION_MS = 120_000L` (`ProfileSessionEligibility.kt:21`, `23-26`).
- `CalibrationPolicy`: única sede de la cláusula "≥N sesiones válidas cross-session". `EMERGING_MIN_SESSIONS = 2`; `ESTABLISHED_MIN_SESSIONS = 5`; `ESTABLISHED_MIN_DISTINCT_DAYS = 3` (`CalibrationPolicy.kt:44`, `47`, `50`). `applyCeiling` **solo baja** el estado candidato, nunca lo sube (`CalibrationPolicy.kt:74-81`).

### 3.7 El aggregator — `compositeIndex`

`MindBoostProfileAggregator.aggregate` es una **función pura** que pliega `List<ModuleContribution>` en un `MindBoostProfile` (`MindBoostProfileAggregator.kt:32-47`). Por dimensión, filtra contribuciones cualificantes (`normalizedScore != null && dataSufficiency >= 0.5 && confidence >= 0.4`; `MindBoostProfileAggregator.kt:53-57`), calcula una media ponderada por `confidence·dataSufficiency` (`60-65`), deriva estado por confianza (`ESTABLISHED` si `confidence >= 0.7`, si no `EMERGING`; `68-72`) y aplica el techo de `CalibrationPolicy` para fuentes con `sessionEvidence` (`76-78`). Sin contribución cualificante → `CALIBRATING` con valor `null`, **nunca un 0 silencioso** (`58`, `79-80`). El `compositeIndex` se computa **solo** sobre dimensiones `ESTABLISHED`, requiere `>= 3` (`minEstablishedForComposite = 3`, `AggregationConfig.kt` inline `MindBoostProfileAggregator.kt:22`) y es `null` si no las hay (`95-114`).

---

## 4. Tabla: Cláusula PRD → Implementación real

| Cláusula (contrato de producto) | Cómo se cumple hoy | Tipo | file:line | Estado |
|---|---|---|---|---|
| **Score 0-100 por bloque** | `UserDisplayScoreMapper`: `0..1 → 0-100` lineal, redondeado y acotado; solo se muestra entero si el gate DC-2 pasa; si no, `—` | Equivalencia superior (mapeo + gate de display, no un número siempre) | `UserDisplayScoreMapper.kt:66-72`, `81-82` | Cumple |
| **Global Score comprensible** | `compositeIndex` = media simple de dimensiones `ESTABLISHED`; UI lo muestra `·100` redondeado o `—` | Equivalencia superior (media honesta, no media ciega de 8) | `MindBoostProfileAggregator.kt:95-114`; `BrainProfileUiModelBuilder.kt:84-105` | Cumple |
| **Calibración antes del rango oficial** | Estado `ESTABLISHED` gateado por confianza (`>=0.7`) y por `CalibrationPolicy` (≥5 sesiones válidas / ≥3 días) para fuentes de sesión | Equivalencia superior (dos ejes: confianza + evidencia cross-session) | `MindBoostProfileAggregator.kt:68-78`; `CalibrationPolicy.kt:66-81` | **Parcial** (DC-3 solo activo para Enfoque/Lógica; ver nota) |
| **Progreso medible** | `DimensionState.trend` (`IMPROVING/STABLE/DECLINING/UNKNOWN`) del contribuyente de mayor confianza; progreso de calibración `dataSufficiency·100` mientras `CALIBRATING` | Equivalencia superior (tendencia + progreso a la primera lectura) | `MindBoostProfileAggregator.kt:66`, `87`; `BrainProfileUiModelBuilder.kt:55-59`, `77-82` | Cumple |
| **La velocidad NUNCA se premia si destruye la precisión** | Lógica: la velocidad solo resta vía impulsividad; máquina de estados: `RT_CRITICAL` con precisión al azar → `CALIBRATING` (no VALID); Decisiones: IED penaliza RT impulsivo | Literal (regla explícita del PRD, implementada) | `ReasoningScorer.kt:19`, `39`, `48-52`; `ScoreStateMachine.kt:80`; `ScoringEquations.kt:89` | Cumple |
| **Protección contra volatilidad** | `EMERGING` con valor puede excluirse del Global Score; `ESTABLISHED` exige confianza alta y evidencia cross-session; ponderación por confianza·suficiencia | Equivalencia superior (estados + techo de calibración amortiguan sesiones sueltas) | `MindBoostProfileAggregator.kt:96-99`; `CalibrationPolicy.kt:66-81` | **Parcial** (amortiguación de outliers intra-serie no auditada) |
| **Fórmulas versionadas** | `ScoringVersionRegistry` como fuente única; `scoringVersion` sellado en `ScoreProvenance` | Equivalencia superior (registro central vs strings dispersos) | `ScoringVersionRegistry.kt:12-60`; `ScoreResult.kt:51` | **Parcial** (registro poblado; que TODO scorer lo sella no verificado) |
| **Feedback claro** | `ClaimLevel` gradúa qué puede afirmar la UI; `CopySafetyFlag`; copy funcional/no-clínico + disclaimer; `—` mientras calibra | Equivalencia superior (claim-ceiling + copy scanner INFRA-05) | `ScoreTypes.kt:53`, `56`; `BrainProfileUiModelBuilder.kt:32-33`, `65` | **Parcial** (copy verificado; correspondencia claim↔texto por pantalla no auditada) |

**Nota "calibración/DC-3 (Parcial)":** El techo de `CalibrationPolicy` solo se enforce hoy para fuentes de sesión que emiten `SessionEvidence` — **Enfoque (ATTENTION) y Lógica (REASONING)** (KDoc explícito `CalibrationPolicy.kt:27-39`). Lenguaje (no cableado en producción) y Decisiones (vía `ProfileUpdateGate`, hoy limitado a EMERGING por confianza) aún no emiten evidencia; los módulos de acumulación de trials (Memoria) no tienen concepto de sesión y quedan fuera por diseño. La afirmación permitida es "DC-3 enforced para Enfoque/Lógica", **nunca** "DC-3 global".

---

## 5. Cómo el modelo produce un score 0-100 por bloque

El mapeo exacto vive en `UserDisplayScoreMapper` (módulo `profile/domain/display`):

1. Única entrada visible: `ProfileDimensionSignal`, cuyo constructor es **privado**; solo `fromDimensionState` lo crea, y devuelve `null` si `DimensionState.value == null` (CALIBRATING, feedback procedural, o score interno inválido) (`UserDisplayScoreMapper.kt:22-26`, `29-32`). Un score crudo/procedural no tiene ruta accidental a un número visible: es imposible por construcción (KDoc `UserDisplayScoreMapper.kt:8-13`).
2. Gate DC-2 — método `displayScore` (`UserDisplayScoreMapper.kt:66-72`):
   - `signal == null` → `Withheld` → UI muestra **`—`** (nunca 0).
   - `ESTABLISHED` → `Official(toPercent(normalized))`.
   - `EMERGING && confidence >= DISPLAY_CONFIDENCE_GATE (0.55)` → `Provisional(toPercent(normalized))`.
   - resto → `Withheld` (`—`).
3. `toPercent(normalized) = (normalized * 100).roundToInt().coerceIn(0, 100)` (`UserDisplayScoreMapper.kt:81-82`).

Los tres resultados del gate son un `sealed interface DisplayScore` (`Official`/`Provisional`/`Withheld`, `UserDisplayScoreMapper.kt:37-46`). La UI traduce `Withheld` a `displayValue = "—"` y `score0to100 = null` — nunca "0" (`BrainProfileUiModelBuilder.kt:48-52`, `65`). Mientras `CALIBRATING`, en vez de un `—` desnudo, se muestra progreso hacia la primera lectura (`dataSufficiency·100`) (`BrainProfileUiModelBuilder.kt:55-59`, `77-82`).

**Trato de CALIBRATING (crítico):** un bloque/dimensión en calibración **muestra `—`, nunca 0**. Esto es la invariante `null != 0` llevada hasta el píxel.

---

## 6. Cómo `compositeIndex` se convierte en el Global Score visible

`summarize` (`MindBoostProfileAggregator.kt:95-114`):

- Toma **solo** dimensiones `ESTABLISHED` con `value != null` (`96-98`).
- Si hay `< 3` (`minEstablishedForComposite = 3`, `MindBoostProfileAggregator.kt:22`) → `compositeIndex = null` (`99-106`).
- Si hay `>= 3` → `compositeIndex = media simple de esos valores` (`107`); `strongest`/`developing` son la mayor/menor dimensión establecida (`111-112`).

La UI lo pinta como `(composite·100).roundToInt()` o `—` si es null (`BrainProfileUiModelBuilder.kt:84-105`).

**Por qué NO es una media ciega de 8:** el índice **excluye** dimensiones `CALIBRATING` (sin valor) y también `EMERGING` **aunque tengan un valor real**. Solo consolida lo consolidado. Esto es exactamente lo que fija el test `composite_averagesOnlyEstablished_excludesEmergingWithAValue`: con 3 `ESTABLISHED` (0.6, 0.4, 0.8) + 1 `EMERGING` con valor 0.2, el índice es `(0.6+0.4+0.8)/3`, nunca `(0.6+0.4+0.8+0.2)/4 = 0.5` (`MindBoostProfileAggregatorTest.kt:121-141`).

---

## 7. Definiciones de producto selladas

- **Perfil provisional** = cualquier dimensión en `CALIBRATING` o `EMERGING`, y/o marcada `preliminary`. En `EMERGING` puede existir un número (si pasa el gate de confianza 0.55, se muestra como "provisional"); en `CALIBRATING` no hay valor y se muestra `—` (`DisplayScore.Provisional`/`Withheld`, `UserDisplayScoreMapper.kt:42`, `45`; `preliminary`, `MindBoostProfileAggregator.kt:89`).
- **Rango oficial** = dimensión `ESTABLISHED`: confianza alta (`>=0.7`) **y**, para fuentes de sesión, evidencia cross-session suficiente (≥5 sesiones válidas / ≥3 días). Solo estas entran al Global Score.

**Por qué una dimensión con datos (EMERGING) puede NO entrar aún en el Global Score:** `EMERGING` significa "hay un valor pero la confianza/evidencia todavía es baja". Incluirla en el índice global lo haría volátil y sobre-afirmaría madurez que no existe. El Global Score se reserva para señal consolidada; la dimensión `EMERGING` sigue siendo visible por sí misma (posiblemente como "provisional"), pero fuera del compuesto (`MindBoostProfileAggregator.kt:96-99`; test en §10).

---

## 8. Versionado de fórmulas activas

`ScoringVersionRegistry` (`ScoringVersionRegistry.kt:12-60`) es la **fuente única** que fija cómo una señal se convierte en valor persistido o visible, para que ningún string de versión esté hardcodeado en call-sites dispersos (KDoc `ScoringVersionRegistry.kt:3-11`):

- **Display:** `USER_DISPLAY_SCORE_MAPPER = "user-display-score-mapper-1.0.0-linear-preliminary"` — el sufijo `-linear-preliminary` deja explícito que el mapa `0..1→0-100` es lineal y preliminar hasta que exista una curva de baseline por bloque (`ScoringVersionRegistry.kt:20`). `UserDisplayScoreMapper.version` lee de aquí (`UserDisplayScoreMapper.kt:62-63`).
- **Por ejercicio (Tier A, persistidas en `ScoreProvenance.scoringVersion`):** `DECISIONS_REVERSIBILITY="e2-reversibility-v1"`, `MEMORY_SPAN="ex1-span-v1"`, `MEMORY_PAIRS="ex4-pairs-v1"`, `MEMORY_DEFERRED="ex3-deferred-v1"` (`ScoringVersionRegistry.kt:25`, `28`, `31`, `34`).
- **Velocidad (pipeline propio, aún no ruteado por `ScoreResultFactory`):** `SPEED_METRICS="1.0.0"`, `SPEED_STAIRCASE="1.0.0-mvp1"`, `SPEED_DIFFICULTY_MODEL="1.0.0"`, `SPEED_SCORE="1.0.0-mvp3-preliminary"` — nótese que solo el score de perfil MVP-3 lleva sufijo `-preliminary` (`ScoringVersionRegistry.kt:36`, `39-48`).
- **Lenguaje (local, sin LLM):** `LANGUAGE_EVALUATOR_STUB="local-stub-1"`, `LANGUAGE_RUBRIC_STUB="local-stub-rubric-1"`, `LANGUAGE_PROJECTION_ESTIMATOR="v6-projector-1"` (`ScoringVersionRegistry.kt:53`, `56`, `59`).

**Dónde se sella por resultado:** cada `ScoreResult` lleva `provenance.scoringVersion` (`ScoreProvenance.scoringVersion`, `ScoreResult.kt:51`; el `ScoreProvenance` es `ScoreResult.kt:45-53`), así que el historial queda etiquetado con la versión que lo produjo.

**Cómo se evoluciona una fórmula sin romper histórico:** se acuña una nueva versión en el registro (p. ej. `ex1-span-v2`) y los resultados nuevos se sellan con ella, mientras los antiguos conservan su etiqueta previa. El registro versiona **derivaciones** (display + scoring por ejercicio), **no** las definiciones psicométricas de métrica (la tabla golden de `MinSampleGate`) ni versiones de esquema de DTO (KDoc `ScoringVersionRegistry.kt:6-10`).

> **Parcial / a verificar (UNVERIFIED):** el registro está poblado, pero no se auditó en este turno que **todos** los scorers de producción realmente lean su versión de aquí y la sellen en `ScoreProvenance`. Verificación pendiente: grep de cada `*ProfileSource`/scorer para confirmar que `scoringVersion` proviene del registro y no de un literal.

---

## 9. Qué se cumple por EQUIVALENCIA SUPERIOR (no literal) y por qué degradar sería un error

La fórmula simple del PRD (`0.30·Acc + 0.20·Speed + ...`) describe una intención; el modelo actual la supera:

1. **Volatilidad.** Una media ponderada por sesión oscila con cada partida. Los estados `CALIBRATING/EMERGING/ESTABLISHED` + el techo de `CalibrationPolicy` amortiguan (`MindBoostProfileAggregator.kt:68-78`; `CalibrationPolicy.kt:66-81`). Degradar a la media literal reintroduciría el ruido que el modelo elimina.
2. **Gaming.** La media literal no distingue rendimiento honesto de explotación. La máquina de estados bloquea gaming, banco contaminado, timebase no fiable y velocidad-al-azar antes de dejar pasar señal (`ScoreStateMachine.kt:70-82`). La fórmula simple no tiene ese filtro.
3. **`null != 0`.** La media literal trataría "sin datos" como 0, castigando al usuario por no haber entrenado. El modelo usa `null` → `—` (`ScoreResult.kt:30-33`; `Signal.value` KDoc `ScoreResult.kt:82`; `UserDisplayScoreMapper.kt:66-72`; `MindBoostProfileAggregator.kt:58`). Degradar rompería la garantía central de honestidad.
4. **Velocidad no premiada.** La cláusula "la velocidad no debe premiarse si destruye precisión" es literal en el PRD y está implementada literalmente (Lógica: velocidad solo resta, `ReasoningScorer.kt:19`, `39`, `48-52`; máquina de estados: RT-crítico con precisión al azar → no VALID, `ScoreStateMachine.kt:80`). La media ponderada simple, en cambio, **sumaría** puntos por velocidad — exactamente lo prohibido.

---

## 10. Invariantes protegidos por tests

- **Global Score honesto (excluye EMERGING con valor):** `composite_averagesOnlyEstablished_excludesEmergingWithAValue` — 3 ESTABLISHED + 1 EMERGING(0.2) → compuesto `(0.6+0.4+0.8)/3`, nunca `/4` (`MindBoostProfileAggregatorTest.kt:121-141`).
- **`null != 0` en agregación:** `emptyContributions_allDimensionsCalibrating_noComposite`, `nullScore_isIgnored_evenWithHighConfidence`, `belowGate_contributionIsIgnored_dimensionStaysCalibrating` (`MindBoostProfileAggregatorTest.kt:27-35`, `70-74`, `62-68`).
- **Umbral de composite (≥3):** `composite_isNullUntilEnoughEstablished_thenAveragesThem` (`MindBoostProfileAggregatorTest.kt:105-119`).
- **EMERGING vs ESTABLISHED por confianza:** `highConfidenceContribution_isEstablished_withItsValue`, `lowConfidenceButQualifying_isEmerging` (`MindBoostProfileAggregatorTest.kt:47-54`, `56-60`).
- **Media ponderada por confianza·suficiencia:** `multipleContributions_useConfidenceSufficiencyWeightedMean` (`MindBoostProfileAggregatorTest.kt:76-85`).
- **Sello temporal desde el reloj inyectado, nunca fabricado:** `nullContributionTimestamp_doesNotBlockAggregation_profileStampComesFromClock` (`MindBoostProfileAggregatorTest.kt:37-45`).

> Los tests de `UserDisplayScoreMapper` (gate DC-2: `—` nunca número indebido; versión anclada al registro), `CalibrationPolicy` (DC-3: ≥2 EMERGING, ≥5/≥3 días ESTABLISHED, cap solo baja) y `ProfileSessionEligibility` (120s + evidencia + timestamp real) existen y respaldan §5, §3.6; **no se releyeron sus líneas exactas en este turno**, así que sus rangos concretos quedan **UNVERIFIED** hasta reabrir `UserDisplayScoreMapperTest.kt`, `CalibrationPolicyTest.kt` y `ProfileSessionEligibilityTest.kt`. Los invariantes de código que prueban SÍ están verificados en `UserDisplayScoreMapper.kt:66-72`, `CalibrationPolicy.kt:44-81`, `ProfileSessionEligibility.kt:23-26`.

---

## 11. Persistencia y decisión CEO de sync (contexto, NO implementado este turno)

Hoy los datos cognitivos se guardan **local-first y cifrados en reposo**: `SecureKeyValueStore` con **AES-256-GCM** y clave en **Android Keystore** (nunca exportada), en `SharedPreferences` privadas (KDoc de clase `SecureKeyValueStore.android.kt:24-34`). Los namespaces cognitivos están centralizados y son `AccountBound` — incluido `mindboost_profile` (el snapshot del perfil, derivado, coarse, local-only) (`MindBoostStorageNamespaces.kt:19`, lista `AccountBound` en `:21-22`). **Los datos cognitivos hoy nunca se suben.**

La decisión CEO de añadir Firestore sync debe respetar: local-first permanece; guardado local inmediato; **ningún trial crudo de alta granularidad en MVP** (solo métricas estructuradas, coherente con `ModuleContribution`, que "NEVER carries raw trials, timings, or anything uploadable", `ModuleContribution.kt:5-6`); reglas estrictas (`auth.uid == userId`, App Check, lecturas acotadas, escrituras validadas, sin acceso cross-user, sin auto-Elite del cliente); consentimiento GDPR; integración con el borrador de cuenta (`mindboost_profile` ya está en `AccountBound`, cubierto por el eraser — ver aprendizaje 2026-06-28 en `CLAUDE.md`); y **el sync nunca debe bloquear el entrenamiento**. Diseño detallado y reglas: **fuera de alcance de este turno** (documentación only).

---

## 12. Sello final

**La fórmula simple del PRD es una BASELINE CONCEPTUAL, NO una orden para degradar el modelo actual.** El motor riguroso — scorers bespoke, `ScoreStateMachine`, `MinSampleGate`, `ProfileUpdateGate`, `ScoringVersionRegistry`, `CalibrationPolicy` y el `compositeIndex` sobre solo-ESTABLISHED — se preserva íntegro. Cumple el contrato de producto por equivalencia superior; degradarlo reintroduciría volatilidad, abriría gaming y rompería `null != 0`.