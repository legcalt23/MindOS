@file:Suppress("MagicNumber") // 8 is the named min-trials gate; 0.0/2.0 are domain neutrals/divisors.

package com.mindos.app.features.mindboost.decisiones.domain.scoring

/**
 * E2 Reversibilidad (Decisiones · P1) · One reversibility-triage card: the scenario was [actuallyReversible]
 * (Type-2) or irreversible (Type-1), and the user classified it as [selectedReversible].
 *
 * The task is a 2-alternative classification, so RAW accuracy is gameable by base-rate guessing (always
 * answer the more frequent class). The honest signal is therefore BALANCED accuracy (the mean of per-class
 * accuracy), which collapses any base-rate strategy toward chance. RT/lock timing is never part of the
 * profile signal (it is a secondary quality signal owned elsewhere).
 */
data class ReversibilityTrial(
    val actuallyReversible: Boolean,
    val selectedReversible: Boolean,
) {
    val correct: Boolean get() = actuallyReversible == selectedReversible
}

/** Block-level honesty state for a reversibility-triage block. */
enum class ReversibilityBlockState { VALID, INVALIDATED, CALIBRATING }

/**
 * Scored reversibility-triage block. [discriminationIndex] is the balanced accuracy over the block, null
 * when not computable — NEVER 0 for "no data". [validTrials] counts the cards that fed a VALID block.
 */
data class ReversibilityBlockResult(
    val state: ReversibilityBlockState,
    val discriminationIndex: Double?,
    val validTrials: Int,
)

/**
 * E2 Reversibilidad scorer (decision-making) · Turns triage cards into an honest DISCRIMINATION signal.
 *
 * Honesty / anti-gaming, tested against synthetic adversaries:
 *  - BASE-RATE GUESSING is defeated by construction: the index is BALANCED accuracy = (sensitivity over
 *    reversible + specificity over irreversible) / 2, so answering the majority class converges to chance.
 *  - A fully-CONSTANT responder (never used one of the two responses) on a set that contained BOTH stimulus
 *    classes failed to discriminate at all → INVALIDATED (a tested gaming pattern, not a low score —
 *    mirrors the EX4/EX3 over-selection gate).
 *  - A block that did not present BOTH stimulus classes cannot measure discrimination → CALIBRATING
 *    (insufficient class coverage; not the user's fault, so not INVALIDATED).
 * Thresholds are adapter-owned honest defaults (the module exposes no calibrated population norm).
 */
object ReversibilityScorer {
    const val MIN_VALID_TRIALS = 8

    fun scoreBlock(trials: List<ReversibilityTrial>): ReversibilityBlockResult {
        val reversible = trials.filter { it.actuallyReversible }
        val irreversible = trials.filterNot { it.actuallyReversible }
        // Empty collapses here too: no class present ⇒ class-coverage branch ⇒ calibrating(null, 0).
        val bothClassesPresent = reversible.isNotEmpty() && irreversible.isNotEmpty()
        val usedBothResponses = trials.any { it.selectedReversible } && trials.any { !it.selectedReversible }
        return when {
            !bothClassesPresent -> calibrating(null, trials.size) // insufficient class coverage
            !usedBothResponses -> invalidated() // constant responder on a discriminating set
            trials.size < MIN_VALID_TRIALS -> calibrating(null, trials.size)
            else -> validBlock(reversible, irreversible, trials.size)
        }
    }

    private fun validBlock(
        reversible: List<ReversibilityTrial>,
        irreversible: List<ReversibilityTrial>,
        total: Int,
    ): ReversibilityBlockResult {
        val sensitivity = reversible.count { it.correct }.toDouble() / reversible.size
        val specificity = irreversible.count { it.correct }.toDouble() / irreversible.size
        return ReversibilityBlockResult(
            state = ReversibilityBlockState.VALID,
            discriminationIndex = (sensitivity + specificity) / 2.0,
            validTrials = total,
        )
    }

    /** Inter-block aggregation: valid-trials-weighted mean index over VALID blocks only. null if none. */
    fun sessionIndex(blocks: List<ReversibilityBlockResult>): Double? {
        val good = blocks.filter { it.state == ReversibilityBlockState.VALID && it.discriminationIndex != null }
        if (good.isEmpty()) return null
        val weight = good.sumOf { it.validTrials.toDouble() }
        return good.sumOf { checkNotNull(it.discriminationIndex) * it.validTrials } / weight
    }

    private fun invalidated() = ReversibilityBlockResult(ReversibilityBlockState.INVALIDATED, null, 0)

    private fun calibrating(index: Double?, validTrials: Int) =
        ReversibilityBlockResult(ReversibilityBlockState.CALIBRATING, index, validTrials)
}
