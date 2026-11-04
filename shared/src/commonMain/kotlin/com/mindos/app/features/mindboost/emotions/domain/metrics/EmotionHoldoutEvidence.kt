package com.mindos.app.features.mindboost.emotions.domain.metrics

import com.mindos.app.features.mindboost.emotions.domain.exercise.EmotionHoldoutProbe
import com.mindos.app.features.mindboost.emotions.domain.model.TransferConfidence

/** One administered probe: how many held-out items were answered correctly, out of how many were shown. */
data class HoldoutProbeOutcome(val correct: Int, val total: Int)

/** One session that carried a probe, paired with how the user did on the TRAINED items of that same session. */
data class HoldoutSessionEvidence(val probe: HoldoutProbeOutcome, val trainedAccuracy: Double)

/** Why a holdout figure cannot be produced. Named, because "no number" must be explainable to its reader. */
enum class HoldoutGap {
    /** No probe has been administered yet. */
    NO_PROBE_SERVED,

    /** Probes exist, but pooled they hold fewer items than the estimate needs. */
    TOO_FEW_ITEMS,

    /** Fewer than two probed sessions, so there is no change to look at. */
    TOO_FEW_PROBES,

    /**
     * Each probe is too small for its own estimate, so two of them cannot be compared. This is the state the
     * authored bank produces today: the held-out partition can supply complete probes, but not probes large
     * enough for a per-probe number that would survive its own error bar.
     */
    PROBE_TOO_SMALL_TO_COMPARE,
}

/** Ability on items the user was never trained on, corrected for the chance of guessing. */
sealed interface HoldoutSeparation {
    data class Measured(val value: Double, val items: Int) : HoldoutSeparation
    data class Unavailable(val reason: HoldoutGap) : HoldoutSeparation
}

/** How much of the visible improvement is explained by the trained items alone. */
sealed interface PracticeEffectRisk {
    data class Measured(val value: Double) : PracticeEffectRisk
    data class Unavailable(val reason: HoldoutGap) : PracticeEffectRisk
}

/**
 * Ola 4 · P3 — the arithmetic that separates PRACTICE from PROGRESS, and the honest refusals around it.
 *
 * Getting better at the exact items you were drilled on is the default outcome of any training task, and it is
 * not what the module claims to deliver. The only evidence that survives that objection is performance on items
 * the training never touched, which is what the held-out probe supplies.
 *
 * Two different questions, deliberately kept apart because they need very different amounts of data:
 *  - [separation] — "how well does this person do on unseen items?" One pooled estimate, so probes accumulate
 *    into it. Answerable once enough held-out items have been answered in total.
 *  - [practiceEffectRisk] — "did the improvement carry to unseen items, or only to the drilled ones?" This is a
 *    CHANGE between two points in time, so it needs each point to be its own usable estimate. Four items per
 *    probe cannot do that: at chance 0.25 a 4-item probe is worth roughly a coin flip, and differencing two of
 *    them produces a number whose error bar is wider than any effect it could show. It therefore refuses, and
 *    [HoldoutGap.PROBE_TOO_SMALL_TO_COMPARE] names the bank as the reason.
 *
 * Nothing here can raise a claim: [maxTransferConfidence] returns a CEILING, never a promotion.
 */
object EmotionHoldoutEvidence {

    /** Pooled held-out items required before an unseen-item estimate is reported at all. */
    const val MIN_POOLED_ITEMS: Int = 8

    /** Items ONE probe must hold before it can serve as one endpoint of a change comparison. */
    const val MIN_ITEMS_PER_PROBE_FOR_CHANGE: Int = 8

    /** Probed sessions required before "did it carry over?" is even a question with two sides. */
    const val MIN_PROBES_FOR_CHANGE: Int = 2

    /** Guessing rate on the four fixed category targets. */
    const val CHANCE: Double = 0.25

    /**
     * Chance-corrected accuracy over ALL held-out items answered so far: `max(0, acc - chance) / (1 - chance)`.
     * Pooling is legitimate here because this is an ability estimate, not a change score.
     */
    fun separation(probes: List<HoldoutProbeOutcome>): HoldoutSeparation {
        val items = probes.sumOf { it.total }
        val correct = probes.sumOf { it.correct }
        return when {
            probes.isEmpty() || items == 0 -> HoldoutSeparation.Unavailable(HoldoutGap.NO_PROBE_SERVED)
            items < MIN_POOLED_ITEMS -> HoldoutSeparation.Unavailable(HoldoutGap.TOO_FEW_ITEMS)
            else -> {
                val accuracy = correct.toDouble() / items
                val corrected = ((accuracy - CHANCE) / (1.0 - CHANCE)).coerceIn(0.0, 1.0)
                HoldoutSeparation.Measured(corrected, items)
            }
        }
    }

    /**
     * Ola 3 · R3 · P3 — whether EM3 may serve its blocks under time pressure, decided from the pooled held-out
     * probes and nothing else.
     *
     * Trained-item accuracy cannot answer this: the training bank is drilled, so it rewards memory as readily as
     * separation, and a gate on it would open for the wrong reason. Held-out items are the only accuracy in this
     * module that practice cannot have manufactured.
     *
     * **Every probe is administered in calm, whatever mode the session's training blocks run in.** That is what
     * makes this rule readable ("accuracy in calm") after pressure has been unlocked once, and it keeps a user's
     * probe history one comparable series rather than two half-series in different conditions — which matters
     * more here than anywhere else, because [EmotionHoldoutProbe.MAX_PROBES] complete probes is all a user will
     * ever have.
     *
     * **Re-decided from the whole pooled history every session, and therefore reversible.** That is what licenses
     * a point estimate against [PRESSURE_UNLOCK_ACCURACY] instead of an interval: this is not a claim published
     * about the user, it is a routing decision that the next probe re-opens, and the session already carries
     * overload detection and intensity reduction for the case where it opened too early. The stated limit is
     * plain — early on the decision rests on 8 to 24 items, so it can turn over on a single answer.
     */
    fun pressureUnlocked(correct: Int, items: Int): Boolean =
        items >= MIN_POOLED_ITEMS && correct.toDouble() / items >= PRESSURE_UNLOCK_ACCURACY

    /** Held-out accuracy in calm at or above which EM3 may serve pressure blocks. */
    const val PRESSURE_UNLOCK_ACCURACY: Double = 0.80

    /**
     * Share of the trained-item gain that did NOT appear on unseen items. `1.0` means the drilled items improved
     * and the unseen ones did not move at all; `0.0` means the gain carried over completely, or that trained
     * performance did not rise in the first place (there is then no gain to attribute).
     *
     * Refuses whenever the probes cannot each stand as an endpoint — a number here would be indistinguishable
     * from noise while reading like a verdict.
     */
    fun practiceEffectRisk(sessions: List<HoldoutSessionEvidence>): PracticeEffectRisk {
        val usable = sessions.filter { it.probe.total > 0 }
        val comparable = usable.all { it.probe.total >= MIN_ITEMS_PER_PROBE_FOR_CHANGE }
        return when {
            usable.isEmpty() -> PracticeEffectRisk.Unavailable(HoldoutGap.NO_PROBE_SERVED)
            usable.size < MIN_PROBES_FOR_CHANGE -> PracticeEffectRisk.Unavailable(HoldoutGap.TOO_FEW_PROBES)
            !comparable -> PracticeEffectRisk.Unavailable(HoldoutGap.PROBE_TOO_SMALL_TO_COMPARE)
            else -> PracticeEffectRisk.Measured(riskOf(usable))
        }
    }

    private fun riskOf(sessions: List<HoldoutSessionEvidence>): Double {
        val first = sessions.first()
        val last = sessions.last()
        val trainedGain = last.trainedAccuracy - first.trainedAccuracy
        val holdoutGain = accuracyOf(last.probe) - accuracyOf(first.probe)
        return if (trainedGain <= 0.0) 0.0 else ((trainedGain - holdoutGain) / trainedGain).coerceIn(0.0, 1.0)
    }

    private fun accuracyOf(probe: HoldoutProbeOutcome): Double =
        if (probe.total == 0) 0.0 else probe.correct.toDouble() / probe.total

    /**
     * The strongest transfer wording the evidence permits — a ceiling applied on top of whatever a caller
     * believes, never an upgrade. [TransferConfidence.LIMITED] additionally requires the practice-effect check
     * to have actually run and come back low, which the authored bank cannot currently support (see
     * [EmotionHoldoutProbe.MAX_PROBES] and [HoldoutGap.PROBE_TOO_SMALL_TO_COMPARE]).
     */
    fun maxTransferConfidence(
        separation: HoldoutSeparation,
        practiceEffectRisk: PracticeEffectRisk,
    ): TransferConfidence {
        val carried = separation is HoldoutSeparation.Measured &&
            separation.value > 0.0 &&
            practiceEffectRisk is PracticeEffectRisk.Measured &&
            practiceEffectRisk.value <= MAX_RISK_FOR_LIMITED
        return if (carried) TransferConfidence.LIMITED else TransferConfidence.NOT_DEMONSTRATED
    }

    /** Above this share of unexplained gain, improvement is attributed to the drilled items, not to transfer. */
    private const val MAX_RISK_FOR_LIMITED: Double = 0.5
}
