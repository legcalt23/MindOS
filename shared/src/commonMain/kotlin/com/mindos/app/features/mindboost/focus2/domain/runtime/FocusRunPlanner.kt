package com.mindos.app.features.mindboost.focus2.domain.runtime

import com.mindos.app.features.mindboost.core.runtime.SeedSource
import com.mindos.app.features.mindboost.core.session.purpose.SessionMeasurementPurpose
import com.mindos.app.features.mindboost.focus2.domain.generator.VectorFilterPlan
import com.mindos.app.features.mindboost.focus2.domain.generator.VectorFilterPlanner
import com.mindos.app.features.mindboost.focus2.domain.generator.VectorFilterSpec
import com.mindos.app.features.mindboost.focus2.domain.scoring.DescriptiveScorer

/**
 * Focus V5 · S3 · tarea (5) — tutorial, familiarization and measurement runs.
 *
 * The plan's requirement is that a warm-up has "purpose propio, jamás evidencia". That is enforced twice here,
 * and the redundancy is deliberate because the two barriers fail in different ways:
 *
 *  1. **By purpose.** `SessionMeasurementPurpose.TUTORIAL` / `FAMILIARIZATION` are excluded by
 *     `ProfileMeasurementEligibility`, which is an allowlist — so this holds even if someone later adds a
 *     warm-up variant and forgets about it.
 *  2. **By construction.** A warm-up's cells are smaller than `MinSampleGate`'s d' floor, so even if the
 *     purpose gate were somehow bypassed, the scorer has nothing to compute a d' from and returns absence
 *     rather than a number. A warm-up cannot be mistaken for a measurement by arithmetic either.
 *
 * The second barrier is the one that survives a refactor of the first.
 *
 * What warm-ups keep is everything the participant experiences: the same mechanic, the same generator, full
 * accuracy feedback. They are shorter, not lesser — withholding practice would be the wrong way to protect the
 * measurement.
 */
private const val TUTORIAL_TRIALS_PER_CELL = 2
private const val FAMILIARIZATION_TRIALS_PER_CELL = 5

enum class FocusRunKind(val purpose: SessionMeasurementPurpose, val trialsPerCell: Int) {

    /** First contact: the instructions, with a handful of trials to try them on. */
    TUTORIAL(SessionMeasurementPurpose.TUTORIAL, TUTORIAL_TRIALS_PER_CELL),

    /**
     * A warm-up before the measured block.
     *
     * It is a separate purpose from [TUTORIAL] rather than being flattened into it because washout analysis has
     * to tell first exposure apart from a familiar person warming up, and a label that was never recorded
     * cannot be recovered afterwards.
     */
    FAMILIARIZATION(SessionMeasurementPurpose.FAMILIARIZATION, FAMILIARIZATION_TRIALS_PER_CELL),

    /** The real block. The only kind whose purpose can ever reach the profile. */
    MEASUREMENT(SessionMeasurementPurpose.MEASUREMENT, VectorFilterSpec.DEFAULT_TRIALS_PER_CELL),
    ;

    /** True when this kind's cells are, by construction, too small for the ratified d' floor. */
    val belowMeasurementFloor: Boolean get() = trialsPerCell < DescriptiveScorer.minNPerCell
}

/** A prepared run: what will be shown, and the seal that says what it is for. */
data class FocusRun(val descriptor: FocusSessionDescriptor, val plan: VectorFilterPlan) {

    /**
     * The classification facts of each planned trial, in order.
     *
     * This is the projection the engine consumes, and it is deliberately narrower than the plan: the runtime
     * needs to know which condition a trial belonged to and which side of the signal axis it sat on, and
     * nothing else. Handing it the whole `PlannedTrial` would let the engine grow a dependency on the flanker
     * generator that S4's vigilance tracer could not satisfy.
     */
    val trialFacts: List<TrialFacts>
        get() = plan.trials.map { TrialFacts(it.conditionId, it.isSignal) }
}

/**
 * Prepares a run. The seal happens FIRST and the plan is generated FROM the sealed seed, never the other way
 * round — generating first and labelling afterwards is exactly how a run's purpose ends up chosen with
 * knowledge of what it produced.
 */
object FocusRunPlanner {

    fun prepare(
        kind: FocusRunKind,
        sessionId: String,
        seedSource: SeedSource,
        accessibility: AccessibilityContext = AccessibilityContext.NONE,
        baseSpec: VectorFilterSpec = VectorFilterSpec(),
    ): FocusRun {
        val spec = baseSpec.copy(trialsPerCell = kind.trialsPerCell)
        val descriptor = FocusSessionDescriptor.seal(
            sessionId = sessionId,
            exerciseId = VectorFilterPlanner.EXERCISE_ID,
            purpose = kind.purpose,
            seedSource = seedSource,
            generatorVersion = VectorFilterPlanner.GENERATOR_VERSION,
            scorerVersion = DescriptiveScorer.SCORER_VERSION,
            constructModelVersion = CONSTRUCT_MODEL_VERSION,
            spec = spec,
            inputModality = accessibility.impliedModality,
            screenReaderActive = accessibility.screenReaderActive,
        )
        val seed = requireNotNull(descriptor.seed) { "the seal must carry an algorithm this build can mint" }
        return FocusRun(descriptor, VectorFilterPlanner.plan(spec, seed))
    }

    /**
     * The version of the CONSTRUCT the exercise claims to engage — the 5 sub-skills as labels, per S2.
     *
     * It is sealed with the session so that a later change to what the exercise is understood to measure cannot
     * silently re-interpret sessions recorded under the old understanding.
     */
    const val CONSTRUCT_MODEL_VERSION: Int = 1
}
