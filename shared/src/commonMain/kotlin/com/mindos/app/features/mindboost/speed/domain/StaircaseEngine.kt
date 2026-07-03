package com.mindos.app.features.mindboost.speed.domain

import com.mindos.app.features.mindboost.speed.domain.metrics.FatigueIndex
import com.mindos.app.features.mindboost.speed.domain.model.BlockMetrics

/**
 * Motor adaptativo del §4/§6. MVP-1 (Hito 1): adaptación **unidimensional** gobernada por
 * la precisión (`pc`) de `BlockMetrics`, bloque a bloque. Ajusta la Carga Cognitiva
 * Efectiva (CCE ∈ [0,1]) y, a partir de ella, el `deadline` de respuesta de E6.
 *
 * Límites estrictos de MVP-1 (el resto es MVP-2+):
 * - Solo `pc` decide (REGLA 1 SUBIR / REGLA 2 BAJAR / REGLA 3 MANTENER). Las puertas
 *   multi-señal (ImpIdx, CV) y la ESTABILIZACIÓN/COOLDOWN por fatiga (REGLA 4/5) NO se
 *   implementan aquí: pertenecen a MVP-2.
 * - **VCE no autoriza avance** (acceptance #6): el motor IGNORA `metrics.vce`; dos bloques
 *   con la misma `pc` y distinto `vce` producen la misma decisión.
 * - Sin score visible.
 */
interface StaircaseEngine {
    /** Actualiza y devuelve la nueva CCE [0,1] a partir de las métricas del bloque. */
    fun updateDifficulty(metrics: BlockMetrics): Double

    /** Deadline de respuesta (nanos) para la CCE dada. Mayor CCE ⇒ deadline más corto. */
    fun getNextDeadline(cce: Double): Long?

    /** CCE actual del motor. */
    fun currentCce(): Double

    /** Reinicia el motor a la CCE inicial. */
    fun reset()
}

/**
 * Parámetros versionados del staircase (§6). Espejo de [MetricThresholds] en intención.
 *
 * @property accuracyHigh A_high — por encima, SUBIR presión.
 * @property accuracyLow A_low — por debajo, BAJAR presión.
 * @property deltaUp incremento base de CCE al subir, proporcional al margen de precisión.
 * @property deltaDown decremento de CCE al bajar.
 * @property maxDeadlineNanos deadline en CCE=0 (mínima presión).
 * @property minDeadlineNanos deadline en CCE=1 (máxima presión).
 */
@Suppress("MagicNumber", "LongParameterList") // Los defaults SON la calibración inicial del §6.
data class StaircaseConfig(
    val accuracyHigh: Double = 0.85,
    val accuracyLow: Double = 0.70,
    val deltaUp: Double = 0.08,
    val deltaDown: Double = 0.10,
    val maxDeadlineNanos: Long = 1_200_000_000L, // 1200 ms
    val minDeadlineNanos: Long = 450_000_000L, // 450 ms
) {
    init {
        require(accuracyLow < accuracyHigh) { "accuracyLow debe ser < accuracyHigh." }
        require(minDeadlineNanos < maxDeadlineNanos) { "minDeadline debe ser < maxDeadline." }
    }

    /** Deadline (nanos) para una CCE: mayor CCE ⇒ deadline más corto (más presión). */
    fun deadlineNanos(cce: Double): Long {
        val clamped = cce.coerceIn(0.0, 1.0)
        val span = maxDeadlineNanos - minDeadlineNanos
        return maxDeadlineNanos - (clamped * span).toLong()
    }

    companion object {
        val DEFAULT: StaircaseConfig = StaircaseConfig()
    }
}

/**
 * Implementación pc-driven del staircase (MVP-1). Stateful: mantiene la CCE entre bloques.
 *
 * Reglas (§6, restringidas a `pc` en MVP-1):
 * - REGLA 1 (SUBIR): `pc >= A_high` ⇒ `CCE = min(1, CCE + deltaUp·(pc - A_high + 1))`.
 * - REGLA 2 (BAJAR): `pc < A_low`  ⇒ `CCE = max(0, CCE - deltaDown)`. Incluye el caso
 *   límite "0 trials correctos ⇒ pc=0.0 ⇒ BAJAR" (§7).
 * - REGLA 3 (MANTENER): `A_low <= pc < A_high` ⇒ CCE sin cambios.
 */
class DefaultStaircaseEngine(
    private val config: StaircaseConfig = StaircaseConfig.DEFAULT,
    private val initialCce: Double = 0.5,
) : StaircaseEngine {

    private var cce: Double = initialCce.coerceIn(0.0, 1.0)

    override fun updateDifficulty(metrics: BlockMetrics): Double {
        // Solo pc decide. metrics.vce se ignora deliberadamente (acceptance #6).
        val pc = metrics.pc
        cce = when {
            pc >= config.accuracyHigh -> (cce + config.deltaUp * (pc - config.accuracyHigh + 1.0)).coerceAtMost(1.0)
            pc < config.accuracyLow -> (cce - config.deltaDown).coerceAtLeast(0.0)
            else -> cce
        }
        return cce
    }

    override fun getNextDeadline(cce: Double): Long = config.deadlineNanos(cce)

    override fun currentCce(): Double = cce

    override fun reset() {
        cce = initialCce.coerceIn(0.0, 1.0)
    }
}

// ── MVP-2: adaptación multi-señal ───────────────────────────────────────────

/** Directiva de adaptación del staircase (§6, reglas R1–R5). */
enum class StaircaseDirective {
    /** R1 — SUBIR presión. */
    RAISE,

    /** R2 — BAJAR presión. */
    LOWER,

    /** R3 — MANTENER. */
    HOLD,

    /** R4 — ESTABILIZAR (alta precisión pero inestable): mantener sin subir. */
    STABILIZE,

    /** R5 — COOLDOWN por fatiga: reducir carga y terminar la corrida de forma controlada. */
    COOLDOWN,
}

/** Decisión del staircase multi-señal: nueva CCE + la directiva que la motivó. */
data class StaircaseDecision(val cce: Double, val directive: StaircaseDirective)

/**
 * Staircase con salida enriquecida (directiva), para que la corrida pueda honrar
 * ESTABILIZAR/COOLDOWN. Extiende [StaircaseEngine] (compatible con MVP-1).
 */
interface AdaptiveStaircaseEngine : StaircaseEngine {
    fun evaluate(metrics: BlockMetrics): StaircaseDecision
    fun lastDirective(): StaircaseDirective
}

/**
 * Parámetros del staircase multi-señal de MVP-2 (§6). Reusa [base] para precisión, deltas
 * y mapeo de deadline; añade las puertas de impulsividad, inestabilidad y fatiga.
 */
@Suppress("MagicNumber", "LongParameterList") // Los defaults SON la calibración del §6.
data class MultiSignalStaircaseConfig(
    val base: StaircaseConfig = StaircaseConfig.DEFAULT,
    val impIdxCap: Double = 0.35, // I_cap
    val cvCap: Double = 0.25, // V_cap
    val highAnticipationRate: Double = 0.15,
    val highOmissionRate: Double = 0.20,
    val fatIdxCap: Double = 2.0,
) {
    companion object {
        val DEFAULT: MultiSignalStaircaseConfig = MultiSignalStaircaseConfig()
    }
}

/**
 * Staircase multi-señal de MVP-2 (§6). Gobierna la CCE con PC + ImpIdx + CV + (anticipación,
 * omisión) y, a partir del 3er bloque, FatIdx corrido ([FatigueIndex] sobre la serie
 * acumulada — resuelve la inconsistencia §6/§7, ver [FatigueIndex]).
 *
 * Reglas (en orden de precedencia):
 * - R5 COOLDOWN: `fatIdx > fatIdxCap` ⇒ baja carga y la corrida termina en cooldown.
 * - R2 BAJAR: `pc < A_low` **o** `impIdx > I_cap` **o** anticipación/omisión elevadas.
 * - R4 ESTABILIZAR: `pc >= A_high` pero `cv > V_cap` ⇒ mantener (no subir).
 * - R1 SUBIR: `pc >= A_high` con todas las puertas en orden.
 * - R3 MANTENER: `A_low <= pc < A_high`.
 *
 * **VCE se ignora** (acceptance #6). Métricas nulas (impIdx/cv) NO disparan su puerta.
 */
class MultiSignalStaircaseEngine(
    private val config: MultiSignalStaircaseConfig = MultiSignalStaircaseConfig.DEFAULT,
    private val initialCce: Double = 0.5,
) : AdaptiveStaircaseEngine {

    private var cce: Double = initialCce.coerceIn(0.0, 1.0)
    private val history = mutableListOf<BlockMetrics>()
    private var directive: StaircaseDirective = StaircaseDirective.HOLD

    override fun evaluate(metrics: BlockMetrics): StaircaseDecision {
        history += metrics
        val fatIdx = FatigueIndex.compute(history)
        directive = decide(metrics, fatIdx)
        cce = apply(directive, metrics)
        return StaircaseDecision(cce, directive)
    }

    override fun updateDifficulty(metrics: BlockMetrics): Double = evaluate(metrics).cce

    override fun lastDirective(): StaircaseDirective = directive

    override fun getNextDeadline(cce: Double): Long = config.base.deadlineNanos(cce)

    override fun currentCce(): Double = cce

    override fun reset() {
        cce = initialCce.coerceIn(0.0, 1.0)
        history.clear()
        directive = StaircaseDirective.HOLD
    }

    @Suppress("CyclomaticComplexMethod") // Tabla de decisión del §6; más clara como un `when` plano.
    private fun decide(metrics: BlockMetrics, fatIdx: Double?): StaircaseDirective {
        val pc = metrics.pc
        val impIdx = metrics.impIdx
        val cv = metrics.cv
        return when {
            fatIdx != null && fatIdx > config.fatIdxCap -> StaircaseDirective.COOLDOWN
            pc < config.base.accuracyLow -> StaircaseDirective.LOWER
            impIdx != null && impIdx > config.impIdxCap -> StaircaseDirective.LOWER
            metrics.anticipationRate > config.highAnticipationRate -> StaircaseDirective.LOWER
            metrics.omissionRate > config.highOmissionRate -> StaircaseDirective.LOWER
            pc >= config.base.accuracyHigh && cv != null && cv > config.cvCap -> StaircaseDirective.STABILIZE
            pc >= config.base.accuracyHigh -> StaircaseDirective.RAISE
            else -> StaircaseDirective.HOLD
        }
    }

    private fun apply(directive: StaircaseDirective, metrics: BlockMetrics): Double = when (directive) {
        StaircaseDirective.RAISE ->
            (cce + config.base.deltaUp * (metrics.pc - config.base.accuracyHigh + 1.0)).coerceAtMost(1.0)
        StaircaseDirective.LOWER, StaircaseDirective.COOLDOWN ->
            (cce - config.base.deltaDown).coerceAtLeast(0.0)
        StaircaseDirective.HOLD, StaircaseDirective.STABILIZE -> cce
    }
}
