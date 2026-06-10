package com.mindos.app.features.mindboost.focus.domain.exercise

import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import kotlin.random.Random

/** E10 (RSVP) parameters from the relevant axes. */
data class E10Params(val streamLength: Int, val minLag: Int, val maxLag: Int, val hasT2Rate: Double)

/**
 * Maps difficulty to E10 (RSVP). Reads TEMPORAL_UNCERTAINTY (shorter T1–T2 lag, the
 * attentional-blink regime) and PERCEPTUAL_LOAD (stream length). Higher temporal
 * uncertainty pushes the lag into the short range where the blink appears.
 */
@Suppress("MagicNumber")
object E10DifficultyMapping {
    fun map(d: DifficultyVector): E10Params {
        // High temporal uncertainty → short lag (1..3); low → long lag (5..8).
        val minLag = lerp(5.0, 1.0, d.temporalUncertainty.value).toInt().coerceAtLeast(1)
        val maxLag = (minLag + 2)
        return E10Params(
            streamLength = lerp(12.0, 24.0, d.perceptualLoad.value).toInt(),
            minLag = minLag,
            maxLag = maxLag,
            hasT2Rate = 0.7,
        )
    }
}

/**
 * E10 Flujo generator. Each trial is one rapid serial stream with a first target
 * (T1) and, usually, a second target (T2) at a short lag — the regime where the
 * attentional blink (reduced T2 detection right after T1) is measurable.
 */
class E10FlujoGenerator : TrialGenerator {
    override val exerciseId: ExerciseId = ExerciseId.E10_FLUJO

    override fun generate(difficulty: DifficultyVector, spec: BlockSpec, seed: Long): List<Stimulus> {
        val params = E10DifficultyMapping.map(difficulty)
        val random = Random(seed)
        return (0 until spec.targetTrialCount).map { index ->
            val lag = params.minLag + random.nextInt((params.maxLag - params.minLag) + 1)
            val t1 = random.nextInt((params.streamLength - lag - 1).coerceAtLeast(1))
            val hasT2 = random.nextDouble() < params.hasT2Rate
            val t2 = if (hasT2) (t1 + lag).coerceAtMost(params.streamLength - 1) else null
            Stimulus(
                index = index,
                isTarget = true,
                correctResponseKey = ResponseKey.PRIMARY,
                payload = StimulusPayload.RsvpStream(length = params.streamLength, t1Index = t1, t2Index = t2),
                interStimulusDelayMs = ISI_MS,
                responseWindowMs = RESPONSE_WINDOW_MS,
            )
        }
    }

    private companion object {
        const val ISI_MS = 800L
        const val RESPONSE_WINDOW_MS = 2_000L
    }
}
