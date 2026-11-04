@file:Suppress("MagicNumber", "MaxLineLength") // Persisted-row fixtures are the data under test.

package com.mindos.app.features.mindboost.emotions.data.local

import com.mindos.app.features.mindboost.emotions.data.dto.DifficultySummaryDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionScoringVersions
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSessionSummaryDto
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTrend
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Ola 0 · S4 — `confidence` and `dataSufficiency` are ORTHOGONAL by charter, and the aggregate used to assign
 * them the identical number. Showing up five times is a statement about how much evidence exists, not about
 * whether that evidence agrees with itself, and reporting the first as the second told a user with wildly
 * inconsistent sessions that their profile was maximally confident.
 */
class EmotionAggregateConfidenceTest {

    private var nextId = 0

    private fun session(
        score: Int?,
        startedAtMillis: Long,
        exercise: EmotionExerciseId = EmotionExerciseId.EM1_PAUSA,
    ) = EmotionSessionSummaryDto(
        exerciseId = exercise.name,
        startedAtMillis = startedAtMillis,
        durationMs = 180_000, // above the validity floor
        difficultySummary = DifficultySummaryDto("INHIBITORY_DEMAND", 0.3, 1),
        metricsSummary = emptyList(),
        completed = true,
        abandoned = false,
        sessionId = "s-${nextId++}",
        endReason = "COMPLETED",
        governingConfident = true,
        scoringVersion = EmotionScoringVersions.SCORING,
        normalizedScore = score,
    )

    private fun aggregateOf(vararg sessions: EmotionSessionSummaryDto) =
        assertNotNull(EmotionAggregateFactory.fromSummaries(sessions.toList(), now = 9_000L))

    @Test
    fun sufficiency_and_confidence_are_no_longer_the_same_number() {
        // Five sessions: sufficiency is full by definition, but the readings disagree violently, so confidence
        // must not follow it up.
        val unstable = aggregateOf(
            session(10, 100), session(95, 200), session(15, 300), session(90, 400), session(20, 500),
        )
        assertEquals(1.0, unstable.dataSufficiency, "five valid sessions is full sufficiency")
        assertTrue(
            unstable.confidence < unstable.dataSufficiency,
            "wildly inconsistent sessions must not report the confidence of consistent ones (got ${unstable.confidence})",
        )
    }

    @Test
    fun consistent_sessions_report_higher_confidence_than_inconsistent_ones() {
        // The discriminating half. Without it, returning a constant 0.0 for confidence would satisfy the test
        // above while destroying the signal entirely.
        val steady = aggregateOf(
            session(70, 100), session(72, 200), session(69, 300), session(71, 400), session(70, 500),
        )
        val erratic = aggregateOf(
            session(10, 100), session(95, 200), session(15, 300), session(90, 400), session(20, 500),
        )
        assertTrue(
            steady.confidence > erratic.confidence,
            "a stable series must earn more confidence than an erratic one (${steady.confidence} vs ${erratic.confidence})",
        )
        assertEquals(steady.dataSufficiency, erratic.dataSufficiency, "both have the same AMOUNT of evidence")
    }

    @Test
    fun a_single_session_reports_no_confidence_rather_than_a_fabricated_fraction() {
        val single = aggregateOf(session(80, 100))
        assertEquals(0.0, single.confidence, "one point cannot establish stability")
        assertTrue(single.dataSufficiency > 0.0, "but it IS evidence, so sufficiency is not zero")
    }

    @Test
    fun a_rising_series_is_reported_as_improving() {
        val improving = aggregateOf(session(50, 100), session(55, 200), session(70, 300), session(75, 400))
        assertEquals(ProfileTrend.IMPROVING.name, improving.trend)
    }

    @Test
    fun a_falling_series_is_reported_as_declining() {
        val declining = aggregateOf(session(80, 100), session(78, 200), session(55, 300), session(50, 400))
        assertEquals(ProfileTrend.DECLINING.name, declining.trend)
    }

    @Test
    fun a_flat_series_is_steady_not_improving() {
        val flat = aggregateOf(session(70, 100), session(71, 200), session(69, 300), session(70, 400))
        assertEquals(ProfileTrend.STEADY.name, flat.trend, "ordinary variation must not be dressed up as progress")
    }

    @Test
    fun too_few_comparable_points_report_unknown_rather_than_a_guess() {
        val two = aggregateOf(session(50, 100), session(90, 200))
        assertEquals(ProfileTrend.UNKNOWN.name, two.trend)
    }

    @Test
    fun scores_from_different_exercises_are_never_pooled_into_one_direction() {
        // Each exercise is adapted on its own governing metric, so its scores are a different measurement on a
        // different scale. Pooling them would manufacture a direction out of the order the user picked exercises.
        val mixed = aggregateOf(
            session(20, 100, EmotionExerciseId.EM1_PAUSA),
            session(30, 200, EmotionExerciseId.EM2_RECUPERACION),
            session(80, 300, EmotionExerciseId.EM3_SEPARADOR),
            session(90, 400, EmotionExerciseId.EM10_SEP_PRESION),
        )
        assertEquals(
            ProfileTrend.UNKNOWN.name,
            mixed.trend,
            "four sessions across four exercises is not a four-point series of anything",
        )
    }

    @Test
    fun rows_written_before_scores_existed_do_not_enter_the_series_as_zeros() {
        // `normalizedScore` is null on older rows. Treating null as 0 would drag both statistics toward a value
        // nobody ever scored, and would read as a catastrophic decline the moment scoring was introduced.
        val scoredOnly = aggregateOf(session(70, 200), session(72, 300), session(69, 400), session(71, 500))
        val withLegacy = aggregateOf(
            session(null, 100), session(null, 150),
            session(70, 200), session(72, 300), session(69, 400), session(71, 500),
        )
        assertEquals(ProfileTrend.STEADY.name, withLegacy.trend, "the unscored rows must be dropped, not counted as zero")
        assertEquals(
            scoredOnly.confidence,
            withLegacy.confidence,
            "adding unscored rows changed the stability reading, so they are entering the series",
        )
        assertTrue(
            withLegacy.dataSufficiency > scoredOnly.dataSufficiency,
            "the unscored rows are still valid sessions, so they DO add evidence — they just cannot be scored",
        )
    }
}
