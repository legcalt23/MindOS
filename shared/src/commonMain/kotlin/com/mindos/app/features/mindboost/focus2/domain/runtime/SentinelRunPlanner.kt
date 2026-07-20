package com.mindos.app.features.mindboost.focus2.domain.runtime

import com.mindos.app.features.mindboost.core.runtime.SeedSource
import com.mindos.app.features.mindboost.core.session.purpose.SessionMeasurementPurpose
import com.mindos.app.features.mindboost.focus2.domain.generator.SentinelPlan
import com.mindos.app.features.mindboost.focus2.domain.generator.SentinelPlanner
import com.mindos.app.features.mindboost.focus2.domain.generator.SentinelSpec
import com.mindos.app.features.mindboost.focus2.domain.scoring.DescriptiveScorer

/**
 * Focus V5 · S4 · tarea (2) — tutorial, familiarization and measurement runs for the vigilance tracer.
 *
 * This is the exercise-B "generador-wrapper", and it is a DELIBERATE MIRROR of `FocusRunPlanner`: same three
 * kinds, same two-barrier warm-up protection, same seal-then-generate order — reusing the engine's
 * `FocusSessionDescriptor.seal` and the neutral `SentinelPlanner` without touching either. That the vigilance
 * run prepares through the identical sealing act the flanker uses is half the proof S4 owes: the purpose seal,
 * the version lineage and the modality capture are engine machinery, not the flanker's.
 *
 * The warm-up guarantee is enforced twice, exactly as in the flanker, because the two barriers fail differently:
 *
 *  1. **By purpose.** `TUTORIAL` / `FAMILIARIZATION` are excluded by `ProfileMeasurementEligibility`'s
 *     allowlist, so a warm-up can never be evidence even if someone later adds a variant and forgets.
 *  2. **By construction.** A warm-up's signal cell is below `DescriptiveScorer.minNPerCell`, so even if the
 *     purpose gate were bypassed the scorer returns `Absent(INSUFFICIENT_N)` rather than a number. For vigilance
 *     this bites on the SIGNAL cell specifically: rare targets mean the signal count is the scarce one, so a
 *     short warm-up starves it first — which is the right failure, since a d' you cannot estimate is not a d'.
 */
object SentinelRunPlanner {

    fun prepare(
        kind: SentinelRunKind,
        sessionId: String,
        seedSource: SeedSource,
        accessibility: AccessibilityContext = AccessibilityContext.NONE,
        baseSpec: SentinelSpec = SentinelSpec(),
    ): SentinelRun {
        val spec = baseSpec.copy(signalTrials = kind.signalTrials, noiseTrials = kind.noiseTrials)
        val descriptor = FocusSessionDescriptor.seal(
            sessionId = sessionId,
            exerciseId = SentinelPlanner.EXERCISE_ID,
            purpose = kind.purpose,
            seedSource = seedSource,
            generatorVersion = SentinelPlanner.GENERATOR_VERSION,
            scorerVersion = DescriptiveScorer.SCORER_VERSION,
            constructModelVersion = CONSTRUCT_MODEL_VERSION,
            spec = spec,
            inputModality = accessibility.impliedModality,
            screenReaderActive = accessibility.screenReaderActive,
        )
        val seed = requireNotNull(descriptor.seed) { "the seal must carry an algorithm this build can mint" }
        return SentinelRun(descriptor, SentinelPlanner.plan(spec, seed))
    }

    /** The construct version for E01. Sealed so a later re-interpretation cannot rewrite old sessions' meaning. */
    const val CONSTRUCT_MODEL_VERSION: Int = 1
}

/** The kinds of vigilance run, each a purpose and a (signal, noise) quota. */
enum class SentinelRunKind(
    val purpose: SessionMeasurementPurpose,
    val signalTrials: Int,
    val noiseTrials: Int,
) {

    /** First contact: the instructions with a handful of targets to try them on. Far below the floor. */
    TUTORIAL(SessionMeasurementPurpose.TUTORIAL, TUTORIAL_SIGNAL, TUTORIAL_NOISE),

    /** A warm-up before the measured block. Still below the floor; a separate purpose so washout can tell it
     *  apart from first exposure. */
    FAMILIARIZATION(SessionMeasurementPurpose.FAMILIARIZATION, FAMILIARIZATION_SIGNAL, FAMILIARIZATION_NOISE),

    /** The real block: the ratified 24/96. The only kind whose purpose can ever reach the profile. */
    MEASUREMENT(
        SessionMeasurementPurpose.MEASUREMENT,
        SentinelSpec.DEFAULT_SIGNAL_TRIALS,
        SentinelSpec.DEFAULT_NOISE_TRIALS,
    ),
    ;

    /** True when either cell is, by construction, too small for the ratified d' floor. */
    val belowMeasurementFloor: Boolean
        get() = signalTrials < DescriptiveScorer.minNPerCell || noiseTrials < DescriptiveScorer.minNPerCell
}

/**
 * A prepared vigilance run: the sealed descriptor and the plan it will present.
 *
 * [trialFacts] is the narrow projection the engine consumes — condition and signal axis only — identical in
 * shape to the flanker's `FocusRun.trialFacts`. That both tracers hand the runtime the same `List<TrialFacts>`
 * and nothing exercise-specific is the mechanical reason the reducer never learned which mechanic it is running.
 */
data class SentinelRun(val descriptor: FocusSessionDescriptor, val plan: SentinelPlan) {
    val trialFacts: List<TrialFacts>
        get() = plan.trials.map { TrialFacts(it.conditionId, it.isSignal) }
}

private const val TUTORIAL_SIGNAL = 2
private const val TUTORIAL_NOISE = 8
private const val FAMILIARIZATION_SIGNAL = 5
private const val FAMILIARIZATION_NOISE = 20
