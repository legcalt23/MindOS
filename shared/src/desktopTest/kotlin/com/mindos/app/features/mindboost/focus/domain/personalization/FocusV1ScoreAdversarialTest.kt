package com.mindos.app.features.mindboost.focus.domain.personalization

import com.mindos.app.core.config.DefaultFeatureFlags
import com.mindos.app.core.config.StaticFeatureFlags
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricSet
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricUnit
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricValue
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * S0 · **The adversarial oracle A1–A6 for the CONTAINED v1 Focus score.**
 *
 * This test is INVERTED on purpose. It does not assert that the v1 scorer is correct — it asserts, with
 * pinned numbers, that it is **wrong in a specific, reproducible way**, and it therefore stands as the
 * executable justification for why `FeatureFlags.focusScoreSurfacesEnabled` ships OFF (ledger
 * `MB-ENG-0007`). Plan V5 §5/S0 task (4); oracle spec in `agent_outputs/03_psychometrician.md` §G.
 *
 * **The defect.** `DefaultPersonalizedSessionScorer` feeds `MetricKey.LAPSES` into a penalty. A "lapse"
 * is thresholded at `lapseRtMs = 500` — a PVT constant — but it is applied to a flanker whose response
 * window is 900–1500 ms. An honest filterer answering in 400–650 ms therefore accrues "lapses" and eats
 * the −0.30 cap, while a metronomic key-masher at a flat ~250 ms accrues **zero**, and additionally
 * collects a perfect SPEED component and a near-perfect STABILITY component for being mechanical. The
 * scorer rewards exactly the behaviour a cognitive trainer must not reward.
 *
 * **Expected values are derived independently, not read from the subject.** Each is computed by hand
 * from the published formula (weights ACCURACY .5 / SPEED .2 / STABILITY .3; speed normalised 250 ms→1,
 * 800 ms→0; stability 1 − cv/0.5; lapse cost .03 each, capped at .30) and pinned as a literal. If the
 * formula changes, these literals must fail — that is the point. Worked example (HONEST):
 * `0.5·0.92 + 0.2·0.50 + 0.3·0.64 = 0.752`, minus the capped `0.30` lapse penalty ⇒ `0.452` ⇒ **45.2**.
 *
 * **When this test starts failing**, someone changed the v1 scorer. That is not automatically progress:
 * re-exposing any number requires the full S0 checklist (A1–A6 green for the exposed scorer, RT excluded
 * unless timing class ≥T2, archetype route cut, coverage/uncertainty metadata) and, for a capability
 * 0–100, V3 PASSED plus ADR-FOCUS-SCORING. Fixing the formula alone does NOT authorise flipping the flag.
 */
class FocusV1ScoreAdversarialTest {

    private val scorer = DefaultPersonalizedSessionScorer()

    /** Archetype-neutral modifiers: isolates the FORMULA's behaviour from any personalization bending. */
    private val neutral = ScoringModifiers()

    private fun metrics(
        accuracy: Double,
        rtMedianMs: Double,
        rtCv: Double,
        lapses: Double,
        n: Int = N_PER_PROFILE,
    ): MetricSet = mapOf(
        MetricKey.ACCURACY to MetricValue(MetricKey.ACCURACY, accuracy, n, true, MetricUnit.PROPORTION),
        MetricKey.RT_MEDIAN to MetricValue(MetricKey.RT_MEDIAN, rtMedianMs, n, true, MetricUnit.MILLISECONDS),
        MetricKey.RT_CV to MetricValue(MetricKey.RT_CV, rtCv, n, true, MetricUnit.RATIO),
        MetricKey.LAPSES to MetricValue(MetricKey.LAPSES, lapses, n, true, MetricUnit.COUNT),
    )

    // The four adversarial profiles of the oracle (E2 flanker, 900-1500 ms window, n = 55 each).
    /** Genuine filtering: high accuracy, human RT spread that straddles the 500 ms "lapse" threshold. */
    private val honest = metrics(accuracy = 0.92, rtMedianMs = 525.0, rtCv = 0.18, lapses = 20.0)

    /** One key, metronomic: chance accuracy, no misses (a wrong key is a FALSE_ALARM), zero lapses. */
    private val masher = metrics(accuracy = 0.50, rtMedianMs = 250.0, rtCv = 0.02, lapses = 0.0)

    /** Waits out the window: chance accuracy, every response past the lapse threshold. */
    private val waiter = metrics(accuracy = 0.50, rtMedianMs = 1400.0, rtCv = 0.05, lapses = 55.0)

    /** Always answers one side: chance accuracy at a comfortable, human-looking speed. */
    private val sideBias = metrics(accuracy = 0.50, rtMedianMs = 450.0, rtCv = 0.20, lapses = 10.0)

    private fun scoreOf(set: MetricSet): Double = assertNotNull(scorer.score(set, neutral).value)

    @Test
    fun a1_honest_does_NOT_beat_masher_which_is_exactly_why_v1_is_hidden() {
        val honestScore = scoreOf(honest)
        val masherScore = scoreOf(masher)

        assertEquals(HONEST_EXPECTED, honestScore, TOLERANCE, "v1 honest score drifted from the pinned value")
        assertEquals(MASHER_EXPECTED, masherScore, TOLERANCE, "v1 masher score drifted from the pinned value")

        // A1 as specified is `score(HONEST) > score(MASHER)`. v1 INVERTS it. Asserting the inversion keeps
        // the defect executable instead of leaving it as prose in a document nobody runs.
        assertTrue(
            masherScore > honestScore,
            "A1 INVERSION LOST: v1 used to reward the masher ($MASHER_EXPECTED) over the honest responder " +
                "($HONEST_EXPECTED). If this now fails the scorer changed — re-run the full S0 checklist " +
                "before exposing any number; a changed formula is not by itself an authorisation.",
        )
        assertTrue(
            masherScore - honestScore > MEANINGFUL_GAP,
            "The inversion must be a MEANINGFUL gap, not a rounding artefact, to justify containment.",
        )
    }

    @Test
    fun a2_masher_reaches_a_high_confident_score_from_pure_chance_accuracy() {
        val result = scorer.score(masher, neutral)
        val value = assertNotNull(result.value)

        // A2 asks that a masher never reach a high/VALID score. v1 hands it one, and marks it CONFIDENT
        // (n = 55 clears MIN_N = 20), so nothing downstream can tell it apart from a real result.
        assertTrue(value > HIGH_SCORE_FLOOR, "v1 gives the masher a high score; that is the P0 being contained.")
        assertTrue(result.confident, "v1 additionally marks the masher's chance-level score as confident.")
    }

    @Test
    fun a3_waiter_and_side_bias_do_score_below_honest() {
        // A3 is the one assertion v1 already satisfies — worth pinning so a future "fix" cannot silently
        // regress it while repairing A1.
        assertTrue(scoreOf(waiter) < scoreOf(honest), "A3: waiter must score below honest.")
        assertTrue(scoreOf(sideBias) < scoreOf(honest), "A3: side-bias must score below honest.")
    }

    @Test
    fun a4_discriminating_half_honest_still_produces_a_confident_non_zero_score() {
        val result = scorer.score(honest, neutral)
        val value = assertNotNull(result.value)

        // THE DISCRIMINATING HALF. Without it, a "fix" that clamped every score to 0.0 would satisfy A1-A3
        // and look like progress while destroying the metric for honest users. An honest session must keep
        // producing a confident, non-zero value. (Whether 45/100 is a DEFENSIBLE level for acc≈0.92 is a
        // separate question that only V3 can answer — v1's absolute calibration is not claimed here.)
        assertTrue(value > 0.0, "An honest session must not collapse to zero.")
        assertTrue(result.confident, "n = $N_PER_PROFILE clears MIN_N, so the honest score must be confident.")
    }

    @Test
    fun a5_score_consumes_rt_cv_and_threshold_lapses_violating_the_input_manifest() {
        // A5 requires the exposed score NOT to consume RT_CV or threshold-based lapses. v1 consumes both.
        // Proven by perturbation rather than by reading the source: change one input, observe the output.
        val cvPerturbed = metrics(accuracy = 0.92, rtMedianMs = 525.0, rtCv = 0.40, lapses = 20.0)
        val lapsesPerturbed = metrics(accuracy = 0.92, rtMedianMs = 525.0, rtCv = 0.18, lapses = 0.0)

        assertTrue(
            abs(scoreOf(cvPerturbed) - scoreOf(honest)) > TOLERANCE,
            "A5 violation: RT_CV moves the score, so the scorer consumes a metric its manifest forbids.",
        )
        assertTrue(
            abs(scoreOf(lapsesPerturbed) - scoreOf(honest)) > TOLERANCE,
            "A5 violation: threshold-lapses move the score — the PVT-constant defect at the heart of the P0.",
        )
    }

    @Test
    fun a6_chance_accuracy_yields_a_number_instead_of_a_typed_absence() {
        // A6: chance-level performance must produce `null` + a reason, never a fabricated number — and
        // never a "0 perfect" either (learning 2026-07-15: absence must fabricate neither a minimum nor a
        // maximum). v1 does neither: it returns a confident 73.8 for behaviour indistinguishable from
        // guessing, because chance accuracy is treated as an ordinary 0.5 with no chance-floor correction.
        val result = scorer.score(masher, neutral)
        assertNotNull(
            result.value,
            "A6 violation documented: v1 emits a number for chance-level accuracy instead of a typed absence.",
        )
        assertFalse(
            result.value == 0.0,
            "The v1 defect is a fabricated HIGH value, not a fabricated zero — both directions are wrong.",
        )
    }

    @Test
    fun containment_flag_defaults_to_off_in_every_impl_so_none_of_the_above_reaches_a_user() {
        // REDTEAM #37: the flag's failure mode must be "hide". A default of true anywhere re-introduces the
        // P0. The Android RemoteConfig impl reads through this same default when the key is absent or the
        // remote value is malformed, so pinning the two shared impls pins the fallback for all three.
        assertFalse(DefaultFeatureFlags.focusScoreSurfacesEnabled, "Default impl must contain the v1 score.")
        assertFalse(StaticFeatureFlags().focusScoreSurfacesEnabled, "Static impl must default to contained.")
        assertEquals(
            "focus_score_surfaces_enabled",
            DefaultFeatureFlags.KEY_FOCUS_SCORE_SURFACES,
            "The Remote Config key is the operational kill switch; renaming it silently would orphan it.",
        )
    }

    private companion object {
        const val N_PER_PROFILE = 55

        // Hand-derived from the published formula; see the class KDoc for the HONEST worked example.
        // HONEST : 0.5(0.92) + 0.2(0.50) + 0.3(0.64) = 0.752 ; lapses 20*0.03 = 0.60 -> capped 0.30 ; = 45.2
        // MASHER : 0.5(0.50) + 0.2(1.00) + 0.3(0.96) = 0.738 ; lapses 0                              ; = 73.8
        const val HONEST_EXPECTED = 45.2
        const val MASHER_EXPECTED = 73.8

        const val TOLERANCE = 0.5
        const val MEANINGFUL_GAP = 10.0
        const val HIGH_SCORE_FLOOR = 65.0
    }
}
