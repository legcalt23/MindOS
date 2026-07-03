package com.mindos.app.features.mindboost.focus.domain.personalization

import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricSet
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricUnit
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricValue
import com.mindos.app.features.mindboost.focus.domain.model.UserArchetype
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The personalized score must honour the archetype's [ScoringModifiers]: impulsive (speedIncentive < 1)
 * rewards speed LESS, fast-but-inaccurate (errorPenalty > 1) punishes errors MORE, low-concentration
 * (lapsePenalty > 1) punishes lapses MORE — and the whole thing stays confidence-gated. These are the
 * 08_ §6 acceptance behaviours.
 */
class PersonalizedSessionScorerTest {

    private val scorer = DefaultPersonalizedSessionScorer()
    private val neutral = PersonalizationProfiles.forArchetype(UserArchetype.BEGINNER).scoringModifiers
    private val impulsive = PersonalizationProfiles.forArchetype(UserArchetype.IMPULSIVE).scoringModifiers

    private fun metric(key: MetricKey, value: Double, n: Int = 40, confident: Boolean = true) =
        key to MetricValue(key, value, n, confident, MetricUnit.RATIO)

    private fun metrics(
        accuracy: Double,
        rtMedian: Double,
        rtCv: Double = 0.3,
        lapses: Double = 0.0,
        n: Int = 40,
    ): MetricSet = mapOf(
        metric(MetricKey.ACCURACY, accuracy, n),
        metric(MetricKey.RT_MEDIAN, rtMedian, n),
        metric(MetricKey.RT_CV, rtCv, n),
        metric(MetricKey.LAPSES, lapses, n),
    )

    @Test
    fun impulsive_rewardsSpeedLessThanNeutral() {
        // Same accuracy/stability; the only difference is RT (fast vs slow). The reward for being faster
        // must be SMALLER under the impulsive modifiers than under neutral.
        fun delta(modifiers: ScoringModifiers): Double {
            val fast = scorer.score(metrics(accuracy = 0.7, rtMedian = 280.0), modifiers).value!!
            val slow = scorer.score(metrics(accuracy = 0.7, rtMedian = 600.0), modifiers).value!!
            return fast - slow
        }
        assertTrue(
            delta(impulsive) < delta(neutral),
            "Impulsive must reward short RT less than neutral does.",
        )
        assertTrue(scorer.score(metrics(0.7, 280.0), impulsive).deemphasizesSpeed)
        assertFalse(scorer.score(metrics(0.7, 280.0), neutral).deemphasizesSpeed)
    }

    @Test
    fun fastButInaccurate_penalizesErrorsMore() {
        val faInaccurate = PersonalizationProfiles.forArchetype(UserArchetype.FAST_BUT_INACCURATE).scoringModifiers
        val m = metrics(accuracy = 0.5, rtMedian = 280.0)
        val neutralScore = scorer.score(m, neutral).value!!
        val faScore = scorer.score(m, faInaccurate).value!!
        assertTrue(faScore < neutralScore, "Higher errorPenalty must lower the score of an inaccurate session.")
    }

    @Test
    fun lowConcentration_penalizesLapsesMore() {
        val lowConc = PersonalizationProfiles.forArchetype(UserArchetype.LOW_CONCENTRATION).scoringModifiers
        val m = metrics(accuracy = 0.8, rtMedian = 400.0, lapses = 5.0)
        val neutralScore = scorer.score(m, neutral).value!!
        val lowConcScore = scorer.score(m, lowConc).value!!
        assertTrue(lowConcScore < neutralScore, "Higher lapsePenalty must lower a lapse-heavy session's score.")
    }

    @Test
    fun isGated_whenAccuracyIsNotConfident() {
        // A small sample → the metrics engine marks ACCURACY unconfident; the scorer must not score it.
        val unconfident = mapOf(
            MetricKey.ACCURACY to MetricValue(
                MetricKey.ACCURACY,
                value = 0.9,
                n = 10,
                confident = false,
                unit = MetricUnit.RATIO,
            ),
            metric(MetricKey.RT_MEDIAN, 300.0, n = 10),
        )
        val score = scorer.score(unconfident, neutral)
        assertNull(score.value)
        assertFalse(score.confident)
    }

    @Test
    fun withoutConfidentAccuracy_returnsNoValue() {
        val noAccuracy = mapOf(metric(MetricKey.RT_MEDIAN, 300.0))
        val score = scorer.score(noAccuracy, neutral)
        assertNull(score.value)
        assertFalse(score.confident)
    }

    @Test
    fun cleanSession_producesAConfidentScore() {
        val score = scorer.score(metrics(accuracy = 0.95, rtMedian = 320.0, rtCv = 0.15, lapses = 0.0), neutral)
        assertNotNull(score.value)
        assertTrue(score.confident)
        assertTrue(score.value!! > 50.0, "A clean, accurate, stable session should score well above the floor.")
    }
}
