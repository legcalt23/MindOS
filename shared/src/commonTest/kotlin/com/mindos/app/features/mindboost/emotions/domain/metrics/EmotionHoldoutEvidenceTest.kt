@file:Suppress("MagicNumber") // The thresholds and item counts ARE the contract under test.

package com.mindos.app.features.mindboost.emotions.domain.metrics

import com.mindos.app.features.mindboost.emotions.domain.exercise.EmotionHoldoutProbe
import com.mindos.app.features.mindboost.emotions.domain.model.TransferConfidence
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Ola 4 · P3 — the practice-vs-progress arithmetic, and (just as important) its refusals.
 *
 * The failure this suite is built to prevent is a plausible-looking transfer number produced from data that
 * cannot support one. Every "unavailable" case below is therefore asserted as strictly as the computed ones.
 */
class EmotionHoldoutEvidenceTest {

    private fun probes(vararg pairs: Pair<Int, Int>) = pairs.map { HoldoutProbeOutcome(it.first, it.second) }

    @Test
    fun no_probe_and_a_short_probe_history_both_refuse_instead_of_returning_zero() {
        assertEquals(
            HoldoutSeparation.Unavailable(HoldoutGap.NO_PROBE_SERVED),
            EmotionHoldoutEvidence.separation(emptyList()),
        )
        // 4 answered items is below the pooled floor: a value here would look like an ability estimate.
        assertEquals(
            HoldoutSeparation.Unavailable(HoldoutGap.TOO_FEW_ITEMS),
            EmotionHoldoutEvidence.separation(probes(4 to 4)),
        )
    }

    @Test
    fun pooled_probes_produce_a_chance_corrected_estimate() {
        // 12 items, 6 correct → accuracy 0.5 → (0.5 - 0.25) / 0.75 = 0.333…
        val measured = assertIs<HoldoutSeparation.Measured>(
            EmotionHoldoutEvidence.separation(probes(2 to 4, 2 to 4, 2 to 4)),
        )
        assertEquals(12, measured.items)
        assertTrue(measured.value in 0.33..0.34, "expected the chance-corrected value, was ${measured.value}")
    }

    @Test
    fun answering_at_chance_is_zero_separation_not_a_negative_or_a_credit() {
        // 3 of 12 correct is exactly guessing on four targets. Below chance must clamp to 0, never go negative.
        val atChance = assertIs<HoldoutSeparation.Measured>(EmotionHoldoutEvidence.separation(probes(3 to 12)))
        assertEquals(0.0, atChance.value)
        val belowChance = assertIs<HoldoutSeparation.Measured>(EmotionHoldoutEvidence.separation(probes(0 to 12)))
        assertEquals(0.0, belowChance.value)

        val perfect = assertIs<HoldoutSeparation.Measured>(EmotionHoldoutEvidence.separation(probes(12 to 12)))
        assertEquals(1.0, perfect.value)
    }

    @Test
    fun practice_effect_needs_two_probes_that_can_each_stand_on_their_own() {
        assertEquals(
            PracticeEffectRisk.Unavailable(HoldoutGap.NO_PROBE_SERVED),
            EmotionHoldoutEvidence.practiceEffectRisk(emptyList()),
        )
        assertEquals(
            PracticeEffectRisk.Unavailable(HoldoutGap.TOO_FEW_PROBES),
            EmotionHoldoutEvidence.practiceEffectRisk(listOf(HoldoutSessionEvidence(HoldoutProbeOutcome(6, 8), 0.6))),
        )
    }

    @Test
    fun the_bank_can_now_support_a_practice_effect_comparison() {
        // This test used to assert the opposite, and it did its job: it carried a guard saying it only meant
        // something while a real probe was smaller than the comparison floor, and that guard is what went red
        // when Ola 2 · R3 · P2 grew the bank and raised the probe. The refusal was never a property of the
        // arithmetic — it was a property of a bank too small to pay for a second endpoint. So the assertion is
        // inverted rather than deleted: probes sized as the real bank NOW supplies them are accepted, and the
        // practice-versus-progress question finally has an answer instead of a permanent excuse.
        val realSized = List(EmotionHoldoutEvidence.MIN_PROBES_FOR_CHANGE) {
            HoldoutSessionEvidence(
                probe = HoldoutProbeOutcome(correct = 2 + it, total = EmotionHoldoutProbe.PROBE_SIZE),
                trainedAccuracy = 0.5 + it * 0.2,
            )
        }
        assertTrue(
            EmotionHoldoutProbe.PROBE_SIZE >= EmotionHoldoutEvidence.MIN_ITEMS_PER_PROBE_FOR_CHANGE,
            "a probe of ${EmotionHoldoutProbe.PROBE_SIZE} is below the comparison floor again",
        )
        assertTrue(
            EmotionHoldoutProbe.MAX_PROBES >= EmotionHoldoutEvidence.MIN_PROBES_FOR_CHANGE,
            "the held-out partition supplies only ${EmotionHoldoutProbe.MAX_PROBES} complete probes",
        )
        assertTrue(
            EmotionHoldoutEvidence.practiceEffectRisk(realSized) is PracticeEffectRisk.Measured,
            "real-sized probes must now produce a number, not a refusal",
        )
    }

    @Test
    fun a_probe_below_the_comparison_floor_is_still_refused() {
        // The discriminating half of the test above: the arithmetic did not get more permissive, the bank got
        // bigger. A short probe must still be refused, or the inversion above would read as the floor being
        // dropped rather than met.
        val belowFloor = EmotionHoldoutEvidence.MIN_ITEMS_PER_PROBE_FOR_CHANGE - 1
        val short = List(EmotionHoldoutEvidence.MIN_PROBES_FOR_CHANGE) {
            HoldoutSessionEvidence(
                probe = HoldoutProbeOutcome(correct = 1, total = belowFloor),
                trainedAccuracy = 0.5 + it * 0.2,
            )
        }
        assertEquals(
            PracticeEffectRisk.Unavailable(HoldoutGap.PROBE_TOO_SMALL_TO_COMPARE),
            EmotionHoldoutEvidence.practiceEffectRisk(short),
        )
    }

    @Test
    fun risk_is_one_when_only_the_drilled_items_improved_and_zero_when_the_gain_carried() {
        val drilledOnly = EmotionHoldoutEvidence.practiceEffectRisk(
            listOf(
                HoldoutSessionEvidence(HoldoutProbeOutcome(4, 8), trainedAccuracy = 0.50),
                HoldoutSessionEvidence(HoldoutProbeOutcome(4, 8), trainedAccuracy = 0.90),
            ),
        )
        assertEquals(PracticeEffectRisk.Measured(1.0), drilledOnly, "unseen items did not move; the gain is practice")

        val carried = EmotionHoldoutEvidence.practiceEffectRisk(
            listOf(
                HoldoutSessionEvidence(HoldoutProbeOutcome(4, 8), trainedAccuracy = 0.50),
                HoldoutSessionEvidence(HoldoutProbeOutcome(7, 8), trainedAccuracy = 0.875),
            ),
        )
        assertEquals(PracticeEffectRisk.Measured(0.0), carried, "unseen items moved as much as the trained ones")

        val noGain = EmotionHoldoutEvidence.practiceEffectRisk(
            listOf(
                HoldoutSessionEvidence(HoldoutProbeOutcome(4, 8), trainedAccuracy = 0.80),
                HoldoutSessionEvidence(HoldoutProbeOutcome(4, 8), trainedAccuracy = 0.70),
            ),
        )
        assertEquals(PracticeEffectRisk.Measured(0.0), noGain, "no trained gain means nothing to attribute")
    }

    @Test
    fun transfer_confidence_is_a_ceiling_and_todays_evidence_never_lifts_it() {
        val separation = EmotionHoldoutEvidence.separation(probes(9 to 12))
        assertIs<HoldoutSeparation.Measured>(separation)

        // Strong unseen-item performance ALONE does not earn a transfer claim while the practice check is refused.
        assertEquals(
            TransferConfidence.NOT_DEMONSTRATED,
            EmotionHoldoutEvidence.maxTransferConfidence(
                separation,
                PracticeEffectRisk.Unavailable(HoldoutGap.PROBE_TOO_SMALL_TO_COMPARE),
            ),
        )
        // Nor does a measured-but-high practice risk.
        assertEquals(
            TransferConfidence.NOT_DEMONSTRATED,
            EmotionHoldoutEvidence.maxTransferConfidence(separation, PracticeEffectRisk.Measured(0.9)),
        )
        // The ceiling only lifts when BOTH halves are present and favourable — unreachable with today's bank.
        assertEquals(
            TransferConfidence.LIMITED,
            EmotionHoldoutEvidence.maxTransferConfidence(separation, PracticeEffectRisk.Measured(0.1)),
        )
    }

    // ------------------------------------------------- Ola 3 · R3 · P3 — the pressure unlock

    @Test
    fun pressure_stays_locked_until_enough_held_out_items_have_been_answered() {
        val floor = EmotionHoldoutEvidence.MIN_POOLED_ITEMS
        assertFalse(
            EmotionHoldoutEvidence.pressureUnlocked(correct = floor - 1, items = floor - 1),
            "a perfect score on fewer than $floor items unlocked a harder mode",
        )
        assertFalse(EmotionHoldoutEvidence.pressureUnlocked(correct = 0, items = 0), "no evidence unlocked pressure")
        assertTrue(
            EmotionHoldoutEvidence.pressureUnlocked(correct = floor, items = floor),
            "a perfect score at exactly the floor did not unlock",
        )
    }

    @Test
    fun pressure_tracks_the_bar_and_not_the_side_of_it_a_user_started_on() {
        // 20 pooled items: 16 correct is exactly the bar, 15 is under it. The rule is re-run every session from
        // the whole pooled history, so this is also the test that a user who falls back goes back to calm.
        assertTrue(EmotionHoldoutEvidence.pressureUnlocked(correct = 16, items = 20), "0.80 is not below the bar")
        assertFalse(EmotionHoldoutEvidence.pressureUnlocked(correct = 15, items = 20), "0.75 cleared an 0.80 bar")
        assertEquals(0.80, EmotionHoldoutEvidence.PRESSURE_UNLOCK_ACCURACY)
    }

    @Test
    fun the_unlock_reads_the_ratified_pooled_floor_rather_than_a_second_one() {
        // A private copy of this number would drift from the floor every other holdout figure is held to, and the
        // divergence would be invisible: both would keep passing their own tests.
        assertTrue(
            EmotionHoldoutEvidence.pressureUnlocked(
                correct = EmotionHoldoutProbe.PROBE_SIZE,
                items = EmotionHoldoutProbe.PROBE_SIZE,
            ),
            "one complete probe is ${EmotionHoldoutProbe.PROBE_SIZE} items and the pooled floor is " +
                "${EmotionHoldoutEvidence.MIN_POOLED_ITEMS}; if these have drifted apart the unlock is measuring " +
                "something no other holdout figure is measured against",
        )
    }
}
