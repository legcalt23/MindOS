# MindBoost Scoring & Measurement — Master Plan RECONCILED

> **Estatus:** este documento **SUPERSEDE** al plan untracked
> `.antigravity-only/plans/2026-07-08-scoring-master-plan/MINDBOOST_SCORING_AND_MEASUREMENT_MASTER_PLAN.md`.
> El blueprint matemático de ese plan es valioso y se conserva; su sección **"Estado actual" es
> OBSOLETA** respecto al código real y queda corregida aquí, archivo por archivo.
>
> **Rol de revisión:** las revisiones psicométrica (matemática) y de eficacia (validez de constructos /
> claim-safety) de esta tanda se realizaron **in-role por el operador** (CEO + Principal Product Engineer
> + Staff KMP + psicómetra), porque el enrutado a los subagentes `psychometrician`/`efficacy-guardian`
> fue declinado en esta sesión. Ambas revisiones son re-ejecutables por esos subagentes bajo petición; sus
> veredictos resumidos están en §7.
>
> **Fecha:** 2026-07-08 · **Rama base auditada:** `d086c303`

---

## 1. Veredicto ejecutivo (corregido)

El plan afirma que "el scoring NO está listo para MVP" y que "faltan los adaptadores concretos que conectan
la interacción con el `ScoreResult` y la persistencia del perfil histórico". **Esto es parcialmente falso
a día de hoy.** La verdad verificada en el repo:

- La **infraestructura de honestidad (INFRA-01..06) está COMPLETA y viva**: `ScoreResult` sella los
  invariantes `null≠0` y `R-NO-REDUCE` en `init`; `ScoreStateMachine` es una tabla de gates pura;
  `MinSampleGate` es la tabla golden de nMin; `BankIntegrity.blocksProfileEligibility()`,
  `DetectorCoverage`, `TimebaseQuality` y el framework anti-gaming (11 detectores + red-team) existen.
- El **agregador de perfil y el gate ya están cableados**: `MindBoostProfileAggregator` (puro),
  `ProfileUpdateGate` (único puente `ScoreResult → ModuleContribution`), `CalibrationPolicy` y
  `ProfileSessionEligibility`. Hay **NUEVE `ProfileSource` cableados** en `profile/data/adapter/`.
- Lo que **falta de verdad** NO son los contratos ni el agregador, sino:
  1. Un **kernel de normalización universal** (winsorize / sigmoide / ajuste por dificultad / combinador
     ponderado / Regla-de-Techo) del que hoy no hay una implementación central reutilizable.
  2. **Mappers `ScoreResult` ricos por ejercicio** que poblen `primarySignal` + `secondarySignals` con
     honestidad (varios módulos quedan `CALIBRATING` por mappers stub o por falta de banco validado).
  3. El **lifecycle de calibración por bloque** y el **score histórico suavizado (EMA)** — pero
     **mapeados a los estados que YA existen**, no como un sistema paralelo.
  4. Un **DTO acotado para la IA Coach** (fuera de MVP, tras guards).
  5. Una **suite de validación tipo Monte Carlo** sobre el kernel y los gates.

**Corolario:** el trabajo NO es "construir el motor de scoring desde cero"; es **completar el kernel de
normalización + poblar mappers honestos por módulo + mapear el lifecycle a lo existente**, sin duplicar
autoridades ni fabricar certeza.

---

## 2. Auditoría CORREGIDA (qué existe ya vs qué falta)

### 2.1 Núcleo `core/scoring/` — EXISTE (verificado)

| Símbolo | Archivo | Realidad (corrige al plan) |
|---|---|---|
| `ScoreResult` | `core/scoring/ScoreResult.kt` | Impone en `init`: `VALID ⟺ primarySignal!=null` y `estado≠VALID ⇒ !updatesProfile`. `Signal.value: Double?` con `unit ∈ {MILLISECONDS,RATIO,COUNT,DPRIME,INDEX,Z_SCORE}` → **ya soporta N métricas tipadas** (secondarySignals ricos son viables sin cambios de modelo). |
| `ScoreProvenance.scoringVersion` | mismo | **La versión de scoring vive aquí** (no en SessionRecord). Confirma resolución del ledger (a). |
| `DataQuality.invalidatedBy: InvalidationReason?` | mismo | **Las anomalías viven aquí** (`ANTI_GAMING/QUALITY_LOW/AGRS_HIGH/CLOCK_DISCONTINUITY/OTHER`), no en SessionRecord. |
| `ScoreState` | `core/scoring/ScoreState.kt` | 8 estados; `VALID` es el ÚNICO que actualiza perfil; el resto renderiza `—`, nunca 0. |
| `ScoreStateMachine` / `ScoreResultFactory` | `core/scoring/ScoreStateMachine.kt` | Tabla ordenada de gates `(state, claim, predicate)`; safety→evidence→VALID. `ScoreResultFactory` es el único constructor. |
| `MinSampleGate` + `ScoreMetricId` | `core/scoring/MinSampleGate.kt` | Tabla golden nMin (Comparativos ≥20/celda+allCells; SSRT ≥30; Brier ≥40; ACCURACY_RT_BASE ≥20…). `InsufficientData` **nunca es un número**. |
| `ScoringVersionRegistry` | `core/scoring/ScoringVersionRegistry.kt` | Versiona derivaciones (display mapper + per-exercise scoring), NO las definiciones psicométricas. |
| `BankIntegrity` / `DetectorCoverage` / `TimebaseQuality` | `core/scoring/ScoreTypes.kt` | `blocksProfileEligibility()` es la única fuente del set bloqueante; `DetectorCoverage.adequateFor(criticality)` (RT_CRITICAL exige ADEQUATE). |
| Anti-gaming (framework + 11 detectores) | `core/antigaming/**` | Existe con red-team; wiring por-ejercicio parcial (INFRA-03). |
| Bank validation | `core/bank/**` | `BankValidator` + `BankIntegrityAuthority` (registro opt-in vacío, fail-closed a `SMALL_UNVALIDATED`). |
| Timing | `core/timing/**` | `FrameStatsCalculator` (puro) + `FrameTimingProbe` (expect/actual). |
| `SessionRecord` | `core/session/SessionRecord.kt` | Lifecycle + contadores gruesos. **SIN `scoringVersion`/`anomalyFlags` — por diseño** (minimización; "It is NOT a second scoring authority"). |

### 2.2 Perfil `profile/` — EXISTE (verificado)

- `MindBoostProfileAggregator` (PURO, `aggregate(userId, List<ModuleContribution>, nowMs)`), con
  `AggregationConfig(minSufficiency=0.5, minConfidence=0.4, establishedConfidence=0.7,
  minEstablishedForComposite=3)`. Una dimensión sin contribución cualificante queda `CALIBRATING` con
  `value=null` (nunca 0). El composite (`OverallSummary.compositeIndex`) exige ≥3 dimensiones
  ESTABLISHED, si no `null`.
- `ProfileUpdateGate` (único puente): `contributionFor(result) = if (canUpdateProfile) toModuleContribution() else null`.
  **`dataSufficiency = nEffective/minNRequired` (muestra) ≠ `confidence` (estabilidad)** — separados, con
  KDoc que documenta que confundirlos fue un bug real (learning 2026-06-28).
- `ModuleContribution`: `normalizedScore: Double?` (0..1 o null), `dataSufficiency` y `confidence`
  ortogonales, `updatedAtMs: Long?` (null = desconocido, nunca 0L fabricado).
- `CalibrationPolicy`: `EMERGING_MIN_SESSIONS=2`, `ESTABLISHED_MIN_SESSIONS=5` + `ESTABLISHED_MIN_DISTINCT_DAYS=3`;
  `applyCeiling` capa la madurez por evidencia cross-session (DC-3, hoy solo Enfoque/Lógica emiten evidencia).
- `ProfileSessionEligibility`: `endedAtMs>0 && span≥120_000ms && hasMeasurableEvidence`.
- **DimensionStatus real** = `{CALIBRATING, EMERGING, ESTABLISHED}` (3 estados, no 5).

### 2.3 Los NUEVE ProfileSources cableados

`profile/data/adapter/`: **Focus, Speed, Memory, Pairs, Episodic, Logic, Language, Emotion, Decisions**.
El único módulo **SIN adapter** es **Creatividad**.

Estado de honestidad conocido (memoria/aprendizajes del repo):
- `Memoria` alimenta el perfil vía gate pero con banco `SMALL_UNVALIDATED` → CALIBRATING por diseño
  (registro de bancos opt-in vacío).
- `Language` (VERBAL): adapter existe pero **evaluador stub** (`LANGUAGE_EVALUATOR_STUB`) y **store de
  producción sin cablear** → CALIBRATING. **Candidato Fase B** (mapper honesto).
- `Decisiones`: EMERGING-capped por confidence; E2-Reversibility usa placeholder → CALIBRATING.
- `Speed` (PROCESSING_SPEED): entra solo con timebase autoritativo (Android API≥34); score MVP-3
  "preliminary".
- El resto contribuye según sus gates.

### 2.4 GAPs REALES (lista corregida, prioridad de ejecución)

1. **Kernel de normalización universal** — no existe una implementación central pura y reutilizable
   (winsorize / sigmoide / difficultyAdjust / combinador ponderado + Regla-de-Techo). **→ Fase A.**
2. **Fórmula de confianza inter-sesión (CL) rota** en el plan (§8.2): puede dar negativo / dividir por 0.
   **→ Fase A (función pura + tests; cableado en Fase C).**
3. **Mappers `ScoreResult` honestos por módulo** que pueblen `primarySignal` + `secondarySignals`. **→ Fase B.**
4. **Lifecycle de calibración por bloque + EMA histórico**, mapeados a `DimensionStatus` + `CalibrationPolicy`
   existentes. **→ Fase C.**
5. **GlobalScore** honesto (≥5 bloques calibrados ⇒ oficial; si no, `—`). Rangos/decay = **PROPUESTA**. **→ Fase D.**
6. **`CognitiveSummaryDto`** acotado, PII-free, para IA Coach (tras guards). **→ Fase E.**
7. **Suite Monte Carlo** de perfiles sintéticos (robot/spammer/impulsivo/fatigado/honesto). **→ Fase F.**

---

## 3. LEDGER DE CONFLICTOS (plan pegado ⟷ código real)

Una entrada por conflicto, con resolución razonada. Todos son **resolubles con criterio** → no hay bloqueo
de dirección de producto que requiera decisión CEO ANTES de Fase A (el único punto que sí requiere sign-off
CEO —rangos/decay— se difiere explícitamente y NO se implementa; ver (e)).

### (a) `SessionRecord.scoringVersion` / `anomalyFlags` — **RECHAZADO**
- **Plan (§4.1):** añadir `scoringVersion: String` y `anomalyFlags: Set<String>` a `SessionRecord`.
- **Código real:** `SessionRecord` los excluye **a propósito** — su KDoc: *"It is NOT a second scoring
  authority… version provenance is attached in task 2.3, never a duplicate score"*, y minimización de datos.
- **Resolución:** la **versión de scoring vive en `ScoreProvenance.scoringVersion`**; las **anomalías viven
  en `ScoreResult.dataQuality.invalidatedBy`** (+ el framework anti-gaming). **NO se modifica `SessionRecord`.**

### (b) Quality/anti-gaming "server-side" — **REENCUADRADO a on-device**
- **Plan (§10):** "comprobaciones server-side".
- **Código real:** todo el scoring, calidad y anti-gaming corren **on-device** (`core/scoring`,
  `core/antigaming`, `core/timing`). El sync a Firestore está **OFF/HOLD** (flag apagado).
- **Resolución:** toda mención "server-side" se lee como **on-device**. Ninguna fase de este trabajo
  enciende Firestore/sync.

### (c) Fórmula de confianza CL — **CORREGIDA (era un bug real)**
- **Plan (§8.2):** `CL = (1 − SD/Mean)(1 − e^{−0.15n})`.
- **Defecto:** `CV = SD/Mean` puede superar 1 ⇒ `(1−CV)` negativo ⇒ **CL negativo**; y `Mean=0` ⇒
  **división por cero**.
- **Resolución (Fase A):** función pura `interSessionConfidence(recentScores)` con **clamp a [0,1]**,
  guard `n<2 ⇒ 0.0` (dispersión indefinida; honesto, no fabricado), guard `mean≤0 ⇒ 0.0` (fail-closed),
  `dispersionTerm=(1−cv).coerceIn(0,1)`, `sampleTerm=1−e^{−0.15n}`, `CL=(dispersion·sample).coerceIn(0,1)`.
  Extremos validados por test (n=0⇒0; CV>1⇒0; CV=0 & n grande⇒→1). **Limitación conocida** (§7): CV es
  sensible a la escala (un rendimiento bajo-pero-estable con media pequeña se penaliza); la evaluación de
  una dispersión normalizada-por-escala se **difiere a Fase C** antes del cableado. **NO se conecta a
  producción en esta tanda** (evitaría reclasificar perfiles existentes).

### (d) `dataSufficiency` vs `confidence` — **MANTENER ORTOGONALES**
- **Plan:** mezcla ambos conceptos en el "nivel de confianza".
- **Código real:** son campos separados en `ModuleContribution`; `ProfileUpdateGate` deriva
  `dataSufficiency` de la muestra y `confidence` de la estabilidad; confundirlos fue un bug (learning
  2026-06-28).
- **Resolución:** el CL corregido es una señal de **confianza/estabilidad**; **jamás** se mapea a
  `dataSufficiency`. El "lifecycle de calibración" (PROVISIONAL/CALIBRATING/CALIBRATED/HIGH_CONFIDENCE/STALE)
  se **MAPEA** a `DimensionStatus{CALIBRATING,EMERGING,ESTABLISHED}` + `CalibrationPolicy` +
  `ProfileSessionEligibility` (ver tabla §4), **sin** crear un segundo sistema.

### (e) GlobalScore ponderado, ladder de rangos, decay/STALE — **PROPUESTA (sign-off CEO), NO se implementa**
- **Plan (§21):** GlobalScore CL-ponderado, ladder Bronce…Leyenda, penalización por inactividad (−½ CL a 30
  días, −1 rango a 60).
- **Riesgo de claim-safety (§7-efficacy):** un rango único o un "GlobalScore" puede leerse como score de
  habilidad/IQ; y un **decay de rango cognitivo por inactividad implica una afirmación de pérdida de
  capacidad de la persona** (health-adjacent) — inaceptable sin reencuadre.
- **Resolución:** se marcan **PROPUESTA**. **En esta tanda NO se implementan penalizaciones de retención ni
  decaimiento de rango.** Si algún día se implementa: el decay solo puede afectar la **recencia/confianza de
  la MEDIDA** ("requiere recalibración"), **nunca** aseverar pérdida de habilidad; los rangos son
  gamificación de la práctica, no certificación cognitiva. **Requiere decisión CEO explícita antes de Fase D.**

### (f) Lifecycle 5-estados del plan ⟷ 3-estados del repo — **COLAPSAR (más seguro)**
- **Resolución:** ver mapa en §4. Colapsar 5→3 es **una sola fuente de verdad**; no se pierde ninguna
  distinción relevante para la honestidad (PROVISIONAL≈CALIBRATING+`preliminary`; HIGH_CONFIDENCE≈ESTABLISHED
  con confidence alta; STALE≈concern de recencia, no implementado).

---

## 4. Mapa lifecycle del plan → estados existentes (sin sistema paralelo)

| Estado del plan (§8.1) | Se mapea a (repo real) | Cómo |
|---|---|---|
| UNSEEN | (ausencia de contribución) | Dimensión sin `ModuleContribution` cualificante ⇒ `DimensionState.calibrating` (`value=null`). |
| PROVISIONAL | `CALIBRATING` + `preliminary=true` | Señal computable pero no calibrada; `ScoreResult.preliminary`. |
| CALIBRATING | `CALIBRATING` | `DimensionStatus.CALIBRATING`. |
| CALIBRATED (≥5) | `EMERGING`/`ESTABLISHED` | `CalibrationPolicy`: ≥2 sesiones→EMERGING; ≥5 & ≥3 días→ESTABLISHED. |
| HIGH_CONFIDENCE (≥15) | `ESTABLISHED` + confidence≥0.7 | `AggregationConfig.establishedConfidence`. |
| STALE (>30d) | (no implementado) | Concern de recencia; **propuesta**, ver ledger (e). |

---

## 5. MAPA DE FASES (rebanadas pequeñas, cada una con su gate)

| Fase | Alcance | Estado | Gate |
|---|---|---|---|
| **A** (esta tanda) | Kernel de normalización universal (puro) + fix CL (función pura, sin cablear). | **EN CURSO** | `:shared:desktopTest` + `:shared:detekt` (+ copy-scanner, no-op: fuera de scope). |
| **B** | Mappers `ScoreResult` por módulo (poblar `primarySignal` + `secondarySignals` ricos). **Empezar por el módulo CALIBRATING por mapper stub (candidato: Lenguaje); Creatividad AL FINAL** (requiere diccionarios locales, mayor riesgo). | Pendiente (luz verde CEO) | desktopTest + detekt + copy-scanner de la variante del source-set tocado. |
| **C** | Lifecycle de calibración por bloque + EMA de score histórico, **mapeados** a `DimensionStatus`/`CalibrationPolicy`. Aquí se decide la dispersión del CL y se cablea. | Pendiente | desktopTest + detekt; re-verificar que no reclasifica perfiles inesperadamente. |
| **D** | GlobalScore honesto (≥5 calibrados ⇒ oficial; si no `—`). **Rangos/decay = PROPUESTA, sign-off CEO.** | Pendiente + decisión CEO | desktopTest + detekt. |
| **E** | `CognitiveSummaryDto` acotado, PII-free, para IA Coach (tras guards; no-MVP). | Pendiente | desktopTest + copy-scanner. |
| **F** | Suite Monte Carlo (robot/spammer/impulsivo/fatigado/honesto) sobre kernel + gates. | Pendiente | desktopTest (+ koverVerifyDesktop opcional). |

---

## 6. Fase A — diseño del kernel (implementado en esta tanda)

Ubicación: `core/scoring/normalization/` (paquete neutral, sin deps de feature ni de plataforma; fuera del
scope del copy-scanner). Funciones **puras, deterministas, acotadas a [0,1]**.

- **`ScoreNormalization`** (`ScoreNormalization.kt`):
  - `winsorizeRt(rtMs, minPhysMs=120.0, maxPhysMs=2500.0)` — coerce a límites biológicos. **Solo para
    métricas de tendencia central**; el conteo de eventos extremos (lapsos/anticipaciones) usa RT crudos
    (learning 2026-06-04). `require(minPhys in 0..maxPhys)`.
  - `sigmoid(x, x0, k)` — logística `1/(1+e^{−k(x−x0)})`, `require` inputs finitos, output coerce [0,1].
    Dirección por signo de `k` (RT "menor es mejor" ⇒ k negativo), documentado.
  - `difficultyAdjust(normalized, difficultyActual, difficultyMax)` — envelope multiplicativo
    `[0.5,1.0]`, `require(difficultyMax>0)`, `ratio` coerced [0,1], output coerce [0,1].
  - `blockSessionScore(components, weights, minAccuracyForSpeed)` — combinador ponderado A/Sd/D/C/E.
    **Regla-de-Techo (Rule-No-Speed) como GATE DURO**: `accuracy < minAccuracyForSpeed ⇒ wSpd efectivo = 0`.
    Combinación `score = Σ(effW_i·C_i) / Σ(baseW_i)` (el peso de velocidad anulado **permanece en el
    denominador ⇒ penaliza**, no renormaliza — anti-gaming: renormalizar premiaría a un spammer de RT
    constante con alta "consistencia"). Devuelve `(score, appliedWeights, speedSuppressed)` para
    trazabilidad. `require(baseSum>0 && nonSpeedSum>0)`.
- **`InterSessionConfidence`** (`InterSessionConfidence.kt`): fix del CL (ledger (c)). Pura, **no cableada**.

Todos los outputs `[0,1]`. Entrada insuficiente ⇒ `0.0` **solo** donde `0.0` es el valor honesto de
"sin confianza aún" (n<2, media≤0); el kernel de normalización nunca fabrica un score de rendimiento — eso
es responsabilidad del gate del `ScoreStateMachine` (que produce `null`/estado no-VALID).

---

## 7. Veredictos de revisión (in-role: psicómetra + eficacia)

### 7.1 Psicómetra (matemática) — **VEREDICTO: APROBADO CON CAMBIOS (ya incorporados)**

1. **Winsorize 120/2500 ms:** OK. Límites por-tarea, expuestos como parámetros. Recordatorio incorporado:
   winsorize es **solo para tendencia central**; lapsos/anticipaciones se cuentan sobre crudos.
2. **Sigmoide:** OK. En IEEE-754 con inputs **finitos**, `exp()` nunca produce NaN (`+Inf⇒0`, `0⇒1`); se
   añade `require` de finitud + coerce [0,1]. La dirección por signo de `k` es aceptable **con KDoc
   explícito** (incorporado).
3. **difficultyAdjust:** envelope `[0.5,1.0]` monótono en `ratio`, acotado; la composición con la sigmoide
   preserva [0,1]. OK.
4. **blockSessionScore:** el denominador = suma de pesos BASE garantiza `score ∈ [0,1]`
   (`numerador ≤ Σ effW ≤ Σ baseW = denominador`). **Confirmado:** mantener el peso de velocidad anulado en
   el denominador (penalizar) es la elección anti-gaming correcta; **renormalizar premiaría al spammer** de
   RT constante (baja variabilidad ⇒ alta consistencia). Residual: la propia "consistencia" es gameable por
   tapping constante, pero la accuracy baja arrastra el score y el anti-gaming (INFRA-03) lo cubre aparte
   (documentado).
5. **CL:** el fix (clamp + guards) es correcto y necesario. **SD muestral (n−1)** apropiada con guard n≥2
   (conservadora ⇒ infra-afirma, correcto). **Limitación relevante:** CV es sensible a la escala (media
   pequeña ⇒ CV explota ⇒ penaliza a un usuario bajo-pero-estable). **Recomendación:** evaluar en **Fase C**
   una dispersión normalizada por la ESCALA del score (p. ej. `SD` sobre el rango [0,1]) antes de cablear.
   **Ortogonalidad confirmada:** CL es confianza/estabilidad; **nunca** `dataSufficiency`.
6. **Downstream (Fase C/D):** EMA `α=0.15·CL·(dur/600000)` debe **clampar `dur/600000 ≤ 1`** (una sesión
   >10 min no debe pesar más que la base) ⇒ `α ∈ [0, 0.15]`, estable. GlobalScore CL-ponderado: correcto,
   pero **si `Σ CL_i → 0` debe devolver `—` (null), nunca 0**. (Ambos son Fase C/D; documentados, no
   implementados.)

### 7.2 Eficacia (validez de constructo / claim-safety) — **VEREDICTO: APROBADO CON GUARDRAILES**

- **A. Rule-No-Speed:** control de speed-accuracy tradeoff **defendible y no acusatorio** (retiene crédito,
  no acusa). Guardrail: `aMin` per-block, ≥ nivel de azar; el retiro de crédito nunca se muestra como
  acusación (copy neutro).
- **B. Difficulty weighting:** aceptable como **rendimiento dentro de la tarea bajo dificultad adaptativa**;
  el suelo 0.5 es honesto **solo si la dificultad es genuinamente adaptativa** (no elegida por el usuario) y
  el banco rota (INFRA-04) para mitigar practice-effect. **Nunca** framear como "habilidad" ni transferencia.
- **C. Lifecycle 5→3:** colapsar es **más seguro** (una fuente de verdad); no se pierde distinción de
  honestidad. OK.
- **D. GlobalScore / rangos / decay:** un score/rango único **riesgo de leerse como IQ/habilidad**.
  Guardrails obligatorios si algún día se implementa: (1) etiquetar como **índice de práctica dentro de la
  app**, no norm-referenced de habilidad; (2) rangos = gamificación de práctica; (3) **el decay solo puede
  afectar la recencia/confianza de la MEDIDA, jamás aseverar pérdida de capacidad de la persona**. → marcado
  PROPUESTA, **no implementado**; **requiere sign-off CEO**.
- **E. Bloques de mayor riesgo:** **Emociones** (nunca diagnóstico ni estado clínico; abandono ya
  `ABANDONED_SAFE`, no penaliza) y **Creatividad** (originalidad por diccionarios locales es arbitraria y
  culturalmente sesgada; framear como fluidez/flexibilidad **dentro de la tarea**, nunca "talento/genio").
  Lenguaje ("riqueza léxica") tiene sesgo educativo/SES — cap a `TASK_PERFORMANCE`.
- **F. Prerrequisitos de un mapper honesto** antes de salir de CALIBRATING: (1) `primarySignal`
  genuinamente computable de evidencia real (no generador placeholder/degenerado); (2) banco validado
  (`BankIntegrity` no bloqueante); (3) timebase autoritativo si RT-crítico; (4) anti-gaming testeado con
  call-site vivo (`DetectorCoverage` adecuado); (5) `MinSampleGate` satisfecho; (6) el constructo medido ES
  honestamente la dimensión que alimenta. Todos ya los impone el `ScoreStateMachine`; el mapper debe
  alimentarlo con verdad.

**Conflicto que requiere decisión CEO (no bloquea Fase A):** rangos + decay por inactividad (ledger (e)) —
diferido y no implementado; pedir sign-off antes de Fase D.

---

## 8. Restricciones de honestidad (charter) aplicadas

- `null ≠ 0`: entrada insuficiente/no válida ⇒ CALIBRATING/`—`/null, nunca 0 fabricado.
- El perfil se actualiza **solo** vía un `ScoreResult` sellado que pasa sus gates; el kernel **no** abre un
  segundo canal.
- Todo detector anti-gaming cuenta solo con call-site vivo + red-team.
- Sin claims médicos/clínicos/diagnósticos ni de IQ/g; léxico prohibido impuesto por `ForbiddenLexicon.kt`
  + scanners de build.

---

## 9. Verificación (gates de Fase A)

Kernel = `commonMain` puro ⇒ **no** requiere `:androidApp:compileDebugKotlin` ni iOS. Comandos:

```
.\gradlew.bat :shared:desktopTest
.\gradlew.bat :shared:detekt
.\gradlew.bat :shared:mindboostCopySafetyScanner   # no-op: core/scoring fuera de scope
```

(Si Android Studio posee el daemon Gradle de este worktree, ejecutarlos tú; el operador verifica
estáticamente y no invoca Gradle por CLI.)

---

## 10. Non-goals de la tanda Fase A

No mappers por módulo, no GlobalScore, no rangos, no decay, no `CognitiveSummaryDto`, no tocar
Firestore/sync, no modificar `SessionRecord`, no reclasificar perfiles existentes en producción.
