package com.mindos.app.features.mindboost.focus.domain.exercise

import com.mindos.app.features.mindboost.core.runtime.conditionSchedule
import com.mindos.app.features.mindboost.core.staircase.StopSignalTracker
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import kotlin.random.Random

/** Concrete E4 (stop-signal) parameters derived from the relevant difficulty axes. */
data class E4Params(
    val stopProportion: Double,
    val responseWindowMs: Long,
    val isiMs: Long,
    val initialSsdMs: Long,
    val ssdStepMs: Long,
)

/**
 * Maps difficulty to E4 (stop-signal). Reads ONLY E4's relevant axes:
 * INHIBITORY_DEMAND (proportion of stop trials) and PRESSURE (response window).
 */
@Suppress("MagicNumber") // The literals are this exercise's difficulty curve.
object E4DifficultyMapping {
    fun map(d: DifficultyVector): E4Params = E4Params(
        stopProportion = lerp(0.25, 0.33, d.inhibitoryDemand.value),
        responseWindowMs = lerp(1200.0, 800.0, d.pressure.value).toLong(),
        isiMs = lerp(900.0, 650.0, d.pressure.value).toLong(),
        initialSsdMs = 250,
        ssdStepMs = 50,
    )
}


/**
 * E4 Freno generator. Produces the go/stop SCHEDULE (which trials carry a stop
 * signal). The actual stop-signal delay is decided online by a [StopSignalTracker],
 * so [StimulusPayload.StopSignal.stopSignalDelayMs] is left null at generation.
 * Deterministic for a given seed; guarantees at least one stop trial.
 */
class E4FrenoGenerator : TrialGenerator {
    override val exerciseId: ExerciseId = ExerciseId.E4_FRENO

    /** A fresh tracker configured for [difficulty]; the runtime drives it during the block. */
    fun tracker(difficulty: DifficultyVector): StopSignalTracker {
        val params = E4DifficultyMapping.map(difficulty)
        return StopSignalTracker(initialSsdMs = params.initialSsdMs, stepMs = params.ssdStepMs)
    }

    override fun generate(difficulty: DifficultyVector, spec: BlockSpec, seed: Long): List<Stimulus> {
        val params = E4DifficultyMapping.map(difficulty)
        val random = Random(seed)
        val count = spec.targetTrialCount
        // The stop/go split is the sample of SSRT, so it is a quota. `atLeast = 1` carries the guarantee the
        // forced index used to provide; with any real proportion the quota already exceeds it.
        val stops = conditionSchedule(count, params.stopProportion, random, atLeast = 1)
        return (0 until count).map { index ->
            val isStop = stops[index]
            Stimulus(
                index = index,
                isTarget = true, // respond by default
                requiresStop = isStop,
                correctResponseKey = ResponseKey.PRIMARY,
                payload = StimulusPayload.StopSignal(isStop = isStop, stopSignalDelayMs = null),
                interStimulusDelayMs = params.isiMs,
                responseWindowMs = params.responseWindowMs,
            )
        }
    }
}
