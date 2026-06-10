package com.mindos.app.features.mindboost.memory.domain.evaluation

/**
 * Interpretation of a pre/post comparison (doc §9.4 table). Distinguishes genuine
 * improvement from familiarity, the core honesty guarantee of the system.
 */
enum class ImprovementPattern {
    /** Trained AND untrained both improved — real capacity gain. */
    GENUINE_IMPROVEMENT,

    /** Trained improved but untrained did not — familiarity, not capacity. */
    PRACTICE_EFFECT,

    /** Neither improved. */
    STAGNATION,

    /** Untrained improved more than trained — rare strong transfer. */
    EXCEPTIONAL_TRANSFER,

    /** Immediate improved but delayed regressed — fragile, not consolidated. */
    POOR_CONSOLIDATION,
}

/**
 * Deltas (post − pre) feeding the interpretation. [untrainedDelta] is the change on
 * the reserved probe — the only delta that proves real change (doc §6/§13.4).
 */
data class ImprovementEvidence(
    val trainedDelta: Double,
    val untrainedDelta: Double,
    val immediateDelta: Double,
    val delayedDelta: Double,
    val effectSize: Double,
    val sustainedWeeks: Int,
)

/**
 * Classifies improvement and gates the "real improvement" claim (doc §9.4 + §16 Q12):
 * a genuine claim requires a meaningful gain ON THE UNTRAINED PROBE, an effect size
 * ≥ 0.30, and at least 2 sustained weeks.
 */
object RealImprovementCriterion {

    const val MIN_DELTA = 0.05
    const val MIN_EFFECT_SIZE = 0.30
    const val MIN_SUSTAINED_WEEKS = 2

    fun classify(e: ImprovementEvidence): ImprovementPattern = when {
        e.delayedDelta < -MIN_DELTA && e.immediateDelta > MIN_DELTA ->
            ImprovementPattern.POOR_CONSOLIDATION

        e.untrainedDelta > e.trainedDelta + MIN_DELTA && e.untrainedDelta > MIN_DELTA ->
            ImprovementPattern.EXCEPTIONAL_TRANSFER

        e.trainedDelta > MIN_DELTA && e.untrainedDelta > MIN_DELTA ->
            ImprovementPattern.GENUINE_IMPROVEMENT

        e.trainedDelta > MIN_DELTA && e.untrainedDelta <= MIN_DELTA ->
            ImprovementPattern.PRACTICE_EFFECT

        else -> ImprovementPattern.STAGNATION
    }

    /** True only when improvement is real by the doc §16 Q12 bar (untrained + effect + sustained). */
    fun isGenuine(e: ImprovementEvidence): Boolean =
        e.untrainedDelta > MIN_DELTA &&
            e.effectSize >= MIN_EFFECT_SIZE &&
            e.sustainedWeeks >= MIN_SUSTAINED_WEEKS
}
