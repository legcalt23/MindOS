@file:Suppress("MagicNumber", "MaxLineLength")

package com.mindos.app.features.mindboost.speed.domain.score

import com.mindos.app.features.mindboost.core.copy.ForbiddenLexicon
import com.mindos.app.features.mindboost.speed.domain.InputTimestampPrecision
import com.mindos.app.features.mindboost.speed.domain.SpeedVersions
import com.mindos.app.features.mindboost.speed.domain.Timebase
import com.mindos.app.features.mindboost.speed.domain.TimestampSource
import com.mindos.app.features.mindboost.speed.domain.model.SessionMetrics
import com.mindos.app.features.mindboost.speed.presentation.SpeedStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * MC9-A · E2 Number Pulse Compare — the preliminary 0–1 score (REUSED [SpeedScoreEngine]) and the serious
 * "speed with precision" feedback. The score engine is not re-implemented: these tests exercise E2-flavoured
 * metrics through it to prove the charter rule "velocidad sin precisión no se premia".
 */
class NumberPulseFeedbackTest {

    private val engine = SpeedScoreEngine()

    private fun metrics(pc: Double, vce: Double?, impIdx: Double? = 0.1, cv: Double? = 0.2, lapseRate: Double = 0.05) =
        SessionMetrics(
            sessionId = "s", totalTrials = 40, pc = pc,
            rtMedianNanos = 300_000_000.0, rtMeanNanos = 300_000_000.0, cv = cv,
            lapseRate = lapseRate, anticipationRate = 0.02, faRate = 0.05, omissionRate = 0.0,
            impIdx = impIdx, fatIdx = 0.0, vce = vce,
            timestampSource = TimestampSource.CHOREOGRAPHER_FRAME,
            inputTimestampPrecision = InputTimestampPrecision.NANOS_NATIVE,
            timebase = Timebase.ANDROID_UI_UPTIME_NANOS,
        )

    private val feedbackStrings = listOf(
        SpeedStrings.FEEDBACK_CALIBRATING, SpeedStrings.FEEDBACK_FAST_LOW_PRECISION,
        SpeedStrings.FEEDBACK_SLOW_PRECISE, SpeedStrings.FEEDBACK_VARIABLE, SpeedStrings.FEEDBACK_BALANCED,
    )

    private val accents = mapOf('á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u', 'ñ' to 'n')
    private fun fold(s: String) = buildString { for (c in s.lowercase()) append(accents[c] ?: c) }

    @Test
    fun the_preliminary_score_stays_in_0_1_and_is_flagged_preliminary() {
        val score = engine.score(metrics(pc = 0.9, vce = 4.0))
        val total = assertNotNull(score.total)
        assertTrue(total in 0.0..1.0, "preliminary score is 0..1: $total")
        assertTrue(score.preliminary, "the score is the preliminary source of truth; never authoritative")
    }

    @Test
    fun the_preliminary_score_never_becomes_a_0_100_official_value() {
        // Sweep extremes; the total stays within 0..1 and preliminary — there is no authoritative 0–100 score.
        listOf(
            metrics(pc = 1.0, vce = 10.0, impIdx = 0.0, cv = 0.0, lapseRate = 0.0),
            metrics(pc = 0.99, vce = 5.0),
            metrics(pc = 0.5, vce = 5.0, impIdx = 0.9),
            metrics(pc = 0.0, vce = 0.0, cv = 1.0, lapseRate = 1.0),
        ).forEach { m ->
            val score = engine.score(m)
            score.total?.let { assertTrue(it in 0.0..1.0, "preliminary score must stay 0..1, got $it") }
            assertTrue(score.preliminary, "score stays preliminary while the §15 gate is open")
        }
    }

    @Test
    fun speed_without_precision_is_not_rewarded() {
        // Fast (max useful speed) but low accuracy (< 0.80 ceiling) ⇒ total penalized + FAST_IMPULSIVE.
        val reckless = engine.score(metrics(pc = 0.55, vce = 5.0, impIdx = 0.6))
        assertTrue(reckless.ceilingPenaltyApplied, "low accuracy must cap the total")
        assertEquals(SpeedMacroCategory.FAST_IMPULSIVE, reckless.macroCategory)
        assertEquals(NumberPulseFeedbackKind.FAST_LOW_PRECISION, NumberPulseFeedback.forScore(reckless))
    }

    @Test
    fun controlled_accuracy_scores_better_than_reckless_speed() {
        val controlled = engine.score(metrics(pc = 0.92, vce = 3.0, impIdx = 0.1, cv = 0.15))
        val reckless = engine.score(metrics(pc = 0.55, vce = 5.0, impIdx = 0.6))
        assertTrue(controlled.total!! > reckless.total!!, "control beats reckless speed: ${controlled.total} vs ${reckless.total}")
    }

    @Test
    fun the_feedback_mapping_covers_every_macro_category() {
        fun score(cat: SpeedMacroCategory) =
            CognitiveProfileScore(0.5, 0.5, 0.5, 0.5, cat, ceilingPenaltyApplied = false)
        assertEquals(NumberPulseFeedbackKind.CALIBRATING, NumberPulseFeedback.forScore(score(SpeedMacroCategory.CALIBRATING)))
        assertEquals(NumberPulseFeedbackKind.FAST_LOW_PRECISION, NumberPulseFeedback.forScore(score(SpeedMacroCategory.FAST_IMPULSIVE)))
        assertEquals(NumberPulseFeedbackKind.SLOW_PRECISE, NumberPulseFeedback.forScore(score(SpeedMacroCategory.SLOW_PRECISE)))
        assertEquals(NumberPulseFeedbackKind.VARIABLE, NumberPulseFeedback.forScore(score(SpeedMacroCategory.VARIABLE)))
        assertEquals(NumberPulseFeedbackKind.BALANCED, NumberPulseFeedback.forScore(score(SpeedMacroCategory.BALANCED)))
    }

    @Test
    fun scoring_and_metrics_versions_are_present() {
        val score = engine.score(metrics(pc = 0.9, vce = 4.0))
        assertTrue(score.scoreVersion.isNotBlank(), "scoreVersion present")
        assertTrue(SpeedVersions.METRICS_VERSION.isNotBlank(), "metricsVersion present")
    }

    @Test
    fun every_feedback_line_mentions_speed_and_precision() {
        feedbackStrings.forEach { s ->
            val f = fold(s)
            assertTrue(f.contains("velocidad") && f.contains("precision"), "must mention speed AND precision: '$s'")
        }
    }

    @Test
    fun feedback_copy_carries_no_forbidden_claim() {
        // Referencing the lexicon (not literals) keeps this test itself copy-scanner-clean.
        val forbidden = ForbiddenLexicon.ALL.map { fold(it) }
        feedbackStrings.forEach { s ->
            val f = fold(s)
            forbidden.forEach { bad -> assertFalse(f.contains(bad), "forbidden claim '$bad' in feedback: '$s'") }
        }
    }
}
