// SyntheticRunConfig es un contenedor de configuración con muchos params por diseño; el
// runner contiene las constantes del modelo conductual sintético.
@file:Suppress("LongParameterList", "MagicNumber")

package com.mindos.app.features.mindboost.speed.simulation

import com.mindos.app.features.mindboost.speed.data.persistence.MeasurementRun
import com.mindos.app.features.mindboost.speed.domain.FixedTrialEngine
import com.mindos.app.features.mindboost.speed.domain.MetricThresholds
import com.mindos.app.features.mindboost.speed.domain.SpeedExerciseId
import com.mindos.app.features.mindboost.speed.domain.SpeedVersions
import com.mindos.app.features.mindboost.speed.domain.metrics.MeasurementContext
import com.mindos.app.features.mindboost.speed.domain.metrics.MetricCalculator
import com.mindos.app.features.mindboost.speed.domain.model.BlockMetrics
import com.mindos.app.features.mindboost.speed.domain.model.BlockRecord
import com.mindos.app.features.mindboost.speed.domain.model.DeviceProfile
import com.mindos.app.features.mindboost.speed.domain.model.QualityDecisionLog
import com.mindos.app.features.mindboost.speed.domain.model.SessionRecord
import com.mindos.app.features.mindboost.speed.domain.model.TrialRecord
import com.mindos.app.features.mindboost.speed.domain.model.VersionSnapshot
import com.mindos.app.features.mindboost.speed.domain.quality.QualitySignals
import com.mindos.app.features.mindboost.speed.domain.quality.SessionQualityEvaluator
import kotlin.math.max

/** Configuración de una corrida sintética. Determinista por [seed]. */
data class SyntheticRunConfig(
    val userId: String,
    val sessionId: String,
    val deviceProfile: DeviceProfile,
    val context: MeasurementContext,
    val exerciseId: SpeedExerciseId,
    val blockCount: Int,
    val trialsPerBlock: Int,
    val nowEpochMs: Long,
    val seed: Long = 1L,
    val thresholds: MetricThresholds = MetricThresholds.DEFAULT,
    val fixedDifficulty: Double = 0.5,
    val deadlineNanos: Long? = null,
    val interTrialGapMs: Long = 700L,
    val frameDropFraction: Double = 0.0,
    val criticalFrameJitter: Boolean = false,
    /**
     * Deltas inter-frame que la corrida simulada declara haber MEDIDO. El default modela un dispositivo
     * medido y limpio (que es lo que [frameDropFraction] `= 0.0` afirma); poner `0` aquí simula el caso
     * «no se midió nada», que la política trata como un hecho distinto (`MB-FOCUS-0071`).
     */
    val sampledFrames: Int = DEFAULT_SIMULATED_FRAMES,
)

/** Frames observados por defecto en una simulación: una sesión real acumula miles. */
private const val DEFAULT_SIMULATED_FRAMES = 600

/**
 * Ejecuta un [SyntheticProfile] contra el pipeline real (TrialEngine → MetricCalculator →
 * Quality) y ensambla una [MeasurementRun] completa, lista para persistir (§12). Es el
 * runner que las pruebas del §12 usan para validar métricas + integridad de persistencia
 * SIN dispositivo (acceptance #23). PRNG sembrado ⇒ totalmente reproducible.
 */
class SyntheticUserRunner(
    private val metricCalculator: MetricCalculator = MetricCalculator(),
    private val qualityEvaluator: SessionQualityEvaluator = SessionQualityEvaluator(),
) {

    @Suppress("LongMethod") // Pipeline lineal de simulación; más claro como un solo flujo.
    fun run(profile: SyntheticProfile, config: SyntheticRunConfig): MeasurementRun {
        val rng = SyntheticRandom(config.seed)
        var clockNanos = START_CLOCK_NANOS

        val allTrials = mutableListOf<TrialRecord>()
        val blocks = mutableListOf<BlockRecord>()
        val blockMetricsList = mutableListOf<BlockMetrics>()

        for (blockIndex in 0 until config.blockCount) {
            val blockId = "${config.sessionId}:b$blockIndex"
            val engine = FixedTrialEngine(
                blockId = blockId,
                exerciseId = config.exerciseId,
                context = config.context,
                thresholds = config.thresholds,
            )
            val baseRtMs = profile.baseRtMs + fatigueRtGrowth(profile, blockIndex)
            val spreadMs = profile.rtSpreadMs + fatigueSpreadGrowth(profile, blockIndex)
            val blockStartNanos = clockNanos

            repeat(config.trialsPerBlock) {
                val isAnticipation = rng.nextDouble() < profile.anticipationRate
                val rtMs = if (isAnticipation) {
                    anticipationRtMs(rng)
                } else {
                    max(NON_ANTICIPATION_FLOOR_MS, baseRtMs + (rng.nextDouble() - 0.5) * 2.0 * spreadMs)
                }
                val rtNanos = (rtMs * NANOS_PER_MS).toLong()
                val stimulus = clockNanos
                val response = stimulus + rtNanos
                engine.startTrial(config.fixedDifficulty, config.deadlineNanos, stimulus)
                val correct = if (isAnticipation) {
                    rng.nextDouble() < ANTICIPATION_ACCURACY
                } else {
                    rng.nextDouble() < profile.accuracy
                }
                engine.onResponse(response, correct)
                clockNanos = response + config.interTrialGapMs * NANOS_PER_MS_LONG
            }

            val trials = engine.getTrialHistory()
            allTrials += trials
            metricCalculator.blockMetrics(blockId, trials, config.context)?.let { blockMetricsList += it }
            blocks += BlockRecord(
                id = blockId,
                sessionId = config.sessionId,
                blockIndex = blockIndex,
                exerciseId = config.exerciseId.code,
                startedAtEpochMs = config.nowEpochMs,
                durationMs = (clockNanos - blockStartNanos) / NANOS_PER_MS_LONG,
                fixedDifficulty = config.fixedDifficulty,
                deadlineNanos = config.deadlineNanos,
                timestampSource = config.context.timestampSource,
                inputTimestampPrecision = config.context.inputTimestampPrecision,
                timebase = config.context.timebase,
            )
        }

        val sessionMetrics = metricCalculator.sessionMetrics(
            sessionId = config.sessionId,
            allTrials = allTrials,
            orderedBlockMetrics = blockMetricsList,
            context = config.context,
        )

        val decision = qualityEvaluator.evaluate(
            QualitySignals(
                totalTrials = allTrials.size,
                cv = sessionMetrics.cv,
                anticipationRate = sessionMetrics.anticipationRate,
                omissionRate = sessionMetrics.omissionRate,
                inputPrecision = config.context.inputTimestampPrecision,
                sampledFrames = config.sampledFrames,
                frameDropFraction = config.frameDropFraction,
                criticalFrameJitter = config.criticalFrameJitter,
                source = config.context.timestampSource,
            ),
        )

        return MeasurementRun(
            session = SessionRecord(
                id = config.sessionId,
                userId = config.userId,
                timestampEpochMs = config.nowEpochMs,
                deviceProfileId = config.deviceProfile.id,
                durationMs = (clockNanos - START_CLOCK_NANOS) / NANOS_PER_MS_LONG,
            ),
            deviceProfile = config.deviceProfile,
            versionSnapshot = VersionSnapshot(
                sessionId = config.sessionId,
                metricsVersion = SpeedVersions.METRICS_VERSION,
                staircaseVersion = SpeedVersions.STAIRCASE_VERSION,
                difficultyModelVersion = SpeedVersions.DIFFICULTY_MODEL_VERSION,
                lapseMultiplier = config.thresholds.lapseMultiplier,
            ),
            blocks = blocks,
            trials = allTrials,
            blockMetrics = blockMetricsList,
            sessionMetrics = sessionMetrics,
            qualityLogs = listOf(
                QualityDecisionLog(
                    id = "${config.sessionId}:q0",
                    sessionId = config.sessionId,
                    deviceTimingRisk = decision.deviceTimingRisk,
                    sessionQuality = decision.sessionQuality,
                    reasons = decision.reasons,
                    createdAtEpochMs = config.nowEpochMs,
                ),
            ),
        )
    }

    private fun fatigueRtGrowth(profile: SyntheticProfile, blockIndex: Int): Double =
        if (blockIndex < profile.fatigueOnsetBlock) {
            0.0
        } else {
            (blockIndex - profile.fatigueOnsetBlock + 1) * profile.fatigueRtGrowthMsPerBlock
        }

    private fun fatigueSpreadGrowth(profile: SyntheticProfile, blockIndex: Int): Double =
        if (blockIndex < profile.fatigueOnsetBlock) {
            0.0
        } else {
            (blockIndex - profile.fatigueOnsetBlock + 1) * profile.fatigueSpreadGrowthMsPerBlock
        }

    /** TR de anticipación: entre 40 y 90 ms (siempre < umbral de anticipación de 100 ms). */
    private fun anticipationRtMs(rng: SyntheticRandom): Double =
        ANTICIPATION_MIN_MS + rng.nextDouble() * (ANTICIPATION_MAX_MS - ANTICIPATION_MIN_MS)

    private companion object {
        const val START_CLOCK_NANOS = 1_000_000_000L
        const val NANOS_PER_MS = 1_000_000.0
        const val NANOS_PER_MS_LONG = 1_000_000L

        // Suelo de TR de un trial NO-anticipado (> umbral de anticipación de 100 ms).
        const val NON_ANTICIPATION_FLOOR_MS = 110.0
        const val ANTICIPATION_MIN_MS = 40.0
        const val ANTICIPATION_MAX_MS = 90.0

        // Aciertos al azar en anticipaciones (respuesta automática ~adivinanza).
        const val ANTICIPATION_ACCURACY = 0.4
    }
}

/**
 * PRNG xorshift64 determinista (no usa `Math.random()`; reproducible por semilla, lo que
 * el §12 exige y la JVM de test prohíbe vía `Math.random()`/`Date.now()`).
 */
internal class SyntheticRandom(seed: Long) {
    private var state: Long = if (seed == 0L) DEFAULT_SEED else seed

    fun nextLong(): Long {
        var x = state
        x = x xor (x shl SHIFT_A)
        x = x xor (x ushr SHIFT_B)
        x = x xor (x shl SHIFT_C)
        state = x
        return x
    }

    /** Double uniforme en [0, 1). */
    fun nextDouble(): Double = (nextLong() ushr DOUBLE_SHIFT).toDouble() / DOUBLE_DIVISOR

    private companion object {
        const val DEFAULT_SEED = -0x61c8864680b583ebL // 0x9E3779B97F4A7C15
        const val SHIFT_A = 13
        const val SHIFT_B = 7
        const val SHIFT_C = 17
        const val DOUBLE_SHIFT = 11
        const val DOUBLE_DIVISOR = 9_007_199_254_740_992.0 // 2^53
    }
}
