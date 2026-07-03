package com.mindos.app.features.arena

import com.mindos.app.features.arena.config.TaskMotorType
import com.mindos.app.features.arena.rating.engine.RatingDelta
import com.mindos.app.features.arena.rating.model.Confidence
import com.mindos.app.features.arena.rating.model.LatencyMs
import com.mindos.app.features.arena.rating.model.Rating
import com.mindos.app.features.arena.rating.model.Sigma
import kotlin.test.Test
import kotlin.test.assertFailsWith

/** Phase 04: Immutable value-object hardening — invalid states cannot be constructed. */
class ArenaValueObjectsTest {

    @Test
    fun invalidRatingUncertaintyFails() {
        assertFailsWith<IllegalArgumentException> { Sigma(-1.0) }
    }

    @Test
    fun nanOrInfiniteRatingMeanFails() {
        assertFailsWith<IllegalArgumentException> { Rating(Double.NaN) }
        assertFailsWith<IllegalArgumentException> { Rating(Double.POSITIVE_INFINITY) }
        assertFailsWith<IllegalArgumentException> { Rating(Double.NEGATIVE_INFINITY) }
        assertFailsWith<IllegalArgumentException> { Sigma(Double.POSITIVE_INFINITY) }
    }

    @Test
    fun invalidAccuracyFails() {
        assertFailsWith<IllegalArgumentException> { Confidence(1.1) }
        assertFailsWith<IllegalArgumentException> { Confidence(-0.1) }
    }

    @Test
    fun negativeLatencyFails() {
        assertFailsWith<IllegalArgumentException> { LatencyMs(-5L) }
    }

    @Test
    fun missingHardwareClassFailsForMentalSpeed() {
        assertFailsWith<IllegalArgumentException> {
            ArenaTestFixtures.verifiedEvidence(
                motorType = TaskMotorType.MENTAL_SPEED_CHOICE_REACTION,
                hardwareClass = null,
            )
        }
    }

    @Test
    fun outOfRangeObservedPerformanceFails() {
        assertFailsWith<IllegalArgumentException> {
            ArenaTestFixtures.verifiedEvidence(observedPerformance = 1.5)
        }
    }

    @Test
    fun nonPositiveModeScalingFactorFails() {
        assertFailsWith<IllegalArgumentException> {
            ArenaTestFixtures.verifiedEvidence(modeScalingFactor = 0.0)
        }
    }

    @Test
    fun nonFiniteRatingDeltaFails() {
        assertFailsWith<IllegalArgumentException> { RatingDelta(Double.NaN, 0.0) }
        assertFailsWith<IllegalArgumentException> { RatingDelta(0.0, Double.POSITIVE_INFINITY) }
    }
}
