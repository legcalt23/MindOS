package com.mindos.app.features.mindboost.focus.domain.exercise

import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import kotlin.random.Random

/** E12 (re-engagement) parameters from the relevant axes. */
data class E12Params(val signalRate: Double, val provokedErrorRate: Double, val responseWindowMs: Long)

/**
 * Maps difficulty to E12. Reads TEMPORAL_UNCERTAINTY (ISI jitter), PRESSURE (window)
 * and INHIBITORY_DEMAND (density of provoked errors to recover from).
 */
@Suppress("MagicNumber")
object E12DifficultyMapping {
    fun map(d: DifficultyVector): E12Params = E12Params(
        signalRate = 0.35,
        provokedErrorRate = lerp(0.10, 0.30, d.inhibitoryDemand.value),
        responseWindowMs = lerp(1300.0, 900.0, d.pressure.value).toLong(),
    )
}

/**
 * E12 Re-enganche generator. A vigilance-like task that deliberately provokes
 * errors so post-error recovery (accuracy after an error) can be measured. The
 * [StimulusPayload.Reengagement.afterProvokedError] flag marks recovery trials.
 */
class E12ReengancheGenerator : TrialGenerator {
    override val exerciseId: ExerciseId = ExerciseId.E12_REENGANCHE

    override fun generate(difficulty: DifficultyVector, spec: BlockSpec, seed: Long): List<Stimulus> {
        val params = E12DifficultyMapping.map(difficulty)
        val random = Random(seed)
        var previousWasProvoked = false
        return (0 until spec.targetTrialCount).map { index ->
            val isSignal = random.nextDouble() < params.signalRate
            val afterError = previousWasProvoked
            previousWasProvoked = random.nextDouble() < params.provokedErrorRate
            Stimulus(
                index = index,
                isTarget = isSignal,
                correctResponseKey = if (isSignal) ResponseKey.PRIMARY else null,
                payload = StimulusPayload.Reengagement(isSignal = isSignal, afterProvokedError = afterError),
                interStimulusDelayMs = ISI_MS,
                responseWindowMs = params.responseWindowMs,
            )
        }
    }

    private companion object {
        const val ISI_MS = 1_200L
    }
}
