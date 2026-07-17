@file:Suppress("MagicNumber") // Named domain constants + [0,1] unit-interval bounds are invariants, not magic.

package com.mindos.app.features.mindboost.core.scoring.normalization

import kotlin.math.exp

/**
 * Fase A · Universal, PURE, deterministic normalization kernel for MindBoost scoring (reconciled master
 * plan §6). No platform, no UI, no I/O. Every function is bounded to `[0,1]` and documented with its range.
 *
 * HONESTY BOUNDARY: this kernel maps already-measured, already-sufficient inputs onto a comparable scale.
 * It is NOT a sufficiency/validity gate and it NEVER decides whether a session counts — that authority is
 * [com.mindos.app.features.mindboost.core.scoring.ScoreStateMachine], which emits `null`/a non-VALID state
 * for insufficient/untrustworthy data (`null != 0`). A `0.0` out of this kernel is a genuine "worst
 * performance on a computable metric", never "no data".
 */
object ScoreNormalization {

    /** Axonal-conduction / anticipation floor: RTs faster than this are not genuine processing. */
    const val RT_MIN_PHYSIOLOGICAL_MS: Double = 120.0

    /** Attentional-persistence ceiling for fast tasks: RTs slower than this are lapses, not processing. */
    const val RT_MAX_ATTENTIONAL_MS: Double = 2500.0

    /** A trivial-difficulty perfect run earns this fraction of full credit (difficulty envelope floor). */
    const val DIFFICULTY_FLOOR: Double = 0.5

    /** Span above [DIFFICULTY_FLOOR] the envelope climbs to at max difficulty (1.0 − floor). */
    const val DIFFICULTY_SPAN: Double = 0.5

    /**
     * Winsorize a reaction time to biological bounds so a single aberrant trial can't drag a
     * central-tendency metric. Returns a value in `[minPhysMs, maxPhysMs]`.
     *
     * WARNING (learning 2026-06-04): winsorization is for CENTRAL-TENDENCY inputs (median/CV/IES) ONLY.
     * Extreme-event counts that are the signal itself — lapses (slow RTs) and anticipations (ultra-fast
     * RTs) — must be counted on RAW RTs, never on winsorized ones (winsorizing erases exactly those events).
     */
    fun winsorizeRt(
        rtMs: Double,
        minPhysMs: Double = RT_MIN_PHYSIOLOGICAL_MS,
        maxPhysMs: Double = RT_MAX_ATTENTIONAL_MS,
    ): Double {
        require(rtMs.isFinite()) { "rtMs must be finite (was $rtMs)." }
        require(minPhysMs >= 0.0 && minPhysMs < maxPhysMs) {
            "winsor bounds require 0 <= min < max (min=$minPhysMs, max=$maxPhysMs)."
        }
        return rtMs.coerceIn(minPhysMs, maxPhysMs)
    }

    /**
     * Logistic (sigmoid) mapping of a continuous metric to `(0,1)`, coerced to `[0,1]` for safety.
     * `x0` = expected-performance midpoint (maps to 0.5); `k` = curve sensitivity AND direction — positive
     * `k` for "higher is better" metrics, NEGATIVE `k` for "lower is better" metrics (e.g. RT). Direction
     * lives in the sign of `k` by design; the caller owns it. Finite inputs guarantee a finite result in
     * IEEE-754 (`exp(+∞arg)=∞ ⇒ 0`, `exp(−∞arg)=0 ⇒ 1`; never NaN).
     */
    fun sigmoid(x: Double, x0: Double, k: Double): Double {
        require(x.isFinite() && x0.isFinite() && k.isFinite()) {
            "sigmoid inputs must be finite (x=$x, x0=$x0, k=$k)."
        }
        return (1.0 / (1.0 + exp(-k * (x - x0)))).coerceIn(0.0, 1.0)
    }

    /**
     * Weight a normalized `[0,1]` performance by the difficulty actually reached: a multiplicative envelope
     * `DIFFICULTY_FLOOR + DIFFICULTY_SPAN * ratio` with `ratio = difficultyActual/difficultyMax ∈ [0,1]`.
     * So a perfect run at trivial difficulty earns half credit, and at max difficulty earns full credit.
     * Monotonic non-decreasing in difficulty. Output in `[0,1]`.
     *
     * Validity boundary (reconciled plan §7-efficacy B): this is TASK PERFORMANCE under adaptive difficulty,
     * honest ONLY when difficulty is genuinely adaptive (not user-chosen) and the item bank rotates — it is
     * never an ability/transfer claim.
     */
    fun difficultyAdjust(normalized: Double, difficultyActual: Double, difficultyMax: Double): Double {
        require(normalized.isFinite() && difficultyActual.isFinite()) {
            "difficulty inputs must be finite (normalized=$normalized, actual=$difficultyActual)."
        }
        require(difficultyMax > 0.0) { "difficultyMax must be > 0 (was $difficultyMax)." }
        val ratio = (difficultyActual / difficultyMax).coerceIn(0.0, 1.0)
        val envelope = DIFFICULTY_FLOOR + DIFFICULTY_SPAN * ratio
        return (normalized * envelope).coerceIn(0.0, 1.0)
    }

    /**
     * Combine the five normalized components (Accuracy, Speed, Difficulty, Consistency, ErrorControl) into a
     * `[0,1]` block-session score, applying the Rule-No-Speed hard gate.
     *
     * Rule-No-Speed (reconciled plan §6/§7-efficacy A): if `components.accuracy < minAccuracyForSpeed`, the
     * SPEED component's weight is forced to 0 — fast responses can't inflate a low-accuracy (likely random)
     * session. The gate WITHHOLDS credit; it never accuses.
     *
     * The score is `Σ(effectiveWeight_i · component_i) / Σ(baseWeight_i)`. The suppressed speed weight stays
     * in the DENOMINATOR (it is NOT renormalized away), so zeroing speed DEPRESSES the score. This is the
     * deliberate anti-gaming choice: renormalizing over the remaining weights would reward a constant-rate
     * spammer whose low RT-variability yields an artificially high Consistency component.
     *
     * Since every component is in `[0,1]` and every effective weight ≤ its base weight, the numerator ≤ the
     * denominator ⇒ the score is provably in `[0,1]` for any non-negative weights.
     */
    fun blockSessionScore(
        components: BlockScoreComponents,
        weights: BlockScoreWeights,
        minAccuracyForSpeed: Double,
    ): BlockScoreOutcome {
        val suppressSpeed = components.accuracy < minAccuracyForSpeed
        val effectiveSpeedWeight = if (suppressSpeed) 0.0 else weights.speed
        val numerator =
            weights.accuracy * components.accuracy +
                effectiveSpeedWeight * components.speed +
                weights.difficulty * components.difficulty +
                weights.consistency * components.consistency +
                weights.errorControl * components.errorControl
        return BlockScoreOutcome(
            score = (numerator / weights.baseSum).coerceIn(0.0, 1.0),
            appliedWeights = weights.copy(speed = effectiveSpeedWeight),
            speedSuppressed = suppressSpeed,
        )
    }
}

/** The five normalized components of a block-session score, each in `[0,1]`. */
data class BlockScoreComponents(
    val accuracy: Double,
    val speed: Double,
    val difficulty: Double,
    val consistency: Double,
    val errorControl: Double,
) {
    init {
        val values = listOf(accuracy, speed, difficulty, consistency, errorControl)
        require(values.all { it in 0.0..1.0 }) { "components must each be in [0,1] (was $values)." }
    }
}

/**
 * Non-negative weights for the five components. [nonSpeedSum] must be > 0 so a block still carries signal
 * when Rule-No-Speed zeroes the speed weight (otherwise a speed-only block would fabricate a 0).
 */
data class BlockScoreWeights(
    val accuracy: Double,
    val speed: Double,
    val difficulty: Double,
    val consistency: Double,
    val errorControl: Double,
) {
    /** Sum of ALL base weights — the fixed denominator, so a suppressed speed weight penalizes. */
    val baseSum: Double get() = accuracy + speed + difficulty + consistency + errorControl

    /** Sum of every weight except speed — must be > 0 (invariant). */
    val nonSpeedSum: Double get() = accuracy + difficulty + consistency + errorControl

    init {
        val values = listOf(accuracy, speed, difficulty, consistency, errorControl)
        require(values.all { it.isFinite() && it >= 0.0 }) {
            "weights must each be finite and >= 0 (was $values)."
        }
        require(nonSpeedSum > 0.0) {
            "non-speed weights must sum > 0 so a suppressed-speed block still has signal."
        }
    }
}

/** Traceable result of [ScoreNormalization.blockSessionScore]: the score, the weights actually applied. */
data class BlockScoreOutcome(
    val score: Double,
    val appliedWeights: BlockScoreWeights,
    val speedSuppressed: Boolean,
)
