package com.mindos.app.features.mindboost.focus.domain.model

/** The logical key a user can press/tap in response to a stimulus. */
enum class ResponseKey {
    PRIMARY,
    SECONDARY,
    LEFT,
    RIGHT,
    NONE,
}

/**
 * Render-agnostic payload of a stimulus, sealed by paradigm family. Render is the
 * UI layer's concern; the domain only carries the logical content.
 */
sealed interface StimulusPayload {
    /**
     * E1 Vigía (PVT / Mackworth): a rare target the user must catch, or a lure
     * that must be ignored.
     *
     * @param contrast 0.0 (degraded/hard) .. 1.0 (vivid/easy) — perceptual load.
     * @param lure a near-target distractor; responding to it is a false alarm.
     */
    data class Vigilance(
        val isSignal: Boolean,
        val contrast: Double,
        val lure: Boolean,
    ) : StimulusPayload

    /**
     * E2 Filtro (Eriksen flanker): respond to the [centerDirection], ignore the
     * flankers. Flankers point the same way ([congruent]) or the opposite way.
     */
    data class Flanker(
        val centerDirection: ResponseKey,
        val congruent: Boolean,
        val flankerCount: Int,
    ) : StimulusPayload

    /** E3 Gatillo (go/no-go): respond to [go] targets, withhold on no-go. */
    data class GoNoGo(val go: Boolean) : StimulusPayload

    /**
     * E4 Freno (stop-signal): respond by default; if [isStop] the stop signal
     * appears after [stopSignalDelayMs] and the response must be aborted.
     */
    data class StopSignal(val isStop: Boolean, val stopSignalDelayMs: Long?) : StimulusPayload

    /**
     * E5 Conflicto (Stroop/Simon): respond to the relevant dimension
     * ([relevantResponse]); [congruent] is false when the conflicting dimension
     * points elsewhere.
     */
    data class Conflict(
        val relevantResponse: ResponseKey,
        val congruent: Boolean,
    ) : StimulusPayload

    /** E6 Cambio (task switching): apply the active [rule]; [isSwitch] flags a rule change. */
    data class TaskSwitch(
        val rule: TaskRule,
        val isSwitch: Boolean,
        val correctResponse: ResponseKey,
    ) : StimulusPayload

    /** E7 Rastreo (visual search): is the target present among [setSize] items? */
    data class Search(val targetPresent: Boolean, val setSize: Int) : StimulusPayload

    /** E8 Dúo (dual task): a trial belonging to the [dual] condition (vs single). */
    data class DualTask(val dual: Boolean, val go: Boolean) : StimulusPayload

    /** E9 Enjambre (MOT): track [targetCount] of [objectCount] moving objects. */
    data class MultiObjectTracking(
        val objectCount: Int,
        val targetCount: Int,
        val targetIndices: Set<Int>,
        val speed: Double,
    ) : StimulusPayload

    /** E10 Flujo (RSVP): a stream of [length] items with targets at [t1Index] (and optional [t2Index]). */
    data class RsvpStream(val length: Int, val t1Index: Int, val t2Index: Int?) : StimulusPayload

    /** E11 Ancla (digital distraction): main task with an intruder present or not. */
    data class AnchorTask(val mainResponse: ResponseKey, val intruderPresent: Boolean) : StimulusPayload

    /** E12 Re-enganche: a vigilance signal, optionally following a provoked error. */
    data class Reengagement(val isSignal: Boolean, val afterProvokedError: Boolean) : StimulusPayload

    /** E13 Presión: a choice under an optional deadline ([underPressure]). */
    data class Pressure(
        val correctResponse: ResponseKey,
        val underPressure: Boolean,
        val deadlineMs: Long,
    ) : StimulusPayload
}

/** Rules for the E6 task-switching paradigm. */
enum class TaskRule { RULE_A, RULE_B }

/** Per-trial congruency, when the paradigm has one (flanker/Stroop); null otherwise. */
fun Stimulus.congruentFlag(): Boolean? = when (val p = payload) {
    is StimulusPayload.Flanker -> p.congruent
    is StimulusPayload.Conflict -> p.congruent
    else -> null
}

/** Extracts the paradigm markers a stimulus carries, so the runtime can attach them to the trial. */
fun Stimulus.toMarkers(): TrialMarkers = when (val p = payload) {
    is StimulusPayload.Flanker -> TrialMarkers(congruent = p.congruent)
    is StimulusPayload.Conflict -> TrialMarkers(congruent = p.congruent)
    is StimulusPayload.TaskSwitch -> TrialMarkers(isSwitch = p.isSwitch)
    is StimulusPayload.Search -> TrialMarkers(setSize = p.setSize)
    is StimulusPayload.DualTask -> TrialMarkers(dualTask = p.dual)
    is StimulusPayload.AnchorTask -> TrialMarkers(duringIntrusion = p.intruderPresent)
    is StimulusPayload.Pressure -> TrialMarkers(underPressure = p.underPressure)
    is StimulusPayload.Vigilance,
    is StimulusPayload.GoNoGo,
    is StimulusPayload.StopSignal,
    is StimulusPayload.MultiObjectTracking,
    is StimulusPayload.RsvpStream,
    is StimulusPayload.Reengagement,
    -> TrialMarkers.NONE
}

/**
 * A single stimulus in a block's generated sequence. Timing fields ([onsetMonotonicNs])
 * are stamped by the runtime at presentation, not by the (deterministic) generator.
 */
data class Stimulus(
    val index: Int,
    val isTarget: Boolean,
    val requiresStop: Boolean = false,
    val correctResponseKey: ResponseKey?,
    val payload: StimulusPayload,
    /** Inter-stimulus interval before this stimulus is shown (ms). */
    val interStimulusDelayMs: Long,
    /** Response window length once shown (ms). */
    val responseWindowMs: Long,
    /** Monotonic onset timestamp, filled by the runtime when actually presented. */
    val onsetMonotonicNs: Long? = null,
)

/** A captured user response with a monotonic timestamp (for RT = response - onset). */
data class Response(
    val key: ResponseKey,
    val responseMonotonicNs: Long,
)
