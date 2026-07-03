package com.mindos.app.features.mindboost.core.scoring

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins the central version registry: the display mapper's version string is registered here (not
 * hardcoded at call sites) and carries the honest `-linear-preliminary` marker. The consumer-side
 * assertion (that the mapper reports exactly this string) lives in `UserDisplayScoreMapperTest` to keep
 * the dependency direction profile → core.
 */
class ScoringVersionRegistryTest {

    @Test
    fun userDisplayScoreMapperVersion_isPinned_andMarkedLinearPreliminary() {
        assertEquals(
            "user-display-score-mapper-1.0.0-linear-preliminary",
            ScoringVersionRegistry.USER_DISPLAY_SCORE_MAPPER,
        )
        assertTrue(
            ScoringVersionRegistry.USER_DISPLAY_SCORE_MAPPER.endsWith("-linear-preliminary"),
            "the display map is a preliminary linear fallback until a per-block baseline curve exists",
        )
    }
}
