# Infraestructura P0 (Kotlin)


## scoreresult_statemachine

I have enough ground truth. Here is the design.

---

# MindBoost — `ScoreResult` universal + máquina de estados de scoring

> Diseño anclado al código real (`FocusScore`, `CognitiveProfileScore`, `MetricValue`, `DimensionState`, `SsrtCalculator.trustworthy`, `SessionQualityEvaluator`, `TimebasePolicy`, AGRS). **No** propone un refactor que toque esos tipos: `ScoreResult` es un **veredicto de borde** (boundary verdict) que se *deriva* de los scores existentes, no los reemplaza. Idiomático Kotlin: `data class` + `sealed interface` + `enum` exhaustivos, `null` real (nunca `0`), bandas de incertidumbre.

---

## 0. Principio de capas (por qué un tipo nuevo NO rompe nada)

El repo tiene 4 estilos de "estado de score" incompatibles entre sí (banda+`confident`, `preliminary`+nullables, sealed gated, máquina de madurez). Forzar a todos a un supertipo común sería un refactor masivo y rompería el ratchet Detekt. La solución: una **capa de adaptación read-only**, exactamente como `ModuleContribution` ya hace hacia el perfil global.

```
  Motores reales (intactos)            Adaptadores (nuevos, finos)        Veredicto universal
  ─────────────────────────            ───────────────────────────        ───────────────────
  FocusScoreCalculator ───────┐
  SpeedScoreEngine     ───────┤        ScoreResultAdapter<Raw>            ┌──────────────┐
  ReasoningScorer      ───────┼──────► fun toScoreResult(raw, ctx): ───► │ ScoreResult  │
  ScoringEquations(AGRS)──────┤                       ScoreResult        │  (sealed-ish)│
  Memory RciResult     ───────┤                                          └──────┬───────┘
  Emotion metrics      ───────┘                                                 │
                                                                                ▼
                                                          ScoreResult.toModuleContribution()
                                                          (perfil global, ya existente)
```

- **`ScoreResult` vive en un paquete nuevo neutro**: `features/mindboost/scoring/contract/`. No depende de ningún módulo concreto; los módulos dependen de él, nunca al revés (misma dirección que `ModuleContribution.kt`).
- **Detekt**: `ScoreResult.kt` es un archivo data-class/enum sin lógica → cero findings estructurales. Los `enum` con muchos valores y los hex/constantes NO aplican (no es Composable ni AppColors). Los adaptadores son funciones puras pequeñas (`<50` líneas), un `when` exhaustivo por estado. El único riesgo es `LongParameterList` en el constructor de `ScoreResult` (18 campos) → se mitiga **agrupando** sub-bloques en `data class` anidadas (`DataQuality`, `AntiGaming`, `Provenance`), bajando el constructor de nivel superior a ~9 params. Esto también es mejor diseño.

---

## 1. `ScoreResult` universal

```kotlin
package com.mindos.app.features.mindboost.scoring.contract

/**
 * Veredicto de scoring UNIVERSAL y read-only de una sesión de cualquier módulo MindBoost.
 *
 * NO es un motor de cálculo: se DERIVA de los scores reales de cada módulo
 * (FocusScore, CognitiveProfileScore, ReasoningScore, RciResult, AGRS...) vía un
 * ScoreResultAdapter. Su única responsabilidad es responder dos preguntas con
 * honestidad auditable:
 *
 *   1. ¿Esta sesión fue JUGABLE? (siempre sí salvo crash) -> independiente del estado.
 *   2. ¿Puede ACTUALIZAR el perfil cognitivo? -> SOLO si [state] == VALID.
 *
 * Invariantes (forzadas en init, nunca silenciadas):
 *   - state == VALID  <=> primarySignal != null  (null NUNCA es 0; §7).
 *   - state != VALID  ==> updatesProfile == false (señal inválida NUNCA reduce perfil).
 *   - preliminary == true mientras no se cumplan TODAS las condiciones robustas.
 */
data class ScoreResult(
    val provenance: ScoreProvenance,          // quién/cuándo/qué versión (agrupado)
    val state: ScoreState,                     // el veredicto (sealed-style enum)
    val primarySignal: Signal?,                // null salvo VALID
    val secondarySignals: List<Signal>,        // diagnósticas; NUNCA degradan perfil
    val sampling: Sampling,                     // nEffective / minNRequired / confidence
    val preliminary: Boolean,                   // true hasta condiciones robustas
    val dataQuality: DataQuality,              // timebase, jank, banco, anti-gaming
    val transferLabel: TransferLabel,          // honestidad de claim (TASK_ONLY default)
    val claimLevel: ClaimLevel,                // qué se permite AFIRMAR en UI
    val copySafetyFlags: Set<CopySafetyFlag>,  // qué copy está PROHIBIDO mostrar
) {
    /** El núcleo: ¿este veredicto puede tocar el CognitiveProfileScore / DimensionState? */
    val updatesProfile: Boolean
        get() = state == ScoreState.VALID && !preliminary == false /* ver regla R-UPDATE */

    init {
        require((state == ScoreState.VALID) == (primarySignal != null)) {
            "VALID iff primarySignal present (state=$state, primary=$primarySignal). null != 0."
        }
        require(state == ScoreState.VALID || profileUpdateForbidden()) {
            "Non-VALID state must forbid profile update (state=$state)."
        }
        require(secondarySignals.none { it.value == null && it.required }) {
            "A required secondary signal cannot be null-but-required; mark it optional."
        }
    }

    private fun profileUpdateForbidden(): Boolean = !rawUpdatesProfile()
    private fun rawUpdatesProfile(): Boolean = state == ScoreState.VALID
}
```

> Nota sobre `updatesProfile`: en código real esto NO es un getter "clever" — se computa en el adaptador (ver §3, `EligibilityGate`) y se guarda como `val`. Lo dejo arriba como contrato conceptual; abajo lo modelo limpio.

### 1.1 Sub-bloques agrupados (anti-`LongParameterList`, mejor diseño)

```kotlin
/** Procedencia + versionado. Pseudónimo: userId hasheado en capa nativa, NUNCA PII. */
data class ScoreProvenance(
    val exerciseId: String,        // p.ej. "focus-e4-freno"
    val moduleId: ModuleId,        // enum
    val sessionId: String,         // opaco, por-sesión
    val pseudoUserId: String,      // hash device-bound; jamás email/uid crudo
    val timestampMs: Long,
    val scoringVersion: String,    // ancla a SpeedVersions.SCORE_VERSION / focus equiv.
    val itemBankVersion: String?,  // null si el módulo es procedural (Decisiones)
)

/**
 * Calidad de medición. Es la entrada PRINCIPAL al gate de profile-update.
 * Cualquier flag "no confiable" aquí fuerza state != VALID.
 */
data class DataQuality(
    val deviceTimingQuality: TimebaseQuality,   // ancla a Timebase/TimebasePolicy
    val criticalFrameJitter: Boolean,           // HOY hardcoded false en speed; ver §4
    val frameDropFraction: Double,              // [0,1]; HOY 0.0 en speed; ver §4
    val itemBankIntegrity: BankIntegrity,       // contaminado/holdout/sin validar
    val metricStability: MetricStability,       // estable / inestable
    val invalidatedBy: InvalidationReason?,     // null si nada invalidó
) {
    init {
        require(frameDropFraction in 0.0..1.0) { "frameDropFraction must be in [0,1]." }
    }
}

/** Flags anti-gaming DISPARADOS (no "diseñados"). Vacío = limpio. */
data class AntiGaming(
    val firedFlags: Set<AntiGamingFlag>,
    val detectorCoverage: DetectorCoverage,   // qué detectores existen REALMENTE para este ejercicio
) {
    val isClean: Boolean get() = firedFlags.isEmpty()
}

/** n por celda. Esta es la fuente de verdad de "¿suficiente evidencia?". */
data class Sampling(
    val nEffective: Int,        // tras filtrar anticipaciones / outliers / no-respuestas
    val minNRequired: Int,      // n_min por celda/métrica (ancla a MIN_N_* reales)
    val confidence: Double,     // [0,1]
    val crossSessionAggregated: Boolean,  // true si nEffective viene de >1 sesión
) {
    val meetsMinN: Boolean get() = nEffective >= minNRequired
    init { require(confidence in 0.0..1.0) }
}
```

### 1.2 La señal (`primarySignal` / `secondarySignals`)

```kotlin
/**
 * Una señal medida. value == null SIEMPRE significa "no computable / insuficiente",
 * jamás "bajo rendimiento". Lleva banda de incertidumbre (como FocusScore) y etiqueta.
 */
data class Signal(
    val key: SignalKey,                // qué mide (RT_MEDIAN, ICR, BRIER, SSRT...)
    val value: Double?,                // null = no computable
    val lowerBound: Double?,           // IC inferior (null si value null)
    val upperBound: Double?,           // IC superior
    val unit: SignalUnit,              // MILLIS / RATIO_0_1 / SCORE_0_100 / Z
    val uncertaintyLabel: UncertaintyLabel,  // texto honesto para UI
    val required: Boolean = false,     // si true, su ausencia bloquea VALID
)

enum class UncertaintyLabel {
    PRECISE,            // banda estrecha, confident
    WIDE_BAND,          // hay valor pero IC ancho -> "estimación preliminar"
    INSUFFICIENT_DATA,  // value == null por n
    CALIBRATING,        // value == null, aún acumulando
    NOT_DEMONSTRATED,   // medible pero sin validación que respalde el claim
}
```

---

## 2. Enums del contrato (todos exhaustivos, `when` sin `else`)

```kotlin
enum class ModuleId { FOCUS, SPEED, LOGIC, DECISIONS, MEMORY, LANGUAGE, CREATIVITY, EMOTIONS }

/** EL conjunto de estados pedido. Orden = de "mejor" a "razón de no-update". */
enum class ScoreState {
    VALID,               // único estado que actualiza perfil
    CALIBRATING,         // jugable, acumulando; value null
    INSUFFICIENT_N,      // n < minN para la celda
    INVALIDATED,         // contaminado/anómalo: se descarta esta sesión
    DEVICE_UNRELIABLE,   // timebase no autoritativa para RT crítico (R1)
    BANK_CONTAMINATED,   // banco memorizado/sin validar/holdout agotado (R5)
    SUSPECTED_GAMING,    // detector anti-gaming REAL disparado (R2)
    ABANDONED_SAFE,      // abandono en ejercicio emocional/frustración (R8) — NO culpa
}

/** Calidad de la timebase. Ancla a Timebase enum + TimebasePolicy.deviceTimingRisk. */
enum class TimebaseQuality {
    AUTHORITATIVE,        // Choreographer + MotionEvent, misma base monotónica (solo speed/Android)
    MONOTONIC_NONFRAME,   // TimeSource.Monotonic, honesto pero NO frame-accurate (focus, emotions)
    WALLCLOCK_DERIVED,    // EpochClock / millis derivados (logic, decisiones, creativity)
    UNTRUSTED,            // riesgo de timing alto / fallback no-autoritativo
    NOT_TIME_CRITICAL,    // el ejercicio NO mide RT físico (language, memory-accuracy)
}

/** ¿La métrica primaria del ejercicio DEPENDE de RT autoritativo? */
enum class TimingCriticality { RT_CRITICAL, RT_SECONDARY, RT_IRRELEVANT }

enum class BankIntegrity {
    OK,                   // banco grande/validado/rotado (aspiracional hoy)
    SMALL_UNVALIDATED,    // Logic(6), Language(36), Creativity(14): memorizable, sin IRT
    HOLDOUT_EXHAUSTED,    // pool de eval agotado -> claim bloqueado
    PROCEDURAL,           // generado por seed (Decisiones): no memorizable como texto
    CONTAMINATED,         // exposición repetida detectada -> esta sesión no cuenta
    NOT_APPLICABLE,
}

enum class MetricStability { STABLE, UNSTABLE, UNKNOWN }

/** Etiqueta de transfer. ANCLA a emotions/EmotionCatalogIds.TransferLabel (mismo enum, elevado). */
enum class TransferLabel { TASK_ONLY, PLAUSIBLE, NOT_DEMONSTRATED, PLACEBO_RISK }

/** Qué se permite AFIRMAR. Escalado por evidencia, espejo de language/ClaimEngine. */
enum class ClaimLevel {
    NONE,                 // no afirmar nada (INVALIDATED/DEVICE_UNRELIABLE)
    PRACTICE_SIGNAL,      // "señal de práctica" — proxy interno
    TASK_PERFORMANCE,     // "rendimiento en esta tarea" (no transfer)
    WITHIN_TASK_TREND,    // "progreso contra tu propia línea base, en esta tarea"
    // NO existe nivel "BROAD_TRANSFER": prohibido por §4. Su ausencia es deliberada.
}

enum class SignalUnit { MILLIS, RATIO_0_1, SCORE_0_100, Z, COUNT }
```

### 2.1 Razones de invalidación / flags anti-gaming (solo los REALES)

```kotlin
/** Por qué se invalidó/degradó. Cada valor mapea a un detector que EXISTE en el repo. */
enum class InvalidationReason {
    ANTICIPATION_FLOOD,      // RT < floor masivo (Focus/Speed TrialClassifier) [REAL]
    RT_UNDER_400_GUARD,      // AGRS rtFactor, Decisiones [REAL]
    SSRT_UNTRUSTWORTHY,      // p(respond|signal) fuera [0.25,0.75] (SsrtCalculator) [REAL]
    FAST_COMMISSION_BURST,   // impulsividad fast-error (AdaptiveEngine) [REAL]
    SESSION_QUALITY_LOW,     // SessionQualityEvaluator -> LOW/CONTAMINATED [REAL]
    AGRS_OVER_THRESHOLD,     // AGRS > 0.75 invalida sesión (ScoringEquations) [REAL]
    OUTLIER_COLLAPSE,        // tras MAD no quedan trials usables
}

/** Detector anti-gaming DISPARADO. Solo entran detectores implementados+testeados (R2). */
enum class AntiGamingFlag {
    ANTICIPATION,            // REAL (Focus/Speed)
    RT_UNDER_400,            // REAL (Decisiones AGRS)
    SSRT_WAITING_STRATEGY,   // REAL (Focus stop-signal)
    FAST_COMMISSION,         // REAL (Focus AdaptiveEngine)
    QUALITY_GATE_TRIP,       // REAL (Speed)
    // DELIBERADAMENTE AUSENTES como flags activos en ejercicios reales:
    //   TAP_CADENCE_MISMATCH -> solo Arena/sintético (R2: no cuenta como cobertura)
    //   SPATIAL_POSITION_BIAS -> no existe (spatialEntropy=1.0 fijo)
    //   KEYWORD_STUFFING -> bloqueado por diseño (Creativity Metric Integrity BLOCKED)
}

/**
 * Cobertura REAL de detectores para el ejercicio. Si un ejercicio mide RT y NO tiene
 * detector implementado+testeado, su claim baja y NO puede ser VALID con confianza alta (R2).
 */
data class DetectorCoverage(
    val hasAnticipationFilter: Boolean,
    val hasRtFloorGuard: Boolean,
    val hasSsrtTrustGuard: Boolean,
    val hasQualityGate: Boolean,
    val hasContentIntegrity: Boolean,   // keyword/calidad: HOY false en todos (Creativity BLOCKED)
) {
    /** Un ejercicio RT-crítico sin ningún detector real es REVISE/BLOCK, no VALID. */
    fun adequateFor(criticality: TimingCriticality): Boolean = when (criticality) {
        TimingCriticality.RT_CRITICAL ->
            hasAnticipationFilter && (hasRtFloorGuard || hasSsrtTrustGuard || hasQualityGate)
        TimingCriticality.RT_SECONDARY -> hasAnticipationFilter
        TimingCriticality.RT_IRRELEVANT -> true
    }
}

/** Copy PROHIBIDO de mostrar para este veredicto (espejo de scanners de terminología). */
enum class CopySafetyFlag {
    NO_FAR_TRANSFER_CLAIM,     // §4: nada de vida real/decisiones/emoción/fake-news/conducción/...
    NO_INTELLIGENCE_CLAIM,     // nada de IQ/inteligencia general
    NO_CLINICAL_CLAIM,         // nada de diagnóstico/salud mental
    NO_ABSOLUTE_CONSTRUCT,     // §6: si solo hay proxy, no mostrar constructo absoluto
    SHOW_PRELIMINARY_BADGE,    // obligar rótulo "estimación preliminar"
    SHOW_NOT_UPDATED_NOTICE,   // "no actualizado por calidad de medición"
}
```

---

## 3. La MÁQUINA DE ESTADOS (el corazón)

### 3.1 Forma idiomática: un `sealed interface` de transición + un evaluador puro

El veredicto se computa con **gates en cascada de cortocircuito**: el PRIMER gate que falla fija el `ScoreState`. El orden importa — un gate de seguridad (timebase, gaming) vence a un gate de cantidad (n), que vence a calibración.

```kotlin
package com.mindos.app.features.mindboost.scoring.contract

/** Entrada cruda y neutra al evaluador (la llena cada adaptador de módulo). */
data class ScoreInputs(
    val criticality: TimingCriticality,
    val isEmotionalOrFrustration: Boolean,   // §8: abandono aquí es seguridad, no culpa
    val completed: Boolean,                   // false = abandono
    val quality: DataQuality,
    val antiGaming: AntiGaming,
    val sampling: Sampling,
    val primaryComputable: Boolean,          // el motor pudo calcular la señal primaria
    val stability: MetricStability,
    val secondaryAccuracyGate: SecondaryGate, // accuracy mínima para que RT signifique algo
)

/** Gate de accuracy secundaria: RT rápido con accuracy basura NO es velocidad útil. */
data class SecondaryGate(val accuracy: Double?, val minAccuracy: Double) {
    val passes: Boolean get() = accuracy != null && accuracy >= minAccuracy
}

/**
 * Evaluador PURO de la máquina de estados. Determinista, sin IO, 100% testeable en desktopTest.
 * Devuelve el estado + si actualiza perfil + si es preliminar. Cero efectos secundarios.
 */
object ScoringStateMachine {

    data class Verdict(
        val state: ScoreState,
        val updatesProfile: Boolean,
        val preliminary: Boolean,
        val claimLevel: ClaimLevel,
    )

    fun evaluate(inputs: ScoreInputs): Verdict {
        // --- GATES DE SEGURIDAD (cortocircuito, mayor prioridad) ---

        // §8 — abandono seguro en ejercicios emocionales: jugable, NUNCA penaliza.
        if (!inputs.completed && inputs.isEmotionalOrFrustration) {
            return verdictNoUpdate(ScoreState.ABANDONED_SAFE, ClaimLevel.NONE)
        }
        // R1 — RT crítico sin timebase autoritativa => DEVICE_UNRELIABLE. Jugable, no update.
        if (inputs.criticality == TimingCriticality.RT_CRITICAL && !timebaseTrusted(inputs.quality)) {
            return verdictNoUpdate(ScoreState.DEVICE_UNRELIABLE, ClaimLevel.PRACTICE_SIGNAL)
        }
        // R4-jank — jank/frame-drop excesivo invalida RT incluso con timebase nominalmente OK.
        if (inputs.criticality != TimingCriticality.RT_IRRELEVANT && jankExcessive(inputs.quality)) {
            return verdictNoUpdate(ScoreState.DEVICE_UNRELIABLE, ClaimLevel.PRACTICE_SIGNAL)
        }
        // R5 — banco contaminado/holdout agotado.
        if (bankContaminated(inputs.quality.itemBankIntegrity)) {
            return verdictNoUpdate(ScoreState.BANK_CONTAMINATED, ClaimLevel.PRACTICE_SIGNAL)
        }
        // R2 — detector anti-gaming REAL disparado. (Detector AUSENTE no llega aquí: lo trata R2-cobertura abajo.)
        if (!inputs.antiGaming.isClean) {
            return verdictNoUpdate(ScoreState.SUSPECTED_GAMING, ClaimLevel.NONE)
        }
        // INVALIDATED — invalidación dura (AGRS>0.75, quality LOW...).
        if (inputs.quality.invalidatedBy != null) {
            return verdictNoUpdate(ScoreState.INVALIDATED, ClaimLevel.NONE)
        }

        // --- GATES DE EVIDENCIA ---

        // Señal primaria no computable o n insuficiente -> calibrar, no penalizar (§7).
        if (!inputs.primaryComputable) {
            return verdictNoUpdate(ScoreState.CALIBRATING, ClaimLevel.PRACTICE_SIGNAL)
        }
        if (!inputs.sampling.meetsMinN) {
            return verdictNoUpdate(ScoreState.INSUFFICIENT_N, ClaimLevel.PRACTICE_SIGNAL)
        }
        // R2-cobertura — RT-crítico sin detector anti-gaming implementado: NO puede ser VALID.
        if (!inputs.antiGaming.detectorCoverage.adequateFor(inputs.criticality)) {
            return verdictNoUpdate(ScoreState.CALIBRATING, ClaimLevel.PRACTICE_SIGNAL)
        }
        // Accuracy secundaria bajo gate -> RT no significa nada todavía.
        if (inputs.criticality == TimingCriticality.RT_CRITICAL && !inputs.secondaryAccuracyGate.passes) {
            return verdictNoUpdate(ScoreState.CALIBRATING, ClaimLevel.PRACTICE_SIGNAL)
        }
        // Métrica inestable -> jugable y se muestra, pero no consolida en perfil.
        if (inputs.stability == MetricStability.UNSTABLE) {
            return verdictNoUpdate(ScoreState.CALIBRATING, ClaimLevel.TASK_PERFORMANCE)
        }

        // --- VALID: única vía a profile-update ---
        // preliminary sigue true salvo robustez plena: confianza alta + agregación inter-sesión.
        val robust = inputs.sampling.confidence >= ROBUST_CONFIDENCE &&
            inputs.sampling.crossSessionAggregated
        return Verdict(
            state = ScoreState.VALID,
            updatesProfile = true,                      // ÚNICO punto que pone true
            preliminary = !robust,                       // robusto => preliminary=false
            claimLevel = if (robust) ClaimLevel.WITHIN_TASK_TREND else ClaimLevel.TASK_PERFORMANCE,
        )
    }

    // VALID jamás reduce: estados no-VALID NUNCA actualizan (señal inválida no degrada perfil).
    private fun verdictNoUpdate(state: ScoreState, claim: ClaimLevel) =
        Verdict(state = state, updatesProfile = false, preliminary = true, claimLevel = claim)

    private fun timebaseTrusted(q: DataQuality): Boolean =
        q.deviceTimingQuality == TimebaseQuality.AUTHORITATIVE

    private fun jankExcessive(q: DataQuality): Boolean =
        q.criticalFrameJitter || q.frameDropFraction > MAX_FRAME_DROP

    private fun bankContaminated(b: BankIntegrity): Boolean = when (b) {
        BankIntegrity.CONTAMINATED, BankIntegrity.HOLDOUT_EXHAUSTED -> true
        BankIntegrity.OK, BankIntegrity.SMALL_UNVALIDATED,
        BankIntegrity.PROCEDURAL, BankIntegrity.NOT_APPLICABLE -> false
    }

    private const val ROBUST_CONFIDENCE = 0.7   // espejo de aggregator minConfidence/established
    private const val MAX_FRAME_DROP = 0.10
}
```

### 3.2 Tabla de transición (jugable ≠ actualiza-perfil)

| Estado | Causa (gate que disparó) | Jugable | `updatesProfile` | `preliminary` | `claimLevel` | Copy obligado |
|---|---|---|---|---|---|---|
| `ABANDONED_SAFE` | abandono en EM/frustración (§8) | ✅ | ❌ | true | `NONE` | sin culpa, "puedes parar" |
| `DEVICE_UNRELIABLE` | RT-crítico sin timebase autoritativa **o** jank excesivo (R1/R4) | ✅ | ❌ | true | `PRACTICE_SIGNAL` | "no actualizado por calidad de medición" |
| `BANK_CONTAMINATED` | banco memorizado/holdout agotado (R5) | ✅ | ❌ | true | `PRACTICE_SIGNAL` | "estimación no fiable: contenido repetido" |
| `SUSPECTED_GAMING` | detector real disparado (R2) | ✅ | ❌ | true | `NONE` | neutro, sin acusar |
| `INVALIDATED` | AGRS>0.75 / quality LOW | ✅ | ❌ | true | `NONE` | "sesión no contabilizada" |
| `INSUFFICIENT_N` | n < minN (R3) | ✅ | ❌ | true | `PRACTICE_SIGNAL` | "datos insuficientes" |
| `CALIBRATING` | primaria null / detector ausente / accuracy gate / inestable | ✅ | ❌ | true | `PRACTICE_SIGNAL`/`TASK_PERFORMANCE` | "calibrando" (`—`, nunca 0) |
| `VALID` (preliminar) | todo OK, confianza<0.7 o sin agregación | ✅ | ✅ | **true** | `TASK_PERFORMANCE` | "estimación preliminar" |
| `VALID` (robusto) | todo OK + conf≥0.7 + inter-sesión | ✅ | ✅ | **false** | `WITHIN_TASK_TREND` | "progreso vs tu línea base (esta tarea)" |

**Reglas invariantes (forzadas en `init` + property tests):**
- **R-VALID-ONLY:** `updatesProfile == true` ⟺ `state == VALID`. Ningún otro path lo pone `true` (un solo `return` en la máquina lo hace).
- **R-NO-REDUCE:** un estado no-`VALID` jamás escribe en `CognitiveProfileScore`/`DimensionState`. La capa de persistencia recibe `Verdict.updatesProfile` y, si es `false`, **no llama** al aggregator. No hay "actualización a la baja" posible — señal inválida es *ausencia* de actualización, no un valor bajo.
- **R-NULL≠0:** estados no-`VALID` producen `primarySignal = null`; la UI lo pinta como `—`/"calibrando"/"datos insuficientes", nunca `0`.
- **R-PRELIM:** `preliminary` solo es `false` en `VALID` robusto. Mientras el §15 physical gate (timebase real, jank real) siga abierto, `Speed` fuerza `preliminary=true` en TODO veredicto (espejo de `CognitiveProfileScore.preliminary=true` SIEMPRE).

---

## 4. Cómo se ALIMENTA cada estado desde el código real (sin fabricar lo que no existe)

```kotlin
// Adaptador de ejemplo: Focus E4 (stop-signal) — RT-crítico, detectores reales.
class FocusScoreResultAdapter(
    private val timebaseQuality: TimebaseQuality,   // = MONOTONIC_NONFRAME (focus usa MonotonicClock)
) : ScoreResultAdapter<FocusSessionOutput> {

    override fun toScoreResult(raw: FocusSessionOutput, ctx: ScoreContext): ScoreResult {
        val quality = DataQuality(
            deviceTimingQuality = timebaseQuality,
            // HOY no hay medición real de jank en focus -> se reporta honestamente como desconocido.
            // Si el §15 gate exige RT autoritativo, RT_CRITICAL nunca pasará timebaseTrusted() aquí.
            criticalFrameJitter = false,
            frameDropFraction = 0.0,
            itemBankIntegrity = BankIntegrity.PROCEDURAL,   // E1–E13 son generadores por seed
            metricStability = raw.metricStability(),
            invalidatedBy = raw.invalidationReason(),       // null o SESSION_QUALITY_LOW
        )
        val antiGaming = AntiGaming(
            firedFlags = buildSet {
                if (raw.ssrt.trustworthy == false) add(AntiGamingFlag.SSRT_WAITING_STRATEGY)
                if (raw.fastCommissionBurst) add(AntiGamingFlag.FAST_COMMISSION)
                if (raw.anticipationFlood) add(AntiGamingFlag.ANTICIPATION)
            },
            detectorCoverage = DetectorCoverage(
                hasAnticipationFilter = true,   // TrialClassifier REAL
                hasRtFloorGuard = true,
                hasSsrtTrustGuard = true,        // SsrtCalculator.trustworthy REAL
                hasQualityGate = false,          // (eso es de speed)
                hasContentIntegrity = false,
            ),
        )
        val sampling = Sampling(
            nEffective = raw.goRtCount,                   // tras filtrar anticipaciones/MAD
            minNRequired = MIN_N_RT,                       // = 5, ancla a DefaultMetricsEngine
            confidence = raw.focusScore.confidenceProxy(), // de la banda de FocusScore
            crossSessionAggregated = ctx.isAggregate,
        )
        val verdict = ScoringStateMachine.evaluate(
            ScoreInputs(
                criticality = TimingCriticality.RT_CRITICAL,
                isEmotionalOrFrustration = false,
                completed = raw.completed,
                quality = quality,
                antiGaming = antiGaming,
                sampling = sampling,
                primaryComputable = raw.focusScore.value != null,
                stability = quality.metricStability,
                secondaryAccuracyGate = SecondaryGate(raw.accuracy, minAccuracy = 0.70),
            ),
        )
        return assemble(raw, ctx, verdict, quality, antiGaming, sampling)  // mapea a ScoreResult + Signals
    }
}
```

**Anclaje por módulo (lo que cada uno puede honestamente reportar HOY):**

| Módulo | `deviceTimingQuality` | `criticality` | Detectores reales | Banco | Notas de honestidad |
|---|---|---|---|---|---|
| **Speed/Android** | `AUTHORITATIVE` | `RT_CRITICAL` | anticipación + quality-gate | `PROCEDURAL` | jank `criticalFrameJitter`/`frameDrop` **hardcoded false/0.0** → hasta medirlos de verdad, `jankExcessive` no puede disparar; el §15 mantiene `preliminary=true` SIEMPRE |
| **Speed/Desktop·iOS** | `UNTRUSTED` (fallback) | `RT_CRITICAL` | igual | `PROCEDURAL` | siempre `DEVICE_UNRELIABLE` para RT crítico → jugable, no-update. Correcto. |
| **Focus** | `MONOTONIC_NONFRAME` | `RT_CRITICAL` | anticipación, SSRT-trust, fast-commission | `PROCEDURAL` | si el §15 exige frame-accurate, RT-crítico nunca es `AUTHORITATIVE` → no-update por diseño hasta tener gate real |
| **Decisiones** | `WALLCLOCK_DERIVED` | `RT_SECONDARY` | AGRS (RT<400, AGRS>0.75) | `PROCEDURAL` | RT no es la primaria (Brier/calibración sí); `INVALIDATED` por AGRS |
| **Logic** | `WALLCLOCK_DERIVED` | `RT_SECONDARY` | — (ninguno real) | `SMALL_UNVALIDATED`(6) | sin detector → `adequateFor(RT_SECONDARY)` falla → `CALIBRATING`; banco memorizable degrada claim |
| **Memory** | `NOT_TIME_CRITICAL` | `RT_IRRELEVANT` | RCI gate (fiabilidad) | n/a | `RciResult.NotEnoughData` → `INSUFFICIENT_N`; nunca RT |
| **Language** | `NOT_TIME_CRITICAL` | `RT_IRRELEVANT` | — | `SMALL_UNVALIDATED`(36) | claim vía `ClaimEngine`; banco fijo → `BANK_CONTAMINATED` tras repetición |
| **Creativity** | `WALLCLOCK_DERIVED` | `RT_IRRELEVANT` | content-integrity **BLOCKED** | `SMALL_UNVALIDATED`(14) | `hasContentIntegrity=false` → keyword-stuffing no detectable → claim tope `PRACTICE_SIGNAL` |
| **Emotions** | `MONOTONIC_NONFRAME` | `RT_SECONDARY` | abandono (seguro) | `SMALL_UNVALIDATED` | abandono → `ABANDONED_SAFE` (§8); `TransferLabel` real ya existe |

---

## 5. Convivencia con los scores reales — un puente, no un reemplazo

```kotlin
/** Adaptador genérico. Cada módulo implementa UNO. Read-only sobre su score nativo. */
interface ScoreResultAdapter<Raw> {
    fun toScoreResult(raw: Raw, ctx: ScoreContext): ScoreResult
}

/** Salida hacia el perfil global: reusa el contrato YA existente (ModuleContribution). */
fun ScoreResult.toModuleContribution(): ModuleContribution = ModuleContribution(
    moduleId = provenance.moduleId.toCognitiveModuleId(),
    // Si NO actualiza perfil, contribuye null (calibrating), JAMÁS 0 (§7).
    normalizedScore = if (updatesProfile) primarySignal?.value?.normalized() else null,
    dataSufficiency = if (updatesProfile) sampling.confidence else 0.0,
    confidence = if (updatesProfile) sampling.confidence else 0.0,
    preliminary = preliminary,
)
```

- **`FocusScore`, `CognitiveProfileScore`, `RciResult`, etc. quedan idénticos.** Son la *fuente* `Raw` del adaptador. Cero cambios → cero rotura de tests/golden/Detekt existentes.
- **El perfil global no cambia de contrato:** `ScoreResult.toModuleContribution()` produce el `ModuleContribution` que `MindBoostProfileAggregator` ya consume. La invariante `DimensionState.require((CALIBRATING)==(value==null))` se respeta automáticamente porque no-update ⇒ `normalizedScore=null`.
- **Detekt sin nuevos findings:** archivo de contrato = solo `data class`/`enum`/un `object` con `when` exhaustivos y funciones `<50` líneas; constructor de `ScoreResult` agrupado en sub-bloques (evita `LongParameterList`); `MetricStability`/`ScoreState` no son Composables ni hex → no requieren `@Suppress`. El evaluador es puro → testeable en `desktopTest` (no escaneado por Detekt si los tests pesados van ahí).

---

## 6. Tests obligatorios para que el diseño NO sea vaporware (R2)

```kotlin
// desktopTest — máquina pura, adversarial, property-based (HOY ausente en mindboost).
class ScoringStateMachineTest {
    @Test fun `RT-critical sin timebase autoritativa nunca actualiza perfil`() { /* R1 */ }
    @Test fun `gaming flag real fuerza SUSPECTED_GAMING y no-update`() { /* R2 */ }
    @Test fun `detector ausente en RT-critico cae a CALIBRATING no VALID`() { /* R2-cobertura */ }
    @Test fun `n por debajo de minN produce INSUFFICIENT_N con primarySignal null`() { /* R3,R7 */ }
    @Test fun `banco contaminado y holdout agotado bloquean update`() { /* R5 */ }
    @Test fun `abandono emocional es ABANDONED_SAFE sin penalizar`() { /* R8 */ }
    @Test fun `VALID es el UNICO estado con updatesProfile true`() { /* R-VALID-ONLY, exhaustivo */ }
    @Test fun `ningun estado no-VALID emite primarySignal no-null`() { /* R-NULL≠0, init */ }
    @Test fun `preliminary solo es false con confidence>=0.7 y agregacion inter-sesion`() { /* R-PRELIM */ }
    // property-based (Kotest Arb) sobre ScoreInputs: invariante updatesProfile => state==VALID SIEMPRE.
}
```

---

## Resumen del diseño

- **`ScoreResult`** es un **veredicto de borde universal** que se *deriva* (no reemplaza) de los scores reales, con todos los campos pedidos, agrupados en `ScoreProvenance` / `DataQuality` / `AntiGaming` / `Sampling` para evitar `LongParameterList` y mejorar cohesión. `null` es real en `primarySignal`/`Signal.value`; nunca `0`.
- **Máquina de estados** = `ScoringStateMachine.evaluate(ScoreInputs): Verdict`, puro y testeable, con **gates de seguridad en cascada de cortocircuito** (abandono → timebase → jank → banco → gaming → invalidación → n → cobertura-detector → accuracy-gate → estabilidad → `VALID`). **`VALID` es el único path que pone `updatesProfile=true`**; señal inválida es ausencia de update, jamás un valor bajo. `preliminary=false` solo con robustez plena.
- **Convivencia:** los 4 estilos reales (`FocusScore` banda+`confident`, `CognitiveProfileScore` `preliminary`+nullables, `RciResult` sealed, `DimensionState` madurez) son `Raw` de adaptadores; la salida reusa `ModuleContribution`, así el `MindBoostProfileAggregator` y sus invariantes no cambian. **Cero rotura de Detekt** (data/enum/`object` puro, sub-bloques, `when` exhaustivos sin `else`).
- **Honestidad anclada al repo:** los enums solo contienen detectores que existen+están testeados (anticipación, RT<400/AGRS, SSRT-trust, fast-commission, quality-gate); los huecos reales (tap-cadence solo en Arena, sesgo posicional inexistente, keyword-stuffing BLOCKED, jank hardcoded a false/0.0, timebase autoritativa solo en speed/Android) se modelan como **cobertura ausente** que **degrada el claim y bloquea `VALID`**, no como capacidad fingida.

---

## timing_antigaming

All anchors confirmed. I have everything I need to write the design grounded in real code.

---

# (3) TimingPolicy KMP + (4) AntiGamingFramework — Design anclado al código real

> **Anclaje verificado en repo (2026-06-27):** `TimebasePolicy` (`speed/domain/TimebasePolicy.kt:11-14,54,66-73`), `SessionQualityEvaluator` + `QualitySignals` (`speed/domain/quality/SessionQualityEvaluator.kt:22-30,46-69`), enums en `SpeedEnums.kt` (`TimestampSource:16`, `InputTimestampPrecision:26`, `Timebase:39`, `DeviceTimingRisk:48`, `SessionQuality:65`), gate muerto en `SpeedSessionStateHolder.kt:259-260` (`frameDropFraction = 0.0`, `criticalFrameJitter = false` **hardcodeados**), Android real en `SpeedTimebase.android.kt`, fallback en `SpeedTimebaseFallback.kt`, iOS delega a fallback en `SpeedTimebase.ios.kt:14` (sin CADisplayLink).

El brief pide diseño. Pero **la Regla No Negociable #2** ("diseñado NO cuenta; detector ausente = BLOCK o REVISE crítico") aplica a mi propio veredicto. Por tanto cada pieza de abajo lleva un **veredicto explícito de estado real** (`IMPLEMENTED+TESTED` / `MODEL-ONLY` / `ABSENT→BLOCK`) y el diseño es el *contrato a implementar*, no una afirmación de que ya existe.

---

## PARTE A — TimingPolicy (subsistema 3)

### A.0 Verdad de partida (no inventar)

| Capa | Estado real | Veredicto |
|---|---|---|
| Aritmética RT pura + prohibición `elapsedRealtimeNanos` | `TimebasePolicy.kt` — **IMPLEMENTED+TESTED** (`TimebasePolicyTest`) | OK |
| Captura frame-accurate Android | `SpeedTimebase.android.kt` (Choreographer + MotionEvent) — **IMPLEMENTED** | OK |
| Modelo de riesgo de dispositivo | `deviceTimingRisk()` + `SessionQualityEvaluator` — **IMPLEMENTED** | OK |
| **Medición real de jank/frame-drop que alimente el gate** | `SpeedSessionStateHolder.kt:259-260` → `0.0`/`false` **HARDCODEADOS** | **ABSENT → el gate `device_unreliable` es decorativo** |
| iOS frame-accurate (CADisplayLink) | `SpeedTimebase.ios.kt:14` delega a fallback monotónico | **ABSENT** |
| Desktop frame-accurate | fallback monotónico | downgrade explícito (aceptable) |

**Conclusión brutal:** existe la *forma* del gate pero **nunca dispara por causa física**, porque las dos señales que lo activarían entran fijas en `false`/`0.0`. Bajo la Regla #1, **hoy ningún ScoreResult de Speed puede afirmar "medido en timebase autoritativa"**, porque no hay evidencia de que el frame no haya saltado. El diseño cierra exactamente ese hueco.

### A.1 Contrato `TimingPolicy` (expect/actual)

Nuevo símbolo de plataforma. No reescribe `TimebasePolicy` (pura, se queda): lo **envuelve** con la captura real de jank que hoy falta.

```kotlin
// commonMain — speed/domain/timing/TimingPolicy.kt
// Resultado tipado del nivel de garantía temporal de una corrida (Regla #1 + #7: nunca "0").
sealed interface TimebaseGuarantee {
    /** Frame-accurate, misma base monotónica estímulo↔input, jank medido y dentro de umbral. */
    data class Authoritative(
        val source: TimestampSource,            // CHOREOGRAPHER_FRAME (SpeedEnums.kt:16)
        val precision: InputTimestampPrecision, // NANOS_NATIVE (SpeedEnums.kt:26)
        val measuredFrameStats: FrameStats,     // medido, NO hardcodeado
    ) : TimebaseGuarantee

    /** Delta monotónico honesto pero NO frame-accurate (desktop, iOS-sin-CADisplayLink, focus-MonotonicClock). */
    data class NonAuthoritative(
        val reason: DowngradeReason,            // SYSTEM_CLOCK_MONOTONIC | MILLIS_DERIVED | NO_DISPLAY_LINK
        val precision: InputTimestampPrecision,
    ) : TimebaseGuarantee

    /** Frame-accurate intentado pero jank/drop superó umbral → medición contaminada. */
    data class DeviceUnreliable(
        val reason: UnreliableReason,           // CRITICAL_FRAME_JITTER | EXCESSIVE_FRAME_DROP | CLOCK_DISCONTINUITY
        val measuredFrameStats: FrameStats,
    ) : TimebaseGuarantee
}

data class FrameStats(
    val sampledFrames: Int,
    val frameDropFraction: Double,   // fracción de frames con duración > 1.5×refresh
    val maxFrameJitterNanos: Long,   // peor desviación vs período nominal
    val criticalFrameJitter: Boolean // jitter en el frame del onset del estímulo (el único que corrompe el RT)
)

enum class DowngradeReason { SYSTEM_CLOCK_MONOTONIC, MILLIS_DERIVED, NO_DISPLAY_LINK }
enum class UnreliableReason { CRITICAL_FRAME_JITTER, EXCESSIVE_FRAME_DROP, CLOCK_DISCONTINUITY }

/**
 * Handle de plataforma: arranca un muestreador de frames durante la corrida y, al cerrar,
 * devuelve FrameStats REALES. Reemplaza el 0.0/false hardcodeado de SpeedSessionStateHolder:259-260.
 */
expect class FrameTimingProbe {
    fun start()
    fun stop(): FrameStats
}

@Composable
expect fun rememberFrameTimingProbe(): FrameTimingProbe
```

### A.2 actual Android — Choreographer/vsync real (mide jank, no lo asume)

```kotlin
// androidMain — speed/domain/timing/TimingPolicy.android.kt
actual class FrameTimingProbe {
    private val frameDurationsNanos = ArrayDeque<Long>(MAX_SAMPLES)
    private var lastFrameNanos = 0L
    private var onsetFrameJitter = false
    private val refreshNanos = currentDisplayRefreshNanos()      // Display.refreshRate → período nominal

    private val callback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {             // MISMA base que SpeedTimebase.android.kt:41
            if (lastFrameNanos != 0L) {
                val dur = frameTimeNanos - lastFrameNanos
                frameDurationsNanos.addLast(dur)
                if (frameDurationsNanos.size > MAX_SAMPLES) frameDurationsNanos.removeFirst()
            }
            lastFrameNanos = frameTimeNanos
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    actual fun start() { lastFrameNanos = 0L; Choreographer.getInstance().postFrameCallback(callback) }

    actual fun stop(): FrameStats {
        Choreographer.getInstance().removeFrameCallback(callback)
        val dropped = frameDurationsNanos.count { it > refreshNanos * FRAME_DROP_FACTOR }
        val maxJitter = frameDurationsNanos.maxOfOrNull { (it - refreshNanos).absoluteValue } ?: 0L
        return FrameStats(
            sampledFrames = frameDurationsNanos.size,
            frameDropFraction = if (frameDurationsNanos.isEmpty()) 0.0
                                else dropped.toDouble() / frameDurationsNanos.size,
            maxFrameJitterNanos = maxJitter,
            criticalFrameJitter = onsetFrameJitter, // marcado por el TrialEngine si el frame del onset saltó
        )
    }

    /** Llamado por el runner cuando se postea el onset: si ESE frame fue tarde, el RT está corrupto. */
    fun markOnsetFrame(scheduledNanos: Long, actualNanos: Long) {
        if ((actualNanos - scheduledNanos) > refreshNanos * ONSET_JITTER_FACTOR) onsetFrameJitter = true
    }

    private companion object {
        const val MAX_SAMPLES = 600           // ~10 s a 60 Hz
        const val FRAME_DROP_FACTOR = 1.5     // frame que dura >1.5× el período = drop
        const val ONSET_JITTER_FACTOR = 1.5
    }
}
```

> **Por qué `criticalFrameJitter` solo importa en el frame del onset:** un drop a mitad de bloque no corrompe un RT ya capturado; solo el frame que **muestra el estímulo** define el onset autoritativo. Medir jank global pero gatear por el onset es la decisión psicométricamente correcta y evita falsos `DEVICE_UNRELIABLE` por un GC puntual fuera del onset.

### A.3 actual iOS — CADisplayLink (hoy ABSENT)

```kotlin
// iosMain — speed/domain/timing/TimingPolicy.ios.kt
// VEREDICTO: ABSENT en repo (SpeedTimebase.ios.kt:14 delega a fallback). Este es el contrato a implementar.
actual class FrameTimingProbe {
    private val link = DisplayLinkSampler()      // wrapper Obj-C de CADisplayLink

    actual fun start() = link.start()
    actual fun stop(): FrameStats {
        val s = link.stop()
        return FrameStats(
            sampledFrames = s.count,
            frameDropFraction = s.droppedFraction,         // basado en CADisplayLink.targetTimestamp vs timestamp
            maxFrameJitterNanos = s.maxJitterNanos,
            criticalFrameJitter = s.onsetJitter,
        )
    }
}
// CADisplayLink.timestamp + MISMA base (CACurrentMediaTime / mach_absolute_time) para el input touch.
// HASTA que exista: iOS devuelve NonAuthoritative(NO_DISPLAY_LINK) → jugable, sin profile-update.
```

### A.4 actual Desktop — downgrade explícito

```kotlin
// desktopMain — speed/domain/timing/TimingPolicy.desktop.kt
// No hay vsync garantizado portable → downgrade explícito, NUNCA fingir autoritativo.
actual class FrameTimingProbe {
    actual fun start() {}
    actual fun stop(): FrameStats = FrameStats(0, 0.0, 0L, criticalFrameJitter = false)
    // El clasificador (A.5) traduce esto a NonAuthoritative(SYSTEM_CLOCK_MONOTONIC), no a Authoritative.
}
```

### A.5 Clasificador puro (commonTest-able, sin device) — donde vive la Regla #1

```kotlin
// commonMain — speed/domain/timing/TimingPolicyClassifier.kt
object TimingPolicyClassifier {

    /** Decide la garantía a partir de señales REALES. Pura → testeable sin dispositivo. */
    fun classify(
        platformSource: TimestampSource,
        precision: InputTimestampPrecision,
        frameStats: FrameStats,
        clockDiscontinuityDetected: Boolean,
        thresholds: MetricThresholds = MetricThresholds.DEFAULT,
    ): TimebaseGuarantee = when {
        clockDiscontinuityDetected ->
            TimebaseGuarantee.DeviceUnreliable(UnreliableReason.CLOCK_DISCONTINUITY, frameStats)

        platformSource != TimestampSource.CHOREOGRAPHER_FRAME ->
            TimebaseGuarantee.NonAuthoritative(DowngradeReason.SYSTEM_CLOCK_MONOTONIC, precision)

        precision == InputTimestampPrecision.MILLIS_DERIVED ->
            // API<34: honesto pero no nanos-nativo → no autoritativo para RT de cola corta.
            TimebaseGuarantee.NonAuthoritative(DowngradeReason.MILLIS_DERIVED, precision)

        frameStats.criticalFrameJitter ->
            TimebaseGuarantee.DeviceUnreliable(UnreliableReason.CRITICAL_FRAME_JITTER, frameStats)

        frameStats.frameDropFraction > thresholds.contaminatedFrameDropThreshold ->
            TimebaseGuarantee.DeviceUnreliable(UnreliableReason.EXCESSIVE_FRAME_DROP, frameStats)

        else ->
            TimebaseGuarantee.Authoritative(platformSource, precision, frameStats)
    }
}
```

### A.6 Cómo gatea el `ScoreResult` (Regla #1, #6, #7)

El repo **no tiene un `ScoreResult` común** (verificado: cada módulo su tipo). El gate se aplica donde Speed produce `CognitiveProfileScore` (`SpeedScoreEngine.kt`, `preliminary=true` siempre). Patrón propuesto, idéntico a la convención sealed de Memory (`RciResult`):

```kotlin
// commonMain — speed/domain/score/GatedScoreResult.kt
sealed interface GatedScoreResult {
    /** Calidad+timebase OK → el score puede actualizar perfil. */
    data class ProfileEligible(val score: CognitiveProfileScore) : GatedScoreResult

    /** Jugable, feedback local, pero NO toca perfil (Regla #1). */
    data class LocalFeedbackOnly(
        val score: CognitiveProfileScore,   // se MUESTRA, con label honesto
        val reason: GateReason,             // UNTRUSTED_TIMEBASE | DEVICE_UNRELIABLE | LOW_QUALITY | INSUFFICIENT_N
    ) : GatedScoreResult

    /** Ni siquiera proxy mostrable (contaminada de raíz). */
    data class Invalidated(val reason: GateReason) : GatedScoreResult
}

enum class GateReason { UNTRUSTED_TIMEBASE, DEVICE_UNRELIABLE, LOW_QUALITY, INSUFFICIENT_N }

object ScoreGate {
    fun apply(
        score: CognitiveProfileScore,
        guarantee: TimebaseGuarantee,
        quality: SessionQuality,      // de SessionQualityEvaluator (SpeedEnums.kt:65)
        gaming: GamingVerdict,        // de la PARTE B
    ): GatedScoreResult = when {
        gaming.action == GamingAction.INVALIDATE ->
            GatedScoreResult.Invalidated(GateReason.LOW_QUALITY)

        guarantee is TimebaseGuarantee.DeviceUnreliable ->
            GatedScoreResult.LocalFeedbackOnly(score, GateReason.DEVICE_UNRELIABLE)

        guarantee is TimebaseGuarantee.NonAuthoritative ->
            // RT crítico sin timebase autoritativa → jugable, profile-update NO (Regla #1).
            GatedScoreResult.LocalFeedbackOnly(score, GateReason.UNTRUSTED_TIMEBASE)

        quality == SessionQuality.CONTAMINATED ->
            GatedScoreResult.Invalidated(GateReason.LOW_QUALITY)

        quality == SessionQuality.LOW || gaming.action == GamingAction.WITHHOLD_PROFILE ->
            GatedScoreResult.LocalFeedbackOnly(score, GateReason.LOW_QUALITY)

        else -> GatedScoreResult.ProfileEligible(score)
    }
}
```

**UI honesta (Regla #6) — mapeo de copy:**

| Estado | Copy | Prohibido |
|---|---|---|
| `ProfileEligible` | "Estimación preliminar" (sigue `preliminary=true` §15) | "tu velocidad de procesamiento es X" |
| `LocalFeedbackOnly(UNTRUSTED_TIMEBASE)` | "No actualizado por calidad de medición · proxy interno, no diagnóstico" | mostrar percentil/absoluto |
| `LocalFeedbackOnly(DEVICE_UNRELIABLE)` | "Tu dispositivo no garantizó precisión esta vez · resultado solo local" | guardar en histórico de perfil |
| `Invalidated` | "Datos insuficientes" / "calibrando" — **barras a `—`, jamás 0** (Regla #7) | render de 0.0 |

**Wiring que mata el hueco actual** — `SpeedSessionStateHolder.kt:259-260` deja de pasar literales:

```kotlin
// ANTES (gate decorativo):
QualitySignals(/* … */ frameDropFraction = 0.0, criticalFrameJitter = false)

// DESPUÉS (gate real):
val frameStats = frameTimingProbe.stop()                       // medido
val guarantee = TimingPolicyClassifier.classify(/* … */, frameStats, clockDiscontinuity)
QualitySignals(/* … */
    frameDropFraction = frameStats.frameDropFraction,
    criticalFrameJitter = frameStats.criticalFrameJitter,
)
```

### A.7 Tests del TimingPolicy

```kotlin
// commonTest — TimingPolicyClassifierTest.kt  (sin device, puro)
@Test fun `choreographer + nanos + clean frames → Authoritative`()
@Test fun `criticalFrameJitter on onset → DeviceUnreliable not LOW`()           // distingue jank-de-onset de jank-global
@Test fun `frameDrop over threshold → DeviceUnreliable`()
@Test fun `non-choreographer source → NonAuthoritative even with perfect frames`()  // desktop/iOS
@Test fun `MILLIS_DERIVED (api<34) → NonAuthoritative`()
@Test fun `clock discontinuity dominates all other signals`()

// commonTest — ScoreGateTest.kt
@Test fun `NonAuthoritative → LocalFeedbackOnly UNTRUSTED_TIMEBASE, score not null`() // jugable sí, perfil no
@Test fun `DeviceUnreliable → never ProfileEligible`()
@Test fun `Invalidated never renders 0 — bars stay null`()                            // Regla #7

// androidUnitTest — FrameTimingProbeTest.kt  (Robolectric Choreographer)
@Test fun `injected 100ms stall frame → frameDropFraction reflects it`()
@Test fun `markOnsetFrame within budget → criticalFrameJitter false`()
```

> **Test seam para jank sin device:** `FrameTimingProbe` se inyecta vía `rememberFrameTimingProbe()`; en tests se sustituye por un `FakeFrameTimingProbe(stats)` que devuelve `FrameStats` arbitrarios — así el clasificador y el gate se prueban end-to-end sin Choreographer real. El test *con* Choreographer (Robolectric) cubre solo el muestreador.

---

## PARTE B — AntiGamingFramework (subsistema 4)

### B.0 Verdad de partida + arquitectura

**Lo que existe de verdad (IMPLEMENTED+TESTED):** filtro de anticipación (Focus/Speed), guard RT<400 AGRS (Decisiones), SSRT-`trustworthy` waiting-strategy (Focus), impulsividad fast-commission (Focus), quality-gate (Speed). **Lo que es solo simulación offline:** cadence/fraude Arena. **Lo que NO existe:** sesgo posicional (neutralizado con `spatialEntropy=1.0`), keyword-stuffing (capa Creativity `BLOCKED`), perfiles sintéticos adversarios fuera de Focus+Decisiones+Arena.

**Contrato común** (un detector = una función pura sobre la telemetría que YA capturamos; ningún detector mide algo nuevo a nivel UI salvo que el repo ya lo capture):

```kotlin
// commonMain — antigaming/Detector.kt
data class TrialTelemetry(                       // derivable de RecordedTrial + TimebaseGuarantee
    val index: Int,
    val rtNanos: Long?,                          // null = omisión
    val outcome: TrialOutcome,                   // HIT/MISS/FALSE_ALARM/CORRECT_REJECTION/ANTICIPATION
    val correct: Boolean,
    val responseKey: ResponseKey?,               // LEFT/RIGHT/PRIMARY (ya existe en Stimulus.kt)
    val correctKey: ResponseKey?,
    val itemId: String?,                         // para repeated-bank
    val revealedExplanation: Boolean,            // para low-effort reveal
    val timebase: TimebaseGuarantee,
)

interface GamingDetector {
    val id: String
    fun evaluate(trials: List<TrialTelemetry>, ctx: SessionContext): DetectorSignal
}

data class DetectorSignal(
    val detectorId: String,
    val triggered: Boolean,
    val strength: Double,            // 0..1 (continuo, no booleano duro)
    val recommendedAction: GamingAction,
    val evidence: Map<String, Double>,
)

enum class GamingAction { NONE, FLAG_ONLY, WITHHOLD_PROFILE, INVALIDATE }

// Agregador: la acción más severa gana; nunca penaliza score como "bajo rendimiento" (Regla #7).
data class GamingVerdict(val action: GamingAction, val signals: List<DetectorSignal>)
```

### B.1 Los 12 detectores

Para cada uno: **qué observa · threshold inicial · falsos positivos esperables · test unitario · test adversarial (perfil sintético) · acción sobre ScoreResult · veredicto de estado real.**

---

**D1 — Rhythm tapping (cadencia constante)**

- **Observa:** CV de los intervalos inter-respuesta (IRI). Tapeo robótico ⇒ IRI casi constante e independiente del estímulo.
- **Threshold inicial:** `iriCv < 0.08` **Y** correlación(RT, dificultad-del-trial) ≈ 0 sobre ≥ 12 trials.
- **Falsos positivos:** usuario genuinamente muy regular en tarea de ritmo fijo (ISI fijo). Mitigación: solo dispara si además `accuracy` no covaría con la manipulación experimental.
- **Test unitario:** `iri constant + accuracy flat → triggered`.
- **Test adversarial:** `RobotTapperProfile` (IRI = ISI exacto, ignora estímulo) → `WITHHOLD_PROFILE`.
- **Acción:** `WITHHOLD_PROFILE`.
- **Veredicto:** **ABSENT en MindBoost real** (cadencia solo en Arena sintético). → **REVISE crítico** hasta implementar.

```kotlin
class RhythmTappingDetector : GamingDetector {
    override fun evaluate(t, ctx): DetectorSignal {
        val iris = t.zipWithNext { a, b -> (b.rtNanos ?: return@zipWithNext null) - (a.rtNanos ?: 0) }
                    .filterNotNull()
        if (iris.size < MIN_IRI) return idle()
        val cv = stdev(iris) / mean(iris)
        val rtDiffCorr = correlation(t.mapNotNull { it.rtNanos }, ctx.trialDifficulties)
        val triggered = cv < IRI_CV_FLOOR && rtDiffCorr.absoluteValue < FLAT_CORR
        return signal(triggered, strength = (IRI_CV_FLOOR - cv).coerceAtLeast(0.0) / IRI_CV_FLOOR,
                      WITHHOLD_PROFILE, mapOf("iriCv" to cv))
    }
    companion object { const val MIN_IRI = 12; const val IRI_CV_FLOOR = 0.08; const val FLAT_CORR = 0.1 }
}
```

---

**D2 — Waiting strategy (esperar para no fallar)**

- **Observa:** en stop-signal, inflación de go-RT para "ganar" al stop. **Ya existe** vía `SsrtCalculator.trustworthy` cuando `p(respond|signal) ∉ [0.25, 0.75]`.
- **Threshold:** `pRespond < 0.25` (frenado estratégico) — reusar `SsrtCalculator.kt:39-53`.
- **Falsos positivos:** participante naturalmente cauto. Aceptable: solo invalida el SSRT, no el bloque entero.
- **Test unitario:** existe — `SsrtCalculatorTest.kt:30 flagsGaming_whenProbabilityFarFromHalf`.
- **Test adversarial:** existe — `SimulatorLoopTest.kt:94 gamerOnStopSignal_breaksSsrtEstimate`.
- **Acción:** `WITHHOLD_PROFILE` para la métrica SSRT (no para todo).
- **Veredicto:** **IMPLEMENTED+TESTED** → solo envolver en el contrato `GamingDetector`.

---

**D3 — Speed-accuracy hacking (sacrificar precisión por velocidad o viceversa para inflar una barra)**

- **Observa:** punto de operación fuera de la SAT esperada — RT ultrabajo con accuracy en azar, o RT inflado con accuracy techo en tarea cronometrada.
- **Threshold:** `medianRt < chanceFloorMs && accuracy ≤ chance + 0.05` (responder rápido sin procesar) **o** `accuracy ≥ 0.98 && rtCv` anómalo. El `SpeedScoreEngine.kt:97-99` ya penaliza `pc<0.80 || cv>0.30` con `×0.50` — D3 lo **eleva de penalización a invalidación** cuando es patológico.
- **Falsos positivos:** experto real rápido-y-preciso (RT bajo + accuracy alta NO dispara: la condición exige accuracy ≈ azar).
- **Test unitario:** `fast + chance-accuracy → INVALIDATE`.
- **Test adversarial:** `MashButtonProfile` (RT≈120ms, accuracy 0.5).
- **Acción:** `INVALIDATE`.
- **Veredicto:** **PARTIAL** (ceiling penalty existe, no como invalidación) → REVISE.

---

**D4 — Too-fast-to-process (por debajo del piso fisiológico)**

- **Observa:** RT por debajo del piso de procesamiento. **Ya existe** como filtro de anticipación (`TrialClassifier` Focus, `MetricCalculator.kt:130-147` Speed, umbral 100ms).
- **Threshold:** RT < `anticipationFloorMs` (existe). D4 agrega: si `anticipationRate > 0.30` del bloque ⇒ no es ruido, es estrategia.
- **Falsos positivos:** lapsus motor aislado (mitigado: tasa de bloque, no trial suelto).
- **Test unitario:** existe (`TrialClassifierTest.kt:42`, `MetricCalculatorTest.kt:48`).
- **Test adversarial:** `AnticipatorProfile` (responde antes del estímulo sistemáticamente).
- **Acción:** trial → descartado (existe); bloque con tasa alta → `WITHHOLD_PROFILE` (nuevo).
- **Veredicto:** **IMPLEMENTED+TESTED** a nivel trial; **PARTIAL** a nivel bloque.

---

**D5 — Memorization (banco fijo memorizado)**

- **Observa:** caída brusca de RT + subida de accuracy en ítems **repetidos exactos** entre sesiones, sin transferencia a isomorfos. Crítico porque Logic(6), Language(36) son fijos y memorizables.
- **Threshold:** para ítem visto ≥ 2ª vez: `rt drop > 40% && accuracy → 1.0` mientras isomorfo no entrenado se mantiene plano.
- **Falsos positivos:** aprendizaje genuino de la *regla* (mitigado: exige que el isomorfo NO mejore; si mejora, es transfer real, no memorización). Reusa el principio ya implementado en `RealImprovementCriterion.kt:42-68` (untrained probe).
- **Test unitario:** `repeated item rt-collapse + isomorph flat → triggered`.
- **Test adversarial:** `RoteMemoryProfile` (memoriza respuesta por itemId).
- **Acción:** `WITHHOLD_PROFILE` + señal a rotación de banco.
- **Veredicto:** **ABSENT** (no hay exposure tracking por ítem; grep `exposure`=0). → **BLOCK** del claim de mejora para Logic/Language hasta tener banco rotado + este detector.

```kotlin
class MemorizationDetector : GamingDetector {
    override fun evaluate(t, ctx): DetectorSignal {
        val repeated = t.filter { ctx.priorExposures[it.itemId] ?: 0 >= 1 }
        if (repeated.size < MIN_REPEATED) return idle()
        val rtDropRatio = 1 - mean(repeated.rt) / ctx.firstExposureMeanRt
        val isomorphGain = ctx.isomorphAccuracyDelta   // de RealImprovementCriterion
        val triggered = rtDropRatio > RT_COLLAPSE && isomorphGain < ISOMORPH_FLOOR
        return signal(triggered, rtDropRatio, WITHHOLD_PROFILE,
                      mapOf("rtDrop" to rtDropRatio, "isomorphGain" to isomorphGain))
    }
    companion object { const val MIN_REPEATED = 4; const val RT_COLLAPSE = 0.40; const val ISOMORPH_FLOOR = 0.05 }
}
```

---

**D6 — Answer-position bias (responder siempre el mismo lado/posición)**

- **Observa:** distribución de `responseKey` (LEFT/RIGHT) vs la distribución de `correctKey`. Entropía baja de respuesta + accuracy ≈ baseRate ⇒ no procesa, elige posición fija.
- **Threshold:** `responseEntropy < 0.4 bits && accuracy ≤ expectedFromBias + 0.05`. **Hoy `spatialEntropy=1.0` está hardcodeado** (`BayesArenaStateHolder.kt:71`) — D6 calcula la entropía REAL y la inyecta en el término `entropyFactor` del AGRS (`ScoringEquations.kt:205`), que hoy nunca recibe entropía real.
- **Falsos positivos:** bloque donde por azar la respuesta correcta es mayoritariamente un lado (mitigado: compara contra `correctKey` real del bloque, no contra 0.5 ciego).
- **Test unitario:** `all-LEFT responses + balanced correct → triggered`.
- **Test adversarial:** `LeftStubbornProfile` (siempre LEFT).
- **Acción:** `INVALIDATE` (señal de no-procesamiento).
- **Veredicto:** **ABSENT** (neutralizado por diseño con `spatialEntropy=1.0`). → **REVISE crítico**: el término AGRS existe pero está muerto.

```kotlin
class PositionBiasDetector : GamingDetector {
    override fun evaluate(t, ctx): DetectorSignal {
        val choices = t.mapNotNull { it.responseKey }.filter { it.isDirectional }
        if (choices.size < MIN_CHOICE) return idle()
        val entropy = shannonEntropy(choices.groupingBy { it }.eachCount())
        val expectedFromBias = dominantSideShare(t)   // si siempre eliges el lado modal
        val triggered = entropy < ENTROPY_FLOOR && observedAccuracy(t) <= expectedFromBias + SLACK
        return signal(triggered, (ENTROPY_FLOOR - entropy).coerceAtLeast(0.0) / ENTROPY_FLOOR,
                      INVALIDATE, mapOf("responseEntropy" to entropy))
    }
    companion object { const val MIN_CHOICE = 10; const val ENTROPY_FLOOR = 0.4; const val SLACK = 0.05 }
}
```

---

**D7 — Repeated-bank exploitation (explotar reexposición del banco fijo)**

- **Observa:** complementa D5 a nivel *banco*: fracción de la sesión compuesta por ítems ya servidos en sesiones previas. Crítico: Logic sirve los **mismos 6 en orden fijo**.
- **Threshold:** `reexposedFraction > 0.5` del bloque servido **y** ganancia concentrada en reexpuestos.
- **Falsos positivos:** banco pequeño legítimo (es justamente el problema → la acción es retener perfil, no culpar al usuario).
- **Test unitario:** `>50% items previously seen → triggered`.
- **Test adversarial:** `BankGrinderProfile` (repite sesión idéntica).
- **Acción:** `WITHHOLD_PROFILE` + telemetría que fuerza rotación (Regla #5).
- **Veredicto:** **ABSENT** (sin exposure tracking, sin holdout activo — `isEval` reservado a Ola-4 no implementada). → **BLOCK** del claim de progreso de Logic/Language.

---

**D8 — Keyword-stuffing (inflar respuesta abierta con palabras clave)**

- **Observa:** en Creativity/respuesta abierta, densidad de keywords premiadas sin estructura — repetición, longitud sin diversidad léxica (type-token ratio bajo).
- **Threshold:** `typeTokenRatio < 0.4 && keywordDensity > 0.3 && ideaCount inflado por duplicados`.
- **Falsos positivos:** estilo conciso legítimo (mitigado: solo dispara con TTR bajo *y* duplicación).
- **Test unitario:** `repeated keywords low-TTR → triggered`.
- **Test adversarial:** `KeywordSpammerProfile`.
- **Acción:** `INVALIDATE` la métrica de fluidez/originalidad.
- **Veredicto:** **ABSENT + BLOQUEADO por diseño** (`CreativityAiLayers.kt:44 BLOCKED`; validator solo chequea timing/no-vacío/minIdeas). → **BLOCK**: cualquier score de creatividad de contenido es no-confiable hoy; mantener Creativity en feedback-local sin profile-update.

---

**D9 — Low-effort reveal (revelar respuesta/explicación y "responder" después)**

- **Observa:** flag `revealedExplanation` antes de responder, o RT post-reveal anómalamente preciso. Útil en Logic/Language/Decisiones donde hay explicación.
- **Threshold:** `revealedThenCorrectFraction > 0.5`.
- **Falsos positivos:** usuario que estudia la explicación tras fallar (mitigado: solo cuenta reveal **antes** del commit).
- **Test unitario:** `reveal-before-commit + correct → triggered`.
- **Test adversarial:** `PeekerProfile`.
- **Acción:** `WITHHOLD_PROFILE` para los trials revelados; el resto cuenta.
- **Veredicto:** **ABSENT** (no se captura orden reveal↔commit). → REVISE.

---

**D10 — Motor/tecleo contamination (ruido motor, no cognitivo)**

- **Observa:** ráfagas de toques múltiples por trial, doble-tap, toques fuera de la zona de respuesta — RT corrompido por motor, no por procesamiento.
- **Threshold:** `multiTouchTrials / total > 0.15` o intervalos sub-50ms entre toques del mismo trial.
- **Falsos positivos:** temblor genuino / accesibilidad → **NO culpar**; reclasificar a `DEVICE_UNRELIABLE`/`LocalFeedbackOnly`, no a fraude (cruza con A.6).
- **Test unitario:** `double-tap bursts → flagged as motor-contam not gaming`.
- **Test adversarial:** `ButtonMasherProfile` (mash continuo).
- **Acción:** `WITHHOLD_PROFILE` (calidad), **nunca penaliza score** (Regla #7).
- **Veredicto:** **ABSENT** (ningún ejercicio mide multi-touch/cadencia real). → REVISE.

---

**D11 — Abandonment manipulation (abandonar para evitar score bajo)**

- **Observa:** patrón de abandono selectivo — abandona bloques difíciles, completa fáciles, para inflar el promedio retenido.
- **Threshold:** `abandonRate covaría con dificultad (corr > 0.5)`. Existe persistencia de abandono (`EmotionSessionStateHolder.kt:159-163`).
- **Falsos positivos en ejercicios emocionales/frustración:** **PROHIBIDO penalizar** (Regla #8 — abandono ahí es señal de seguridad). D11 **se desactiva** para módulo Emotions y para cualquier ejercicio marcado `frustrationInductive`.
- **Test unitario:** `abandons hard cognitive blocks selectively → triggered (logic)`.
- **Test adversarial:** `QuitterProfile` (cognitivo) **vs** `SafetyExitProfile` (emocional → NUNCA dispara).
- **Acción:** `WITHHOLD_PROFILE` (solo módulos no-emocionales).
- **Veredicto:** **PARTIAL** (abandono se persiste; el detector de manipulación selectiva no existe). → REVISE. La salvaguarda Regla #8 es **bloqueante de diseño**.

```kotlin
class AbandonmentManipulationDetector(private val moduleKind: ModuleKind) : GamingDetector {
    override fun evaluate(t, ctx): DetectorSignal {
        // Regla #8: en módulos emocionales/frustración, abandono = seguridad, NUNCA culpa.
        if (moduleKind == ModuleKind.EMOTIONAL || ctx.frustrationInductive) return idle()
        val corr = correlation(ctx.blockDifficulties, ctx.blockAbandonFlags.map { if (it) 1.0 else 0.0 })
        val triggered = corr > SELECTIVE_ABANDON_CORR
        return signal(triggered, corr.coerceIn(0.0, 1.0), WITHHOLD_PROFILE, mapOf("abandonDiffCorr" to corr))
    }
    companion object { const val SELECTIVE_ABANDON_CORR = 0.5 }
}
```

---

**D12 — Synthetic adversarial profiles (perfiles sintéticos adversarios) — el harness, no un detector más**

Esto es la **Regla #2 operacionalizada**: una suite que instancia bots y verifica que **cada detector D1–D11 los atrapa** y que un `HonestUserProfile` **no** dispara nada (control de falsos positivos). Es el equivalente al `RedTeamAdversarialTest` de Arena, pero sobre los ejercicios reales de MindBoost.

- **Observa:** N/A — genera telemetría adversaria y la pasa por el agregador.
- **Perfiles:** `RobotTapper`(D1), `WaitingGamer`(D2), `MashButton`(D3,D10), `Anticipator`(D4), `RoteMemory`(D5), `LeftStubborn`(D6), `BankGrinder`(D7), `KeywordSpammer`(D8), `Peeker`(D9), `Quitter`(D11), `SafetyExit`(control Regla #8), **`HonestUser`(control FP)**.
- **Test adversarial (la suite ES el test):**

```kotlin
// commonTest — AntiGamingRedTeamTest.kt
@Test fun `every adversarial profile is caught by its detector`() {
    adversaries.forEach { (profile, expectedDetector, expectedAction) ->
        val verdict = framework.evaluate(profile.synthesize(seed = 42L))
        assertTrue(verdict.signals.any { it.detectorId == expectedDetector && it.triggered },
                   "MISS: $expectedDetector did not catch ${profile.name}")
        assertEquals(expectedAction, verdict.action)
    }
}

@Test fun `honest user triggers no detector — false-positive control`() {
    val verdict = framework.evaluate(HonestUserProfile.synthesize(seed = 7L))
    assertEquals(GamingAction.NONE, verdict.action)   // FP rate guard
}

@Test fun `safety exit in emotional module never penalized — Regla 8`() {
    val verdict = framework.evaluate(SafetyExitProfile.synthesize(moduleKind = EMOTIONAL))
    assertEquals(GamingAction.NONE, verdict.action)
}
```

- **Acción sobre ScoreResult:** ninguna directa; es la red anti-regresión que hace cumplir "implementado+testeado". **Sin esta suite verde, ningún detector cuenta (Regla #2).**
- **Veredicto:** **ABSENT en MindBoost real** (solo Arena tiene su `RedTeamAdversarialTest`, fuera de cognitive-training). → **BLOCK**: hasta que exista, todos los detectores D1,D3,D5,D6,D7,D8,D9,D10,D11 están "diseñados" y por Regla #2 **no cuentan**.

---

### B.2 Tabla-resumen de veredictos (Regla #2 — honestidad brutal)

| ID | Detector | Estado real en repo | Acción | Veredicto Regla #2 |
|---|---|---|---|---|
| D1 | Rhythm tapping | ABSENT (cadencia solo Arena synth) | WITHHOLD | **REVISE crítico** |
| D2 | Waiting strategy | **IMPLEMENTED+TESTED** (SSRT trustworthy) | WITHHOLD(SSRT) | OK |
| D3 | Speed-accuracy hacking | PARTIAL (ceiling penalty ×0.5, no invalida) | INVALIDATE | REVISE |
| D4 | Too-fast-to-process | **IMPLEMENTED+TESTED** (trial) / PARTIAL (bloque) | discard/WITHHOLD | OK trial |
| D5 | Memorization | ABSENT (sin exposure tracking) | WITHHOLD | **BLOCK** claim Logic/Language |
| D6 | Answer-position bias | ABSENT (`spatialEntropy=1.0` muerto) | INVALIDATE | **REVISE crítico** |
| D7 | Repeated-bank exploit | ABSENT (sin holdout activo) | WITHHOLD | **BLOCK** claim progreso |
| D8 | Keyword-stuffing | ABSENT + BLOCKED por diseño | INVALIDATE | **BLOCK** score creatividad |
| D9 | Low-effort reveal | ABSENT (sin orden reveal↔commit) | WITHHOLD | REVISE |
| D10 | Motor/tecleo contam. | ABSENT (sin multi-touch capture) | WITHHOLD (calidad) | REVISE |
| D11 | Abandonment manip. | PARTIAL (abandono persiste; manip. no) | WITHHOLD (no-emo) | REVISE + Regla #8 bloqueante |
| D12 | Synthetic adversaries | ABSENT (solo Arena) | harness | **BLOCK** — sin esto, D1/D3/D5–D11 no cuentan |

**Síntesis:** de 12, **2 cuentan hoy** (D2, D4-trial); el resto es diseño/placeholder. Esto confirma el ground-truth del brief ("~4 implementadas, ~2 simulación, ~2 ausentes") y, por **Regla #2**, el AntiGamingFramework como conjunto está en estado **REVISE/BLOCK** hasta que D12 (la suite adversaria sobre ejercicios reales) esté verde.

---

### B.3 Reglas transversales aplicadas (#3, #4, #5, #7, #9, #10)

- **#3 (n insuficiente):** cada detector tiene `MIN_*` (p. ej. `MIN_IRI=12`, `MIN_CHOICE=10`, `MIN_REPEATED=4`). Bajo n_min → `DetectorSignal.idle()` (no dispara, no afirma). El gate global: `INSUFFICIENT_N → LocalFeedbackOnly`, jamás `0` (Regla #7). n_efectivo = trials válidos tras filtro de anticipación/contaminación. Agregación inter-sesión solo para D5/D7 (memorización/banco necesitan historial); el resto es intra-sesión. IC/etiqueta: el score arrastra `confident=false` (Focus `MetricValue`) o `preliminary=true` (Speed).
- **#4 (far-transfer copy):** los detectores **no** generan copy; cualquier feedback usa el `TransferLabel` honesto existente (`TASK_ONLY/PLAUSIBLE/NOT_DEMONSTRATED/PLACEBO_RISK`). Ningún detector justifica claims de vida real/inteligencia/emoción, ni para negarlos.
- **#5 (validación de banco):** D5/D7 son la **palanca** que hace cumplir la regla — mientras Logic(6)/Language(36) sean fijos sin holdout/inter-rater, D5/D7 fuerzan `WITHHOLD_PROFILE` permanente → el claim queda **BLOCK** por construcción.
- **#7 (null≠0):** `GamingAction` nunca escribe un score bajo; reclasifica a `LocalFeedbackOnly`/`Invalidated` con barras `null`/`—`.
- **#8:** D11 desactivado en módulos emocionales (bloqueante de diseño, test dedicado).
- **#9 (redundancia):** D2 y D4 comparten varianza (RT-de-cola); D1 y D10 comparten varianza (regularidad motora). **Diferenciados** por la señal primaria (D2=p-respond, D4=piso fisiológico; D1=independencia-de-estímulo, D10=multi-touch). D5 y D7 se **fusionan** en un solo subsistema de exposición (uno intra-ítem, otro intra-banco) compartiendo `priorExposures`.
- **#10 (mejores señales, no más):** la prioridad de implementación NO es "los 12 ya", sino **D6 + D12 primero** (desbloquean el término AGRS muerto y la red anti-regresión), luego D5/D7 (desbloquean claims), luego D1/D3. D8/D9/D10 quedan tras decidir si Creativity/reveal/multi-touch justifican la captura extra — menos detectores limpios > 12 ruidosos.

---

## Resumen ejecutivo

**TimingPolicy (3):** el repo tiene aritmética RT autoritativa y captura Android frame-accurate reales, pero el **gate `device_unreliable` está muerto** (`SpeedSessionStateHolder.kt:259-260` pasa `0.0`/`false`). El diseño añade `FrameTimingProbe` (expect/actual: Android Choreographer real, iOS CADisplayLink hoy ABSENT, Desktop downgrade explícito) que **mide** jank/drop, un `TimingPolicyClassifier` puro que produce `TimebaseGuarantee{Authoritative|NonAuthoritative|DeviceUnreliable}`, y un `ScoreGate` que convierte eso en `GatedScoreResult{ProfileEligible|LocalFeedbackOnly|Invalidated}`. **Regla #1 cumplida:** RT sin timebase autoritativa ⇒ jugable + feedback local, **profile-update NO**.

**AntiGamingFramework (4):** 12 detectores especificados con threshold/FP/test/adversarial/acción. **Honestidad Regla #2:** solo **D2 (waiting) y D4-trial (too-fast)** están IMPLEMENTED+TESTED hoy; **D5/D7/D8/D12 son BLOCK** (memorización, banco, keyword-stuffing, suite adversaria), **D1/D3/D6/D9/D10/D11 son REVISE**. La pieza que valida todo es **D12** (perfiles sintéticos adversarios sobre ejercicios reales, hoy solo en Arena): sin esa suite verde, los detectores "diseñados" **no cuentan**.

**Archivos reales tocados/creados (rutas absolutas):**
- Anclaje existente (no reescribir, envolver): `C:\Users\curro\AndroidStudioProjects\MindOS\shared\src\commonMain\kotlin\com\mindos\app\features\mindboost\speed\domain\TimebasePolicy.kt`, `...\speed\domain\quality\SessionQualityEvaluator.kt`, `...\speed\domain\SpeedEnums.kt`
- Hueco a cerrar: `C:\Users\curro\AndroidStudioProjects\MindOS\shared\src\commonMain\kotlin\com\mindos\app\features\mindboost\speed\presentation\SpeedSessionStateHolder.kt:259-260`
- Plataforma timebase existente: `...\speed\presentation\SpeedTimebase.android.kt`, `SpeedTimebaseFallback.kt`, `SpeedTimebase.ios.kt`
- Nuevos (a crear): `...\speed\domain\timing\TimingPolicy.kt` + `.android.kt`/`.ios.kt`/`.desktop.kt`, `TimingPolicyClassifier.kt`, `...\speed\domain\score\GatedScoreResult.kt`, `...\mindboost\antigaming\Detector.kt` + detectores D1–D11, `...\antigaming\AntiGamingRedTeamTest.kt` (D12).

---

## bank_copyscanner

I have the real scanner infrastructure confirmed. Now I'll design both systems, anchored to the actual code.

---

# (5) `ItemBankValidation` + (6) `CopySafetyScanner`

> **Anchoring note.** Ground-truth from the repo: **zero** psychometric validation exists today across Logic (6 authored), Language (36 authored), Emotions (PAUSA 24 + SEPARATOR 40, `isEval` reserved-but-never-served), Creativity (14 static), Decisiones (procedural, no bank). No `discriminability` (IRT), no `culturalLoad`, no `interRater`, no `exposure` tracking. And there is **no global copy gate** — only `arenaTerminologyScanner` + `emotionsTerminologyScanner`, folder-scoped, hooked to `check`. Both designs below are written to **mirror the existing scanner task shape** (`build.gradle.kts:198-355`) and the existing **gated-result pattern** (`RciResult`, `SsrtResult`, `CognitiveProfileScore.preliminary`) so they drop into the codebase without inventing new conventions.

---

## Part A — `ItemBankValidation`

### A.0 Where it lives & what it gates

| Concern | Decision (anchored) |
|---|---|
| Package | `com.mindos.app.features.mindboost.shared.itembank.validation` (cross-module; Logic/Language consume it; Emotions/Decisiones opt-in) |
| Score gate | Adds `BANK_CONTAMINATED` as a **first-class invalidation reason** in each module's score state machine — never "low performance" (Rule 7). Mirrors `CognitiveProfileScore.preliminary` / `RciResult.NotEnoughData`. |
| n-rule (Rule 3) | Per-cell `n_min`, `n_effective`, `null`/`calibrating`/`blocked` thresholds — defined in `ItemValidationConfig`, enforced in pipeline, surfaced as `IncertaintyLabel`. |
| Anti-gaming tie (Rule 2) | `contaminationFlags` is **populated by the implemented detectors that already exist** (anticipation filter, AGRS, SSRT-trustworthy), not by "designed" ones. Items flagged `LEAKAGE_SUSPECTED` only when a real detector fired. |

### A.1 Per-item record (the structure you asked for)

```kotlin
package com.mindos.app.features.mindboost.shared.itembank.validation

import kotlin.time.Instant

/**
 * Immutable psychometric record for ONE bank item.
 *
 * HONESTY INVARIANTS (enforced in init):
 *  - discriminability/difficulty are NULLABLE: an un-piloted item has value == null, NOT 0.0 (Rule 7).
 *  - validationStatus == VALIDATED  REQUIRES  interRaterAgreement >= MIN_INTER_RATER and a non-null,
 *    in-range discriminability backed by n_effective >= n_min. You cannot hand-set VALIDATED.
 *  - retiredAt != null  IFF  validationStatus == RETIRED.
 *
 * Anchors to repo reality: today Logic items carry only difficulty (1..5) + targetSkills; this record is
 * the *target shape* the bank must migrate to. constructTag re-uses existing enums (LogicSkill / TraitCode
 * / EmotionCategory) via [ConstructTag]; nothing new is invented for the construct taxonomy.
 */
data class ItemPsychometrics(
    val itemId: String,
    val version: Int,                       // bumped on ANY content edit; old version retired, not mutated
    val constructTag: ConstructTag,         // wraps LogicSkill / TraitCode / EmotionCategory (existing)
    val difficulty: Double?,                // IRT b-param OR empirical p-value; null = un-piloted (NOT 0)
    val discriminability: Double?,          // IRT a-param OR point-biserial r_pb; null = un-piloted (NOT 0)
    val culturalLoad: LoadLevel,            // LOW/MEDIUM/HIGH — rater-assessed bias risk (Rule 5)
    val lexicalLoad: LoadLevel,             // reading-demand confound (Language esp.)
    val motorLoad: LoadLevel,               // tap/drag demand confound (Speed/Focus esp.)
    val priorKnowledgeRisk: PriorRisk,      // NONE / LOW / HIGH_PRIOR — HIGH_PRIOR blocks scoring use
    val interRaterAgreement: Double?,       // Krippendorff α or % agreement, 0..1; null = not yet rated
    val holdoutGroup: HoldoutGroup,         // TRAIN / HOLDOUT_EVAL / ROTATION_POOL
    val exposureCount: Long,                // times SERVED to ANY user; drives rotation/leakage (Rule 5)
    val retiredAt: Instant?,                // set when version superseded OR over-exposed
    val contaminationFlags: Set<ContaminationFlag>,  // populated ONLY by real detectors (Rule 2)
    val generatedBy: Authorship,            // HUMAN_AUTHOR / PROCEDURAL_SEED / AI_ASSISTED(blocked today)
    val reviewedBy: Set<String>,            // rater ids; size >= MIN_RATERS required for VALIDATED
    val validationStatus: ValidationStatus, // single source of truth for "can this item score?"
) {
    init {
        require(version >= 1) { "version is 1-based" }
        require(interRaterAgreement == null || interRaterAgreement in 0.0..1.0)
        require(culturalLoadConsistent()) { "culturalLoad cannot be HIGH while VALIDATED" }
        require((retiredAt != null) == (validationStatus == ValidationStatus.RETIRED)) {
            "retiredAt and RETIRED status must agree"
        }
        if (validationStatus == ValidationStatus.VALIDATED) {
            // Hard gate: VALIDATED is EARNED, never asserted.
            requireNotNull(discriminability) { "VALIDATED requires piloted discriminability" }
            requireNotNull(interRaterAgreement) { "VALIDATED requires inter-rater" }
            require(interRaterAgreement >= MIN_INTER_RATER) { "inter-rater below 0.80 (Rule 5)" }
            require(priorKnowledgeRisk != PriorRisk.HIGH_PRIOR) { "HIGH_PRIOR cannot be VALIDATED" }
            require(reviewedBy.size >= MIN_RATERS) { "need >= $MIN_RATERS independent raters" }
        }
    }

    private fun culturalLoadConsistent(): Boolean =
        validationStatus != ValidationStatus.VALIDATED || culturalLoad != LoadLevel.HIGH

    /** Read-only: can this item legitimately contribute to a USER-FACING score right now? */
    val isScoreEligible: Boolean
        get() = validationStatus == ValidationStatus.VALIDATED &&
            contaminationFlags.none { it.blocksScoring } &&
            priorKnowledgeRisk != PriorRisk.HIGH_PRIOR

    companion object {
        const val MIN_INTER_RATER = 0.80   // Rule 5 hard floor
        const val MIN_RATERS = 2           // independent raters before α is meaningful
    }
}

enum class LoadLevel { LOW, MEDIUM, HIGH }
enum class PriorRisk { NONE, LOW, HIGH_PRIOR }       // HIGH_PRIOR = answerable from prior knowledge
enum class HoldoutGroup { TRAIN, HOLDOUT_EVAL, ROTATION_POOL }
enum class Authorship { HUMAN_AUTHOR, PROCEDURAL_SEED, AI_ASSISTED }

enum class ValidationStatus {
    DRAFT,            // authored, never piloted -> difficulty/discriminability null
    PILOTING,         // collecting responses, n_effective < n_min -> CALIBRATING, not scoreable
    CALIBRATING,      // enough to estimate but band wide / inter-rater pending
    VALIDATED,        // passes ALL gates -> score-eligible
    QUARANTINED,      // a detector flagged contamination -> BANK_CONTAMINATED downstream
    RETIRED,          // superseded version OR over-exposed -> never served again
}

/** Flags come from detectors that ACTUALLY RUN (Rule 2). A flag with blocksScoring=true forces null. */
enum class ContaminationFlag(val blocksScoring: Boolean) {
    OVER_EXPOSED(blocksScoring = true),          // exposureCount past rotation ceiling -> memorization
    HIGH_PRIOR_DETECTED(blocksScoring = true),   // empirical: too-high p-value at first exposure
    INTER_RATER_FAILED(blocksScoring = true),    // α < 0.80
    LEAKAGE_SUSPECTED(blocksScoring = true),     // anomalous accuracy spike correlated w/ exposure
    CULTURAL_BIAS_SUSPECTED(blocksScoring = false), // DIF signal — warn, don't hard-block alone
    LOW_DISCRIMINATION(blocksScoring = true),    // r_pb < floor -> item measures nothing useful (Rule 10)
}

/** Wraps the EXISTING construct enums; no new taxonomy invented. */
sealed interface ConstructTag {
    data class Logic(val skill: String) : ConstructTag      // backed by LogicSkill
    data class Language(val trait: String) : ConstructTag   // backed by TraitCode
    data class Emotion(val category: String) : ConstructTag // backed by EmotionCategory
}
```

### A.2 Validation config — the n-rule, made explicit (Rule 3)

```kotlin
/**
 * Per-CELL thresholds. A "cell" = (constructTag, difficultyBand). n_min/n_effective semantics are
 * defined HERE so every module reads the same definition of "enough data".
 */
data class ItemValidationConfig(
    val nMinPerCell: Int = 200,            // responses before discriminability is estimable
    val nMinPerItem: Int = 30,             // per-item floor for a usable p-value
    val interRaterFloor: Double = 0.80,    // Rule 5
    val discriminationFloor: Double = 0.20, // point-biserial r_pb floor; below -> LOW_DISCRIMINATION
    val highPriorPValue: Double = 0.95,    // p > .95 at FIRST exposure => HIGH_PRIOR_DETECTED
    val exposureCeiling: Long = 1_000,     // serves before forced rotation (anti-memorization, Rule 5)
    val leakageExposureWindow: Long = 50,  // window over which an accuracy spike = leakage signal
    val minHoldoutFraction: Double = 0.20, // >= 20% of every cell reserved as HOLDOUT_EVAL
) {
    init {
        require(interRaterFloor >= 0.80) { "cannot lower the Rule 5 floor below 0.80" }
        require(minHoldoutFraction >= 0.10) { "holdout cannot be token-only" }
    }
}

/**
 * n_effective is NOT raw count. Contaminated trials (anticipations, AGRS-invalid, SSRT-untrustworthy)
 * are subtracted BEFORE counting — re-using detectors that already exist in the repo (Rule 2).
 */
data class CellEvidence(
    val rawResponses: Int,
    val excludedContaminated: Int,   // from anticipation filter / AGRS>0.75 / SSRT !trustworthy
) {
    val nEffective: Int get() = (rawResponses - excludedContaminated).coerceAtLeast(0)
}
```

### A.3 The pipeline — what runs, and the gated outcome

```kotlin
/**
 * Pure, deterministic, KMP-common. No device, no IO — testable with synthetic response matrices,
 * exactly like SsrtCalculator / MetricsEngine. Returns a SEALED result; null/calibrating/blocked are
 * first-class, "low performance" is NOT a possible output (Rule 7).
 */
sealed interface ItemBankValidationResult {
    /** Not enough effective responses in this cell -> show "calibrando", do not score. */
    data class Calibrating(val cell: ConstructTag, val nEffective: Int, val nMin: Int) : ItemBankValidationResult

    /** Bank usable: lists which items are score-eligible and the uncertainty label to render. */
    data class Validated(
        val eligibleItemIds: Set<String>,
        val label: IncertaintyLabel,
    ) : ItemBankValidationResult

    /** A real detector fired -> downstream score MUST surface BANK_CONTAMINATED, value=null. */
    data class Contaminated(
        val offendingItemIds: Set<String>,
        val flags: Set<ContaminationFlag>,
    ) : ItemBankValidationResult
}

enum class IncertaintyLabel {
    INSUFFICIENT_DATA,        // "datos insuficientes"
    PRELIMINARY_ESTIMATE,     // "estimación preliminar"   (wide band)
    CALIBRATING,              // "calibrando"
    NOT_UPDATED_QUALITY,      // "no actualizado por calidad de medición"
    PROXY_NOT_DIAGNOSTIC,     // "proxy interno, no diagnóstico"
}

class ItemBankValidator(private val config: ItemValidationConfig = ItemValidationConfig()) {

    fun validate(
        items: List<ItemPsychometrics>,
        evidenceByCell: Map<ConstructTag, CellEvidence>,
        exposureLog: Map<String, Long>,           // itemId -> serves (real exposure, not a placeholder)
    ): ItemBankValidationResult {
        // 1. INTER-RATER GATE (Rule 5): any item below 0.80 -> flag, not silently dropped.
        val raterFailures = items.filter {
            it.interRaterAgreement != null && it.interRaterAgreement < config.interRaterFloor
        }

        // 2. HIGH_PRIOR DETECTION (Rule 5): empirical p-value too high at first exposure.
        val highPrior = items.filter {
            it.priorKnowledgeRisk == PriorRisk.HIGH_PRIOR ||
                (it.difficulty != null && it.difficulty >= config.highPriorPValue && it.exposureCount <= config.nMinPerItem)
        }

        // 3. ROTATION / OVER-EXPOSURE (Rule 5): exposure past ceiling => memorization risk.
        val overExposed = items.filter { (exposureLog[it.itemId] ?: it.exposureCount) > config.exposureCeiling }

        // 4. LOW-DISCRIMINATION pruning (Rule 9/10): items that measure nothing get retired, not kept.
        val deadWeight = items.filter {
            it.discriminability != null && it.discriminability < config.discriminationFloor
        }

        // 5. LEAKAGE: accuracy spike correlated with exposure inside the window (detector-fed).
        val leaking = items.filter { LEAKAGE_SUSPECTED in it.contaminationFlags }

        val contaminated = (raterFailures + highPrior + overExposed + deadWeight + leaking).toSet()
        if (contaminated.isNotEmpty()) {
            return ItemBankValidationResult.Contaminated(
                offendingItemIds = contaminated.map { it.itemId }.toSet(),
                flags = collectFlags(raterFailures, highPrior, overExposed, deadWeight, leaking),
            )
        }

        // 6. n-RULE (Rule 3): every scored cell must clear n_min on EFFECTIVE responses.
        val starvedCell = evidenceByCell.entries.firstOrNull { it.value.nEffective < config.nMinPerCell }
        if (starvedCell != null) {
            return ItemBankValidationResult.Calibrating(
                cell = starvedCell.key,
                nEffective = starvedCell.value.nEffective,
                nMin = config.nMinPerCell,
            )
        }

        // 7. HOLDOUT presence (Rule 5): each cell needs a live eval reserve (today emotions' isEval
        //    is filtered out and NEVER served -> would FAIL this gate, which is the point).
        val cellsMissingHoldout = items.groupBy { it.constructTag }
            .filterValues { group -> group.none { it.holdoutGroup == HoldoutGroup.HOLDOUT_EVAL } }
        if (cellsMissingHoldout.isNotEmpty()) {
            return ItemBankValidationResult.Calibrating(
                cell = cellsMissingHoldout.keys.first(),
                nEffective = 0, nMin = config.nMinPerCell,
            )
        }

        val eligible = items.filter { it.isScoreEligible }.map { it.itemId }.toSet()
        return ItemBankValidationResult.Validated(
            eligibleItemIds = eligible,
            label = labelFor(items),
        )
    }

    private fun labelFor(items: List<ItemPsychometrics>): IncertaintyLabel {
        val anyWideBand = items.any { it.discriminability != null && it.discriminability < 0.35 }
        return if (anyWideBand) IncertaintyLabel.PRELIMINARY_ESTIMATE else IncertaintyLabel.PROXY_NOT_DIAGNOSTIC
    }
    // collectFlags / LEAKAGE_SUSPECTED omitted for brevity
}
```

### A.4 How `BANK_CONTAMINATED` blocks the score (per-module wiring)

The repo has **no common `ScoreResult`** — each module has its own. So `BANK_CONTAMINATED` is woven into each module's existing state machine **as a reason, never as a value**. Logic example (mirrors `RciResult`/`CognitiveProfileScore.preliminary`):

```kotlin
// Logic module — extend the existing scoring result, do NOT invent a new score type.
sealed interface LogicScoreState {
    data object Calibrating : LogicScoreState                       // existing-style
    data class Value(val icr: Double, val confident: Boolean) : LogicScoreState

    /** NEW: validation said the bank is dirty. value is absent — never 0, never "low". (Rule 7) */
    data class Invalidated(val reason: InvalidationReason) : LogicScoreState
}

enum class InvalidationReason {
    BANK_CONTAMINATED,        // ItemBankValidationResult.Contaminated downstream of this session
    UNTRUSTED_TIMEBASE,       // Rule 1 — RT-critical w/o authoritative timebase (Focus/Speed)
    INSUFFICIENT_N,           // Rule 3 cell-starvation
}

// In the orchestrator (mirrors DecisionSessionOrchestrator's AGRS gate at ScoringEquations.kt:239):
fun finalizeLogicScore(
    sessionItems: List<ItemPsychometrics>,
    bankResult: ItemBankValidationResult,
    rawIcr: Double,
): LogicScoreState = when (bankResult) {
    is ItemBankValidationResult.Contaminated ->
        LogicScoreState.Invalidated(InvalidationReason.BANK_CONTAMINATED) // <- value suppressed
    is ItemBankValidationResult.Calibrating ->
        LogicScoreState.Calibrating
    is ItemBankValidationResult.Validated ->
        // only count items the validator green-lit; if the session used contaminated items, drop them
        if (sessionItems.all { it.itemId in bankResult.eligibleItemIds })
            LogicScoreState.Value(icr = rawIcr, confident = true)
        else
            LogicScoreState.Invalidated(InvalidationReason.BANK_CONTAMINATED)
}
```

UI side (Rule 6): `Invalidated(BANK_CONTAMINATED)` renders **"no actualizado por calidad de medición — proxy interno, no diagnóstico"**, never a number, never a low bar.

### A.5 Build-gate that enforces the migration (mirrors `arenaProductionGuard`)

A static guard, shaped exactly like `ArenaProductionGuardTask`, fails the build if a module serves items **without** going through validation — so the validation can't be silently bypassed:

```kotlin
// shared/build.gradle.kts — add next to arenaProductionGuard (build.gradle.kts:218)
abstract class ItemBankValidationGuardTask : org.gradle.api.DefaultTask() {
    @get:org.gradle.api.tasks.InputDirectory
    abstract val sourceDir: org.gradle.api.file.DirectoryProperty

    @org.gradle.api.tasks.TaskAction
    fun scan() {
        val dir = sourceDir.get().asFile
        if (!dir.exists()) return
        // Any content provider that returns sessionItems() MUST reference ItemBankValidator,
        // OR carry an explicit, reviewed opt-out marker. Procedural banks (Decisiones) are exempt.
        dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            val src = file.readText()
            val servesItems = Regex("""fun\s+sessionItems\s*\(""").containsMatchIn(src)
            val validates = src.contains("ItemBankValidator") ||
                src.contains("// itembank-exempt: procedural-no-authored-bank")
            if (servesItems && !validates) {
                throw GradleException(
                    "Item bank '${file.name}' serves items without ItemBankValidator and no exemption marker",
                )
            }
        }
    }
}
```

---

## Part B — `CopySafetyScanner`

### B.0 Two halves, both required

1. **Build task** — `copySafetyScanner` (+ CommonTest/DesktopTest variants), shaped **byte-for-byte** like `EmotionsTerminologyScannerTask` (accent-folding, whole-word, hyphen-crossing, `requireDir`, hooked to `check`). This is the one that **breaks the build**.
2. **In-code pure counterpart** — `CopyTerminologyScanner.kt`, the same rules as a testable function (mirrors `EmotionTerminologyScanner.kt` ↔ its build task). A **KMP test** runs it over `AppStrings` + screen string literals.

The crucial difference from the two existing scanners: this one is **NOT folder-scoped to one feature** — it covers **all of mindboost + `design/AppStrings.kt`** (the documented gap). Forbidden lexemes live **outside the scanned tree** (in `build.gradle.kts`) so the gate never self-trips — same trick the emotions gate uses.

### B.1 Forbidden lexemes & patterns (far-transfer ban, Rule 4)

```kotlin
// shared/build.gradle.kts — lives OUTSIDE features/** so it never self-matches.
// Folding = lowercase + Spanish-accent strip (reuse the emotions fold()). Matching = whole-word,
// hyphen-crossing (\b treats '-' as a boundary, like both existing scanners).
//
// THE BAN IS BIDIRECTIONAL (Rule 4): forbidden even when NEGATING the claim ("no mejora la
// inteligencia" still contains "inteligencia" -> blocked). Honest copy must use the ALLOWED phrasings
// below, which never reference the far-transfer construct at all.
private val forbiddenCopyLexemes = listOf(
    // far-transfer constructs -- banned affirmative OR negated
    "inteligencia", "ci", "iq", "coeficiente intelectual",
    "fake news", "desinformacion", "bulos",
    "multitarea", "multitasking",
    "productividad", "rendimiento academico", "rendimiento laboral", "rendimiento real",
    "conduce mejor", "conduccion", "al volante",
    "decisiones reales", "decisiones de la vida", "vida real", "vida diaria", "vida cotidiana",
    "regula tus emociones", "regular emociones", "regulacion emocional", "controla la ansiedad",
    "controlar la ansiedad", "autocontrol",
    "atencion en la vida", "concentracion en el trabajo", "foco en el estudio",
    // clinical / medical -- banned outright
    "diagnostico", "diagnosticar", "tratamiento", "tratar", "clinico", "clinica",
    "terapia", "terapeutico", "cura", "salud mental",
    "personalidad",
)

// Regex patterns (compiled folded) for multi-word transfer CLAIMS that single lexemes miss.
private val forbiddenCopyPatterns = listOf(
    """mejora(?:r|s)?\s+(?:tu\s+)?(?:decisiones|memoria|atencion|enfoque)\s+(?:en|de)\s+(?:la\s+)?vida""",
    """entren(?:a|ar)\s+(?:tu\s+)?cerebro\s+para\s+(?:la\s+)?vida""",
    """(?:te\s+)?vuelve(?:s)?\s+mas\s+inteligente""",
    """reduce(?:s)?\s+(?:tu\s+)?ansiedad""",
)
```

### B.2 Allowed copy (Rule 6) — what honest strings look like

```text
ALLOWED (never reference the construct, scoped to the task):
  "señal de práctica"                     "estimación preliminar"
  "datos insuficientes"                   "calibrando"
  "no actualizado por calidad de medición"
  "proxy interno, no diagnóstico"
  "entrenamiento funcional"               "progreso frente a tu propia línea base"
  "dificultad adaptativa"                 "perfil cognitivo funcional"
  "esto NO es un test de inteligencia"  -> BLOCKED (contains "inteligencia"). Use instead:
  "esto entrena una habilidad concreta; no mide rasgos generales."
```

> Subtle but load-bearing (Rule 4): the *negation disclaimers already in the repo* (`TransferReport.kt:54`, `FocusHomeScreen.kt:204`, `MindBoostHubScreen.kt:201`, `CreativityStrings.kt:12`) currently say things like *"NO demuestra mejora de inteligencia"* — those **contain "inteligencia"** and **this scanner will flag them**. That is intentional: the fix is to rewrite them to a construct-free form ("entrena una habilidad concreta; no generaliza"). The scanner forces the negation to stop naming the construct.

### B.3 Build task (breaks the build) — mirrors `EmotionsTerminologyScannerTask`

```kotlin
abstract class CopySafetyScannerTask : org.gradle.api.DefaultTask() {
    @get:org.gradle.api.tasks.Internal
    abstract val sourceDir: org.gradle.api.file.DirectoryProperty
    @get:org.gradle.api.tasks.Input
    abstract val requireDir: org.gradle.api.provider.Property<Boolean>

    @org.gradle.api.tasks.TaskAction
    fun scan() {
        val dir = sourceDir.get().asFile
        if (!dir.exists()) { if (requireDir.get()) throw GradleException("copy gate: required dir missing: ${dir.path}"); return }

        fun fold(raw: String): String = buildString(raw.length) {
            for (ch in raw.lowercase()) append(when (ch) {
                'á' -> 'a'; 'é' -> 'e'; 'í' -> 'i'; 'ó' -> 'o'; 'ú' -> 'u'; 'ü' -> 'u'; 'ñ' -> 'n'; else -> ch
            })
        }

        // Only scan STRING LITERALS, not identifiers/comments-with-construct-names-in-code-paths.
        // (A var named `intelligenceScore` in logic must not trip the gate; user-facing copy must.)
        val stringLiteral = Regex("\"([^\"\\\\]|\\\\.)*\"")

        dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            val literalsFolded = stringLiteral.findAll(file.readText()).joinToString("\n") { fold(it.value) }
            forbiddenCopyLexemes.forEach { term ->
                if (Regex("\\b" + Regex.escape(fold(term)) + "\\b").containsMatchIn(literalsFolded))
                    throw GradleException("Forbidden copy lexeme '$term' in user-facing string of ${file.name}")
            }
            forbiddenCopyPatterns.forEach { pat ->
                if (Regex(pat).containsMatchIn(literalsFolded))
                    throw GradleException("Forbidden far-transfer claim pattern in ${file.name}")
            }
        }
    }
}

// Cover ALL mindboost + global AppStrings (the documented gap). requireDir=true on the two that must exist.
tasks.register<CopySafetyScannerTask>("copySafetyScannerMindboost") {
    group = "verification"
    sourceDir.set(layout.projectDirectory.dir("src/commonMain/kotlin/com/mindos/app/features/mindboost"))
    requireDir.set(true)
}
tasks.register<CopySafetyScannerTask>("copySafetyScannerDesign") {
    group = "verification"
    sourceDir.set(layout.projectDirectory.dir("src/commonMain/kotlin/com/mindos/app/design")) // AppStrings.kt
    requireDir.set(true)
}
tasks.named("check") { dependsOn("copySafetyScannerMindboost", "copySafetyScannerDesign") }
```

### B.4 In-code counterpart + the KMP test that fails the build

```kotlin
// commonMain — pure, testable mirror of the build task (like EmotionTerminologyScanner.kt).
object CopyTerminologyScanner {
    fun fold(raw: String): String = /* identical fold() */ TODO()
    fun violations(text: String): List<String> {
        val folded = fold(text)
        return Forbidden.LEXEMES.filter { Regex("\\b" + Regex.escape(fold(it)) + "\\b").containsMatchIn(folded) } +
            Forbidden.PATTERNS.filter { Regex(it).containsMatchIn(folded) }.map { "pattern:$it" }
    }
    object Forbidden { val LEXEMES = listOf(/* same list */); val PATTERNS = listOf(/* same */) }
}
```

```kotlin
// commonTest — runs over the REAL AppStrings registry + a manifest of screen copy. Red on violation.
class CopySafetyTest {

    /** Every user-facing constant exposed by the design system, reflected as (name -> value). */
    private val appStrings: Map<String, String> = AppStrings.allUserFacingStrings() // add this accessor

    @Test
    fun appStrings_containNoFarTransferOrClinicalCopy() {
        val offenders = appStrings.mapNotNull { (name, value) ->
            val v = CopyTerminologyScanner.violations(value)
            if (v.isEmpty()) null else "$name -> $v"
        }
        assertTrue(offenders.isEmpty()) {
            "Far-transfer / clinical copy found (Rule 4). Rewrite construct-free (Rule 6):\n" +
                offenders.joinToString("\n")
        }
    }

    /** Guards the bidirectional ban: a NEGATED claim still names the construct -> must fail. */
    @Test
    fun negatedFarTransferClaim_isStillRejected() {
        assertTrue(CopyTerminologyScanner.violations("Esto NO mejora tu inteligencia").isNotEmpty())
        assertTrue(CopyTerminologyScanner.violations("No regula tus emociones en la vida real").isNotEmpty())
    }

    /** Positive control: the approved honest phrasings must PASS. */
    @Test
    fun approvedHonestCopy_passes() {
        listOf(
            "Entrenamiento funcional", "estimación preliminar", "calibrando",
            "proxy interno, no diagnóstico", "señal de práctica",
            "progreso frente a tu propia línea base",
        ).forEach { assertTrue(CopyTerminologyScanner.violations(it).isEmpty()) { "false positive on '$it'" } }
    }
}
```

---

## Cross-cutting compliance with the non-negotiable rules

| Rule | Where it's satisfied above |
|---|---|
| **1 — RT w/o authoritative timebase** | `InvalidationReason.UNTRUSTED_TIMEBASE` sits alongside `BANK_CONTAMINATED`; a Focus/Speed score on the desktop/iOS fallback (`SpeedTimebaseFallback`, `MonotonicClock`) must resolve to `Invalidated(UNTRUSTED_TIMEBASE)` — **jugable, profile-update no**. The `device_unreliable` gate model exists (`TimebasePolicy.deviceTimingRisk`) but `criticalFrameJitter`/`frameDropFraction` are hardcoded `false`/`0.0` → until real jank is measured, RT-critical scoring stays gated. |
| **2 — anti-gaming must be implemented** | `contaminationFlags` are populated **only** by detectors that already run (anticipation filter, AGRS>0.75, SSRT `trustworthy`); `n_effective` subtracts those flagged trials. No "designed" detector contributes. |
| **3 — n insufficient** | `ItemValidationConfig` (`nMinPerCell`, `nMinPerItem`), `CellEvidence.nEffective`, `Calibrating` outcome, `IncertaintyLabel`. |
| **4 — far-transfer copy** | `forbiddenCopyLexemes` + patterns, **bidirectional** (negation also blocked), `negatedFarTransferClaim_isStillRejected` test. |
| **5 — judgment-bank validation** | inter-rater ≥ 0.80 (`MIN_INTER_RATER`), holdout presence gate, rotation/over-exposure, `HIGH_PRIOR`, cultural/lexical load fields + DIF flag. |
| **6 — honest UI** | `IncertaintyLabel` enum maps exactly to the approved phrasings; `Invalidated` never renders a number. |
| **7 — null ≠ 0** | `difficulty`/`discriminability` nullable; `Invalidated`/`Calibrating` carry no value; `init` forbids `VALIDATED` without piloted data. |
| **8 — no abandonment penalty (emotional)** | validation pipeline never reads emotional-exercise abandonment as a contamination signal; abandonment stays a `SafetyEvent`, out of scope here. |
| **9 — redundancy** | `LOW_DISCRIMINATION` flag retires items that "measure nothing" → forces fuse/retire/differentiate. |
| **10 — better signals, not more** | `LOW_DISCRIMINATION` + exposure-rotation prune the bank toward *fewer, cleaner* items; the guard rejects adding items that bypass validation. |

---

### Files this design would touch (all absolute paths)

New:
- `C:\Users\curro\AndroidStudioProjects\MindOS\shared\src\commonMain\kotlin\com\mindos\app\features\mindboost\shared\itembank\validation\ItemPsychometrics.kt`
- `...\itembank\validation\ItemValidationConfig.kt`
- `...\itembank\validation\ItemBankValidator.kt`
- `C:\Users\curro\AndroidStudioProjects\MindOS\shared\src\commonMain\kotlin\com\mindos\app\features\mindboost\shared\copy\CopyTerminologyScanner.kt`
- `C:\Users\curro\AndroidStudioProjects\MindOS\shared\src\commonTest\kotlin\com\mindos\app\features\mindboost\shared\copy\CopySafetyTest.kt`

Modified (anchored to real files):
- `C:\Users\curro\AndroidStudioProjects\MindOS\shared\build.gradle.kts` — register `copySafetyScanner*` + `itemBankValidationGuard` next to the existing scanners (lines 198–355), add to `check`.
- `C:\Users\curro\AndroidStudioProjects\MindOS\shared\src\commonMain\kotlin\com\mindos\app\design\AppStrings.kt` — add `allUserFacingStrings()` accessor for the test; rewrite the four existing negation disclaimers (`TransferReport.kt:54`, `FocusHomeScreen.kt:204`, `MindBoostHubScreen.kt:201`, `CreativityStrings.kt:12`) construct-free.
- Each module's score finalizer (e.g. `LogicStateHolder.kt`, `DecisionSessionOrchestrator`) — add `BANK_CONTAMINATED` invalidation branch, mirroring the existing AGRS gate at `ScoringEquations.kt:239`.

**Key honesty consequence to flag:** turning on `copySafetyScanner` over real source will **immediately red the build** on today's repo, because the existing *negative* disclaimers contain banned construct names (`inteligencia`, `vida diaria`). That is the design working as intended (Rule 4 is bidirectional) — the migration step is rewriting those disclaimers to construct-free phrasing, not weakening the lexeme list.

---
