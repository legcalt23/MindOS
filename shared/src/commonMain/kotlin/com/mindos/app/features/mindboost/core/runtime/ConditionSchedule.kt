package com.mindos.app.features.mindboost.core.runtime

import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * A fixed-count condition schedule: exactly `round(size · proportion)` of the [size] slots carry the condition,
 * shuffled with [random]. Returns one flag per slot, in presentation order.
 *
 * ## Why a quota and not a coin flip per trial
 *
 * A condition split has two properties that are independent, and drawing each trial with its own Bernoulli
 * couples them: it fixes the expected PROPORTION (exercise design, governed by a difficulty axis) but leaves the
 * realised CELL SIZE — the sample of the comparison — as a random variable. Since a difference score is judged on
 * its SMALLER cell against a ratified floor (`MinSampleGate`), that made the eligibility of a capacity claim
 * depend on the seed: measured on the real generators, the baseline flanker block yields anywhere from 4 to 20
 * incongruent trials against a floor of 20, so roughly one user in a hundred was granted a comparison the design
 * never granted. Worse, how often that happens moves with difficulty as the adaptive engine does its job — E8
 * falls from 88 % to 9 % eligible between minimum and maximum dual-task load.
 *
 * A quota keeps the expected proportion (it may differ by rounding only — asserted, and the abort criterion of
 * the ADR: moving a proportion is a product decision) and drives the cell-size variance to zero, so eligibility
 * becomes a deterministic function of `(size, difficulty)` that a build test can state. Shuffling is not optional:
 * a quota laid out in a block would trade a sampling defect for a worse one, making the condition predictable and
 * confounded with time-on-task.
 *
 * [atLeast] is for generators that guarantee the condition occurs (E3's no-go, E4's stop) — it raises the count,
 * never lowers it. See `docs/audits/ADR_CONDITION_SCHEDULE_2026-08-19.md`.
 *
 * Promoted from `focus.domain.exercise` to `core` in Ola 3 · R1: the fused Emociones inhibition exercise has the
 * same obligation — its stop/go split IS the sample of its SSRT estimate — and the rule that split obeys must be
 * one authority, not a second copy that can drift back to a coin flip.
 */
internal fun conditionSchedule(size: Int, proportion: Double, random: Random, atLeast: Int = 0): List<Boolean> {
    val flagged = conditionQuota(size, proportion, atLeast)
    return (List(flagged) { true } + List(size - flagged) { false }).shuffled(random)
}

/**
 * How many of the [size] slots carry the condition. Split out of [conditionSchedule] so that the authority which
 * DECLARES whether a block can sustain a measurement claim (`MeasurementReachability`) can ask the question
 * without drawing a schedule — and, more importantly, without a second implementation of `round(size · p)`.
 * Two copies of this arithmetic would be two sources of truth for the sample a claim rests on.
 *
 * That promise was only half kept before Ola 3 moved both here: `conditionSchedule` carried its own inline copy
 * of the same expression, so the split existed in the KDoc and not in the code. [conditionSchedule] now calls
 * this, which is what makes the two agree by construction instead of by review.
 */
internal fun conditionQuota(size: Int, proportion: Double, atLeast: Int = 0): Int {
    require(size >= 0) { "A schedule cannot have a negative number of slots (was $size)." }
    require(proportion in 0.0..1.0) { "A proportion must be a share of the block (was $proportion)." }
    return (size * proportion).roundToInt().coerceIn(atLeast.coerceAtMost(size), size)
}
