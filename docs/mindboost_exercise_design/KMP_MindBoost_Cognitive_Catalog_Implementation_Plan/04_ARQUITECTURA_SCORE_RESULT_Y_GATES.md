# 04 — Arquitectura de scoring universal: `ScoreResult` + gates de honestidad

> **Fuentes leídas:** `INFRA_P0.md`, `AUDIT_V2_TECNICO.md`, `AUDIT_V2_GROUND_TRUTH.md`.
> **Marcado de confianza:** `[hecho]` verificado en ground-truth/repo · `[inferido]` deducido de las
> fuentes · `[por_confirmar]` no presente en las fuentes, marcado `POR_CONFIRMAR_EN_REPO`.
> Rutas no verificadas en las fuentes → `ruta probable`.

---

## 0. Invariantes que gobiernan TODO este documento

Estas reglas se repiten en cada documento del plan donde apliquen. Son no negociables.

1. **`null != 0`** `[hecho]`: ningún estado no-`VALID` se serializa, agrega ni renderiza como un
   número (ni 0, ni percentil bajo). La UI lo pinta como `—` / "calibrando" / "datos insuficientes".
   Ya se respeta hoy vía `DimensionState.require((CALIBRATING)==(value==null))`,
   `MetricValue.unavailable()`, barras `Double?` y aggregator que deja `CALIBRATING` con value null
   `[hecho, GROUND_TRUTH §scoring]`.
2. **Enfoque y Velocidad NO actualizan perfil** hasta tener timebase autoritativa **y** jank-gate real
   (`frameDropFraction` **medido**, no el `0.0` hardcoded actual de
   `SpeedSessionStateHolder.kt:259-260`) `[hecho, GROUND_TRUTH §timing]`. Hasta entonces su veredicto
   es jugable pero `updatesProfile=false`.
3. **Cero claims** de far-transfer / clínico / IQ / fake-news / regulación emocional — **ni siquiera
   para negarlos** dentro de un constructo medible. El copy honesto vive en el allowlist de la política
   de copy (doc 13 + INFRA-5).
4. **Anti-gaming solo cuenta si está implementado+testeado** con perfiles sintéticos adversarios
   (regla no negociable #2). Un detector "diseñado" no degrada el claim como si existiera: se modela
   como **cobertura ausente** que **bloquea `VALID`**, nunca como capacidad fingida.
5. **Banco pequeño / no validado bloquea perfil** `[hecho]`: Logic(6), Language(36), Creativity(14),
   Emotions SEPARATOR(40 con holdout fantasma) son memorizables/sin validar
   `[hecho, GROUND_TRUTH §banks]` → su veredicto no puede ser `VALID` consolidable.

---

## 1. Principio de capas — por qué un tipo nuevo NO rompe nada `[inferido de INFRA_P0]`

El repo tiene **4 estilos incompatibles** de "estado de score" `[hecho, GROUND_TRUTH §scoring]`:

| Estilo | Módulo | Tipo real |
|---|---|---|
| Banda + `confident` | Focus | `FocusScore(value: Double?, lowerBound?, upperBound?, confident)` |
| `preliminary=true` + barras nullables | Speed | `CognitiveProfileScore(...: Double?, preliminary=true SIEMPRE)` |
| Sealed gated | Memory | `RciResult.{NotEnoughData\|Value(reliable)}` |
| Máquina de madurez | Perfil global | `DimensionStatus.{CALIBRATING\|EMERGING\|ESTABLISHED}` |
| `Double` crudo + sentinela 0.0 | Decisiones | `ScoringEquations` (PHD=0.0 si AGRS>0.75) |

Forzar un supertipo común sería un refactor masivo y rompería el ratchet Detekt + los golden de
metrics. **Solución:** `ScoreResult` es un **veredicto de borde** read-only que se *deriva* de los
scores existentes — exactamente como `ModuleContribution` ya hace hacia el perfil global
`[hecho, único contrato cross-module hoy]`.

```
  Motores reales (INTACTOS)        Adaptadores (NUEVOS, finos)      Veredicto universal
  FocusScoreCalculator ─┐
  SpeedScoreEngine      ─┤   ScoreResultAdapter<Raw>            ┌──────────────┐
  ReasoningScorer       ─┼─► fun toScoreResult(raw, ctx): ───► │ ScoreResult  │
  ScoringEquations(AGRS)─┤              ScoreResult            └──────┬───────┘
  Memory RciResult      ─┘                                           │
                                          ScoreResult.toModuleContribution()  ◄── ya existe
```

- **Paquete neutro:** `ruta probable` `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/scoring/ScoreResult.kt` (NUEVO). Los módulos dependen de él, nunca al revés.
- **Detekt:** archivo de solo `data class`/`enum`/`object` con `when` exhaustivos y funciones `<50`
  líneas → cero findings estructurales. Riesgo `LongParameterList` mitigado agrupando en sub-bloques.

---

## 2. `ScoreResult` universal (pseudocódigo Kotlin)

```kotlin
package com.mindos.app.features.mindboost.core.scoring

/**
 * Veredicto de scoring UNIVERSAL, read-only, de una sesión de cualquier módulo.
 * NO es un motor: se DERIVA de los scores reales vía un ScoreResultAdapter.
 * Responde dos preguntas con honestidad auditable:
 *   1. ¿Fue JUGABLE? (siempre sí salvo crash) — independiente del estado.
 *   2. ¿Puede ACTUALIZAR el perfil cognitivo? — SOLO si state == VALID (invariante 1/2).
 */
data class ScoreResult(
    val provenance: ScoreProvenance,        // quién/cuándo/versión scoring/versión banco
    val state: ScoreState,                  // el veredicto (ver INFRA_01)
    val primarySignal: Signal?,             // null salvo VALID (invariante null!=0)
    val secondarySignals: List<Signal>,     // diagnósticas; NUNCA degradan perfil
    val sampling: Sampling,                 // nEffective / minNRequired / confidence / IC
    val updatesProfile: Boolean,            // computado por la state machine; ÚNICO true en VALID
    val preliminary: Boolean,               // true hasta robustez plena
    val dataQuality: DataQuality,           // timebase, jank, banco, estabilidad
    val claimLevel: ClaimLevel,             // qué se permite AFIRMAR en UI
    val copySafetyFlags: Set<CopySafetyFlag>,
) {
    init {
        require((state == ScoreState.VALID) == (primarySignal != null)) {
            "VALID iff primarySignal present. null != 0."           // invariante 1
        }
        require(state == ScoreState.VALID || !updatesProfile) {
            "Estado no-VALID NUNCA actualiza perfil."               // invariante 2 (R-NO-REDUCE)
        }
    }
}
```

### 2.1 Sub-bloques (anti-`LongParameterList`, mejor cohesión)

```kotlin
data class ScoreProvenance(
    val exerciseId: String, val moduleId: ModuleId, val sessionId: String,
    val pseudoUserId: String,         // hash device-bound; jamás email/uid crudo (privacidad)
    val timestampMs: Long,
    val scoringVersion: String,       // ancla a SpeedVersions.SCORE_VERSION [hecho] / equiv focus
    val itemBankVersion: String?,     // null si procedural (Decisiones) [hecho]
)

data class DataQuality(
    val deviceTimingQuality: TimebaseQuality,   // ancla a Timebase/TimebasePolicy [hecho]
    val criticalFrameJitter: Boolean,           // HOY hardcoded false [hecho] → ver INFRA_02
    val frameDropFraction: Double,              // [0,1]; HOY 0.0 hardcoded [hecho] → ver INFRA_02
    val itemBankIntegrity: BankIntegrity,
    val metricStability: MetricStability,
    val invalidatedBy: InvalidationReason?,
) { init { require(frameDropFraction in 0.0..1.0) } }

data class Sampling(
    val nEffective: Int, val minNRequired: Int, val confidence: Double,
    val crossSessionAggregated: Boolean, val confidenceInterval: ClosedRange<Double>?,
) {
    val meetsMinN: Boolean get() = nEffective >= minNRequired
    init { require(confidence in 0.0..1.0) }
}

/** value==null SIEMPRE = "no computable", JAMÁS "bajo rendimiento" (invariante 1). */
data class Signal(
    val key: SignalKey, val value: Double?, val lowerBound: Double?, val upperBound: Double?,
    val unit: SignalUnit, val uncertaintyLabel: UncertaintyLabel, val required: Boolean = false,
)
```

> Las definiciones canónicas de `ScoreState`, su state machine y la factory que impide `VALID` con
> `n<nMin` viven en **INFRA_01** (`05_INFRAESTRUCTURA_P0/INFRA_01_SCORE_RESULT_STATE_MACHINE.md`).
> Este documento las consume; no las duplica.

---

## 3. Adaptación desde los scores existentes `[inferido de INFRA_P0 §4]`

Cada módulo implementa UN adaptador read-only sobre su score nativo. Anclaje honesto por módulo
(lo que cada uno puede reportar **hoy**, verificado en GROUND_TRUTH):

| Módulo | `Raw` real | `deviceTimingQuality` | criticality | Detectores reales (testeados) | Banco | Notas de honestidad |
|---|---|---|---|---|---|---|
| **Speed/Android** | `CognitiveProfileScore` | `AUTHORITATIVE` | `RT_CRITICAL` | anticipación + quality-gate `[hecho]` | `PROCEDURAL` | jank hardcoded `false/0.0` → `jankExcessive` no puede disparar; §15 mantiene `preliminary=true` SIEMPRE `[hecho]` |
| **Speed/Desktop·iOS** | idem | `UNTRUSTED` (fallback) | `RT_CRITICAL` | idem | `PROCEDURAL` | siempre `DEVICE_UNRELIABLE` → no-update. Correcto `[hecho fallback]` |
| **Focus** | `FocusScore` | `MONOTONIC_NONFRAME` | `RT_CRITICAL` | anticipación, SSRT-trust, fast-commission `[hecho]` | `PROCEDURAL` (E1–E13 por seed) | onset es corrutina, no vsync `[hecho]`; si §15 exige frame-accurate, RT-crítico nunca es `AUTHORITATIVE` → no-update por diseño |
| **Decisiones** | `Double`/`ScoringEquations` | `WALLCLOCK_DERIVED` | `RT_SECONDARY` | AGRS RT<400, AGRS>0.75 `[hecho]` | `PROCEDURAL` | RT no es primaria (Brier/calibración sí); `INVALIDATED` por AGRS; `spatialEntropy=1.0` muerto `[hecho]` |
| **Logic** | `ReasoningScore` | `WALLCLOCK_DERIVED` | `RT_SECONDARY` | — ninguno real `[hecho]` | `SMALL_UNVALIDATED`(6) | sin detector → `adequateFor` falla → `CALIBRATING`; banco memorizable degrada claim |
| **Memory** | `RciResult` | `NOT_TIME_CRITICAL` | `RT_IRRELEVANT` | RCI reliability gate `[hecho]` | n/a | `NotEnoughData`→`INSUFFICIENT_N`; nunca RT |
| **Language** | `ScoringResult`/`ClaimEngine` | `NOT_TIME_CRITICAL` | `RT_IRRELEVANT` | — `[hecho]` | `SMALL_UNVALIDATED`(36) | sin RT físico `[hecho]`; banco fijo → `BANK_CONTAMINATED` tras repetición |
| **Creativity** | validator | `WALLCLOCK_DERIVED` | `RT_IRRELEVANT` | content-integrity **BLOCKED** `[hecho]` | `SMALL_UNVALIDATED`(14) | keyword-stuffing no detectable → claim tope `PRACTICE_SIGNAL` |
| **Emotions** | `EmotionMetrics` | `MONOTONIC_NONFRAME` | `RT_SECONDARY` | abandono (seguro) `[hecho]` | `SMALL_UNVALIDATED` | abandono → `ABANDONED_SAFE`; `TransferLabel` real ya existe `[hecho]` |

```kotlin
/** Adaptador genérico. Cada módulo implementa UNO. Read-only sobre su score nativo. */
interface ScoreResultAdapter<Raw> { fun toScoreResult(raw: Raw, ctx: ScoreContext): ScoreResult }
```

---

## 4. `ProfileUpdateGate` — consume SOLO `ScoreResult`, nunca `Double` crudo

El brain-profile consume **exclusivamente** `ScoreResult`; jamás un `Double` directo de un motor.
Esto cierra el agujero por el que `ScoringEquations` (Decisiones) emite `Double` crudo
`[hecho, GROUND_TRUTH §scoring]`.

```kotlin
/**
 * ÚNICO punto autorizado para escribir en el perfil. Si el veredicto no actualiza,
 * NO llama al aggregator: ausencia de update, jamás un valor a la baja (R-NO-REDUCE).
 */
object ProfileUpdateGate {
    fun apply(result: ScoreResult, aggregator: MindBoostProfileAggregator) {
        if (!result.updatesProfile) return                    // jugable, no toca perfil
        require(result.state == ScoreState.VALID)             // defensa en profundidad
        aggregator.contribute(result.toModuleContribution())  // reusa el contrato existente
    }
}

/** Salida hacia el perfil global: reusa ModuleContribution YA existente [hecho]. */
fun ScoreResult.toModuleContribution(): ModuleContribution = ModuleContribution(
    moduleId = provenance.moduleId.toCognitiveModuleId(),
    // Si NO actualiza, contribuye null (calibrating), JAMÁS 0 (invariante 1).
    normalizedScore = if (updatesProfile) primarySignal?.value?.normalized() else null,
    dataSufficiency = if (updatesProfile) sampling.confidence else 0.0,
    confidence      = if (updatesProfile) sampling.confidence else 0.0,
    preliminary     = preliminary,
)
```

**Por qué nunca acepta `Double` crudo:** un `Double` no transporta `state`, `sampling`, ni
`dataQuality`; el gate no podría distinguir "0.0 = malo" de "0.0 = sentinela de sesión inválida" —
exactamente la ambigüedad de `ScoringEquations` hoy. El `ScoreResult` hace esa distinción explícita.

---

## 5. Relación con `ModuleContribution` / brain-profile `[hecho]`

- `FocusScore`, `CognitiveProfileScore`, `RciResult`, etc. **quedan idénticos** — son la fuente `Raw`.
  Cero rotura de tests/golden/Detekt existentes.
- El perfil global **no cambia de contrato**: `toModuleContribution()` produce el `ModuleContribution`
  que `MindBoostProfileAggregator` ya consume. La invariante
  `DimensionState.require((CALIBRATING)==(value==null))` se respeta automáticamente porque no-update ⇒
  `normalizedScore=null`.
- El composite global sigue requiriendo `≥3 ESTABLISHED` `[hecho]`; un módulo en `CALIBRATING` nunca lo
  fuerza.

---

## 6. Tratamiento `null != 0`, serialización segura y UI safe state

### 6.1 Serialización segura

```kotlin
@Serializable
data class ScoreResultDto(
    val schemaVersion: Int,                 // versionado (Json ignoreUnknownKeys=true) [convención repo]
    val state: String,                      // nombre de ScoreState
    val primaryValue: Double?,              // null persiste como null, NUNCA 0.0
    val nEffective: Int, val minN: Int,
    val updatesProfile: Boolean, val preliminary: Boolean,
)
```

- **Regla de serialización (invariante 1):** un estado no-`VALID` SIEMPRE serializa `primaryValue=null`.
  Test canario obligatorio: "ningún `ScoreResult` no-VALID produce un número en el DTO".
- Persistencia cifrada: reusar el patrón `SecureKeyValueStore` (AES-GCM + Keystore) ya establecido para
  datos cognitivos sensibles `[hecho, convención repo]`. Solo se persisten **resúmenes**, nunca trials
  crudos.

### 6.2 UI safe state

```kotlin
/** El render NUNCA recibe un Double crudo: traduce el veredicto a un estado visual honesto. */
sealed interface ScoreUiState {
    data class Confident(val value: Double, val band: ClosedRange<Double>, val label: String) : ScoreUiState
    data class Preliminary(val value: Double, val band: ClosedRange<Double>) : ScoreUiState  // "estimación preliminar"
    data object Calibrating : ScoreUiState        // "—", nunca 0
    data object InsufficientData : ScoreUiState   // "datos insuficientes"
    data object NotUpdatedQuality : ScoreUiState  // "no actualizado por calidad de medición"
    data object Invalidated : ScoreUiState        // "sesión no contabilizada"
}

fun ScoreResult.toUiState(): ScoreUiState = when (state) {
    ScoreState.VALID -> { /* Confident o Preliminary según this.preliminary */ TODO() }
    ScoreState.CALIBRATING, ScoreState.ABANDONED_SAFE, ScoreState.SUSPECTED_GAMING -> ScoreUiState.Calibrating
    ScoreState.INSUFFICIENT_N -> ScoreUiState.InsufficientData
    ScoreState.DEVICE_UNRELIABLE, ScoreState.BANK_CONTAMINATED -> ScoreUiState.NotUpdatedQuality
    ScoreState.INVALIDATED -> ScoreUiState.Invalidated
}
```

El radar del brain-profile pinta dimensiones `CALIBRATING` como `—`, jamás `0` `[hecho, respetar
comportamiento existente]`.

---

## 7. Verificación

| Criterio | Check |
|---|---|
| Imposible `VALID` con `n<minN` | factory privada + `require` en INFRA_01; test canario `n=nMin-1 ⇒ INSUFFICIENT_N` |
| `updatesProfile` ⟺ `state==VALID` | property test exhaustivo sobre todos los estados |
| Ningún no-VALID emite número | property test + canario de serialización |
| Perfil intacto contractualmente | `toModuleContribution` produce el mismo DTO; suite del aggregator verde |
| Detekt sin nuevos findings | solo data/enum/object puro; sub-bloques agrupados |

---

## Resumen

`ScoreResult` es un **veredicto de borde universal** derivado (no reemplazo) de los 4 estilos de score
reales; `ProfileUpdateGate` consume **solo** `ScoreResult` y, si no actualiza, **no llama** al
aggregator (ausencia de update, nunca valor a la baja). `null != 0`, timebase/jank gates de Enfoque y
Velocidad, cero claims de far-transfer, anti-gaming solo si testeado, y banco no validado bloquea
perfil — todas forzadas en `init`/state machine, no por convención.
