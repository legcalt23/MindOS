package com.mindos.app.features.mindboost.focus.presentation.exercise

import com.mindos.app.features.mindboost.focus.domain.exercise.BlockSpec
import com.mindos.app.features.mindboost.focus.domain.exercise.TrialClassifier
import com.mindos.app.features.mindboost.focus.domain.exercise.TrialGenerator
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.IndividualThresholds
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import com.mindos.app.features.mindboost.focus.domain.model.TrialOutcome
import com.mindos.app.features.mindboost.focus.domain.session.FocusSessionAssembler
import com.mindos.app.features.mindboost.focus.presentation.state.ChoiceParadigm
import com.mindos.app.features.mindboost.focus.presentation.state.FeedbackKind
import com.mindos.app.features.mindboost.focus.presentation.state.FocusFieldState
import kotlinx.coroutines.delay

/** A captured response: its monotonic timestamp (for RT = response − onset) and the key pressed. */
data class FocusResponseEvent(val monotonicNs: Long, val key: ResponseKey)

/**
 * Runs ONE fixed-difficulty block in real time for the **single-tap** (PVT / go-no-go) and
 * **two-choice** (flanker / Stroop-Simon) paradigms only — exactly the paradigms a baseline
 * (E1/E2/E3) and the reserved probes (PVT / FLANKER / GO_NO_GO) use. It never adapts difficulty
 * (the [Mode] must be CALIBRATION or EVALUATION), so it is the shared engine behind both the baseline
 * and the evaluation flows without touching the richer training [com.mindos.app.features.mindboost.focus.presentation.FocusSessionStateHolder]
 * (which also handles stop-signal, deadline and animated paradigms).
 *
 * Timing is honest: reaction time is `responseNanos − onsetNanos` from the injected [MonotonicClock],
 * never wall-clock (risk A1). The holder owns the response input surface and hands the runner an
 * [awaitResponse] suspending lambda; the runner reports each step through [onProgress] so each holder
 * maps it to its own UI state. Classification is delegated to the pure [TrialClassifier]; metrics and
 * adaptation are NOT this runner's concern.
 */
class FocusFixedBlockRunner(private val monotonicClock: MonotonicClock) {

    /** Live counters of the running block (single-threaded, mutated inside the coroutine). */
    data class Counters(
        val hits: Int = 0,
        val lapses: Int = 0,
        val falseAlarms: Int = 0,
        val lastReactionMs: Long? = null,
    )

    /** One emission: what the field shows now, how far the block has progressed, and the live counters. */
    data class Progress(
        val field: FocusFieldState,
        val trialsDone: Int,
        val trialsTotal: Int,
        val counters: Counters,
    )

    /** The finished block: the recorded trials (for the pure assembler) and the final counters. */
    data class Result(
        val recorded: List<FocusSessionAssembler.RecordedTrial>,
        val counters: Counters,
    )

    @Suppress("LongParameterList") // Intrinsic inputs of running one block; grouping them would add noise.
    suspend fun run(
        generator: TrialGenerator,
        difficulty: DifficultyVector,
        trialCount: Int,
        seed: Long,
        mode: Mode,
        thresholds: IndividualThresholds,
        awaitResponse: suspend (windowMs: Long) -> FocusResponseEvent?,
        onProgress: (Progress) -> Unit,
    ): Result {
        require(mode != Mode.TRAINING) {
            "FocusFixedBlockRunner runs fixed-difficulty blocks only; TRAINING adaptation is not allowed here."
        }
        val spec = BlockSpec(targetTrialCount = trialCount, mode = mode)
        val stimuli = generator.generate(difficulty, spec, seed)
        val recorded = ArrayList<FocusSessionAssembler.RecordedTrial>(stimuli.size)
        var counters = Counters()

        stimuli.forEachIndexed { index, stimulus ->
            onProgress(Progress(FocusFieldState.Idle, index, trialCount, counters))
            delay(stimulus.interStimulusDelayMs)

            val onsetNanos = monotonicClock.nowNanos()
            onProgress(Progress(fieldFor(stimulus), index, trialCount, counters))
            val event = awaitResponse(stimulus.responseWindowMs)
            val latencyMs = event?.let { ((it.monotonicNs - onsetNanos) / NANOS_PER_MS).coerceAtLeast(0) }
            recorded += FocusSessionAssembler.RecordedTrial(
                stimulus = stimulus,
                responseLatencyMs = latencyMs,
                responseKey = event?.key,
            )

            val classification = classify(stimulus, latencyMs, event?.key, thresholds)
            counters = counters.updated(classification.outcome, latencyMs, thresholds)
            val feedback = FocusFieldState.Feedback(feedbackFor(classification.outcome))
            onProgress(Progress(feedback, index + 1, trialCount, counters))
            delay(FEEDBACK_MS)
        }
        return Result(recorded = recorded, counters = counters)
    }

    private fun classify(
        stimulus: Stimulus,
        latencyMs: Long?,
        responseKey: ResponseKey?,
        thresholds: IndividualThresholds,
    ): TrialClassifier.Classification {
        val choiceKey = stimulus.correctResponseKey?.takeIf { it.isDirectional }
        return if (choiceKey != null) {
            TrialClassifier.classifyChoice(
                correctKey = choiceKey,
                responseKey = responseKey,
                responseLatencyMs = latencyMs,
                anticipationFloorMs = thresholds.anticipationFloorMs,
                responseWindowMs = stimulus.responseWindowMs,
            )
        } else {
            TrialClassifier.classify(
                isTarget = stimulus.isTarget,
                responseLatencyMs = latencyMs,
                anticipationFloorMs = thresholds.anticipationFloorMs,
                responseWindowMs = stimulus.responseWindowMs,
            )
        }
    }

    /** Maps a tap/choice stimulus to its field; fails fast on any unsupported (stop/animated) paradigm. */
    private fun fieldFor(stimulus: Stimulus): FocusFieldState = when (val payload = stimulus.payload) {
        is StimulusPayload.Vigilance -> {
            val contrast = payload.contrast.toFloat()
            if (stimulus.isTarget) FocusFieldState.Signal(contrast) else FocusFieldState.Lure(contrast)
        }
        is StimulusPayload.GoNoGo ->
            if (stimulus.isTarget) FocusFieldState.Signal(1f) else FocusFieldState.Lure(1f)
        is StimulusPayload.Flanker ->
            FocusFieldState.Choice(
                payload.centerDirection,
                payload.congruent,
                payload.flankerCount,
                ChoiceParadigm.FLANKER,
            )
        is StimulusPayload.Conflict ->
            FocusFieldState.Choice(payload.relevantResponse, payload.congruent, 0, ChoiceParadigm.CONFLICT)
        else -> error("FocusFixedBlockRunner supports tap/choice paradigms only; got ${payload::class.simpleName}.")
    }

    private fun Counters.updated(
        outcome: TrialOutcome,
        latencyMs: Long?,
        thresholds: IndividualThresholds,
    ): Counters = when (outcome) {
        TrialOutcome.HIT -> copy(
            hits = hits + 1,
            lastReactionMs = latencyMs,
            lapses = if (latencyMs != null && latencyMs > thresholds.lapseRtMs) lapses + 1 else lapses,
        )
        TrialOutcome.FALSE_ALARM -> copy(falseAlarms = falseAlarms + 1)
        else -> this
    }

    private fun feedbackFor(outcome: TrialOutcome): FeedbackKind = when (outcome) {
        TrialOutcome.HIT -> FeedbackKind.HIT
        TrialOutcome.MISS, TrialOutcome.TIMEOUT -> FeedbackKind.MISS
        TrialOutcome.FALSE_ALARM -> FeedbackKind.FALSE_ALARM
        TrialOutcome.CORRECT_REJECTION -> FeedbackKind.CORRECT_REJECTION
        TrialOutcome.ANTICIPATION -> FeedbackKind.TOO_EARLY
    }

    private companion object {
        const val NANOS_PER_MS = 1_000_000L
        const val FEEDBACK_MS = 320L
    }
}
